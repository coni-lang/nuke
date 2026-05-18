package com.hellonico.nuke.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessHandlerFactory;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.util.Key;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.CompilerModuleExtension;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class NukeProjectManager {
    public static String getNukeExecutable() {
        String path = NukeSettings.getInstance().getNukeExecutablePath();
        if (path != null && !path.isEmpty() && !path.equals("nuke") && new File(path).exists()) {
            return path;
        }
        
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("win");
        String binName = isWindows ? "nuke.exe" : "nuke";
        
        try {
            File tmpDir = new File(System.getProperty("java.io.tmpdir"), "nuke-intellij-plugin");
            tmpDir.mkdirs();
            File binFile = new File(tmpDir, binName);
            
            java.io.InputStream in = NukeProjectManager.class.getResourceAsStream("/bin/" + binName);
            if (in != null) {
                java.nio.file.Files.copy(in, binFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                in.close();
                binFile.setExecutable(true);
                return binFile.getAbsolutePath();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isWindows ? "nuke.exe" : "nuke";
    }

    public static void sync(Project project) {
        String basePath = project.getBasePath();
        if (basePath == null) return;
        
        GeneralCommandLine cmd = new GeneralCommandLine(getNukeExecutable(), "download-deps");
        cmd.setWorkDirectory(basePath);
        
        try {
            ProcessHandler processHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(cmd);
            processHandler.addProcessListener(new ProcessListener() {
                public void startNotified(ProcessEvent event) {}
                public void processTerminated(ProcessEvent event) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        updateClasspath(project);
                        NukeToolWindowFactory.refresh(project);
                    });
                }
                public void processWillTerminate(ProcessEvent event, boolean willBeDestroyed) {}
                public void onTextAvailable(ProcessEvent event, Key outputType) {}
            });
            processHandler.startNotify();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<String> getLocalDependencies(String basePath) {
        List<String> deps = new ArrayList<>();
        File ednFile = new File(basePath, "nuke.edn");
        if (ednFile.exists()) {
            try {
                String content = new String(java.nio.file.Files.readAllBytes(ednFile.toPath()));
                java.util.regex.Matcher m = java.util.regex.Pattern.compile(":path\\s+\"([^\"]+)\"").matcher(content);
                while (m.find()) {
                    deps.add(m.group(1));
                }
            } catch (Exception e) {}
        }
        return deps;
    }

    private static String getProjectName(String basePath) {
        File ednFile = new File(basePath, "nuke.edn");
        if (ednFile.exists()) {
            try {
                String content = new String(java.nio.file.Files.readAllBytes(ednFile.toPath()));
                java.util.regex.Matcher m = java.util.regex.Pattern.compile(":name\\s+\"([^\"]+)\"").matcher(content);
                if (m.find()) {
                    return m.group(1);
                }
            } catch (Exception e) {}
        }
        return new File(basePath).getName();
    }

    private static void processDependenciesRecursively(Project project, Module parentModule, String moduleBasePath, java.util.Set<String> processed, java.util.Set<String> localProjectNames) {
        if (processed.contains(moduleBasePath)) return;
        processed.add(moduleBasePath);

        List<String> localDeps = getLocalDependencies(moduleBasePath);
        for (String relPath : localDeps) {
            try {
                File depDir = new File(moduleBasePath, relPath).getCanonicalFile();
                if (depDir.exists() && depDir.isDirectory()) {
                    String depName = depDir.getName();
                    String projName = getProjectName(depDir.getAbsolutePath());
                    localProjectNames.add(projName);
                    
                    com.intellij.openapi.module.ModifiableModuleModel moduleModel = ModuleManager.getInstance(project).getModifiableModel();
                    Module depModule = moduleModel.findModuleByName(depName);
                    if (depModule == null) {
                        String imlPath = depDir.getAbsolutePath() + "/" + depName + ".iml";
                        depModule = moduleModel.newModule(imlPath, "JAVA_MODULE");
                    }
                    moduleModel.commit();
                    
                    final Module finalDepModule = depModule;
                    ModuleRootModificationUtil.updateModel(depModule, depModel -> {
                        depModel.inheritSdk();
                        depModel.inheritSdk();
                        ContentEntry entry = null;
                        for (ContentEntry e : depModel.getContentEntries()) {
                            if (e.getUrl().equals(VfsUtil.pathToUrl(depDir.getAbsolutePath()))) {
                                entry = e;
                                break;
                            }
                        }
                        if (entry == null) {
                            VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByPath(depDir.getAbsolutePath());
                            entry = root != null ? depModel.addContentEntry(root) : depModel.addContentEntry(VfsUtil.pathToUrl(depDir.getAbsolutePath()));
                        }
                        
                        entry.clearSourceFolders();
                        java.util.List<String> srcDirs = parseArray(depDir.getAbsolutePath() + "/nuke.edn", ":src-dirs");
                        if (srcDirs.isEmpty()) srcDirs.add("src/main");
                        for (String dir : srcDirs) {
                            VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(depDir.getAbsolutePath() + "/" + dir);
                            if (vf != null) entry.addSourceFolder(vf, false);
                        }
                        
                        java.util.List<String> testDirs = parseArray(depDir.getAbsolutePath() + "/nuke.edn", ":test-dirs");
                        if (testDirs.isEmpty()) testDirs.add("src/tests");
                        for (String dir : testDirs) {
                            VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(depDir.getAbsolutePath() + "/" + dir);
                            if (vf != null) entry.addSourceFolder(vf, true);
                        }
                        
                        
                        VirtualFile resources = LocalFileSystem.getInstance().refreshAndFindFileByPath(depDir.getAbsolutePath() + "/src/main/resources");
                        if (resources != null) entry.addSourceFolder(resources, JavaResourceRootType.RESOURCE);

                        CompilerModuleExtension compilerExtension = depModel.getModuleExtension(CompilerModuleExtension.class);
                        if (compilerExtension != null) {
                            compilerExtension.inheritCompilerOutputPath(false);
                            compilerExtension.setCompilerOutputPath(VfsUtil.pathToUrl(depDir.getAbsolutePath() + "/build/classes/java/main"));
                            compilerExtension.setCompilerOutputPathForTests(VfsUtil.pathToUrl(depDir.getAbsolutePath() + "/build/classes/java/test"));
                        }
                    });
                    
                    ModuleRootModificationUtil.addDependency(parentModule, depModule);
                    
                    processDependenciesRecursively(project, depModule, depDir.getAbsolutePath(), processed, localProjectNames);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void updateClasspath(Project project) {
        String basePath = project.getBasePath();
        if (basePath == null) return;
        
        ApplicationManager.getApplication().runWriteAction(() -> {
            Module[] modules = ModuleManager.getInstance(project).getModules();
            Module module;
            if (modules.length == 0) {
                com.intellij.openapi.module.ModifiableModuleModel moduleModel = ModuleManager.getInstance(project).getModifiableModel();
                module = moduleModel.newModule(basePath + "/" + project.getName() + ".iml", "JAVA_MODULE");
                moduleModel.commit();
            } else {
                module = modules[0];
            }

            Sdk projectSdk = ProjectRootManager.getInstance(project).getProjectSdk();
            if (projectSdk == null) {
                Sdk[] jdks = ProjectJdkTable.getInstance().getAllJdks();
                for (Sdk sdk : jdks) {
                    if (sdk.getSdkType() instanceof JavaSdk) {
                        ProjectRootManager.getInstance(project).setProjectSdk(sdk);
                        break;
                    }
                }
            }

            // Process local dependencies recursively BEFORE building NukeDeps to know which jars to exclude
            java.util.Set<String> processed = new java.util.HashSet<>();
            java.util.Set<String> localProjectNames = new java.util.HashSet<>();
            processDependenciesRecursively(project, module, basePath, processed, localProjectNames);

            java.util.Set<String> validModuleNames = new java.util.HashSet<>();
            validModuleNames.add(module.getName());
            for (String processedPath : processed) {
                validModuleNames.add(new File(processedPath).getName());
            }

            com.intellij.openapi.module.ModifiableModuleModel modifiableModel = ModuleManager.getInstance(project).getModifiableModel();
            for (Module m : modifiableModel.getModules()) {
                if (!validModuleNames.contains(m.getName())) {
                    modifiableModel.disposeModule(m);
                }
            }
            modifiableModel.commit();

            File libsDir = new File(basePath, "libs");
            List<String> jarUrls = new ArrayList<>();
            if (libsDir.exists() && libsDir.isDirectory()) {
                for (File f : libsDir.listFiles()) {
                    if (f.getName().endsWith(".jar")) {
                        boolean isLocal = false;
                        for (String lpn : localProjectNames) {
                            if (f.getName().startsWith(lpn + "-")) {
                                isLocal = true;
                                break;
                            }
                        }
                        if (!isLocal) {
                            jarUrls.add(VfsUtil.getUrlForLibraryRoot(f));
                        }
                    }
                }
            }

            ModuleRootModificationUtil.updateModel(module, model -> {
                model.inheritSdk();
                LibraryTable table = model.getModuleLibraryTable();
                Library library = table.getLibraryByName("NukeDeps");
                if (library != null) {
                    table.removeLibrary(library);
                }
                library = table.createLibrary("NukeDeps");
                Library.ModifiableModel libModel = library.getModifiableModel();
                for (String url : jarUrls) {
                    libModel.addRoot(url, OrderRootType.CLASSES);
                }
                libModel.commit();

                ContentEntry entry = null;
                for (ContentEntry e : model.getContentEntries()) {
                    if (e.getUrl().equals(VfsUtil.pathToUrl(basePath))) {
                        entry = e;
                        break;
                    }
                }
                if (entry == null) {
                    VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath);
                    entry = root != null ? model.addContentEntry(root) : model.addContentEntry(VfsUtil.pathToUrl(basePath));
                }
                
                entry.clearSourceFolders();
                java.util.List<String> srcDirs = parseArray(basePath + "/nuke.edn", ":src-dirs");
                if (srcDirs.isEmpty()) srcDirs.add("src/main");
                for (String dir : srcDirs) {
                    VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath + "/" + dir);
                    if (vf != null) entry.addSourceFolder(vf, false);
                }
                
                java.util.List<String> testDirs = parseArray(basePath + "/nuke.edn", ":test-dirs");
                if (testDirs.isEmpty()) testDirs.add("src/tests");
                for (String dir : testDirs) {
                    VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath + "/" + dir);
                    if (vf != null) entry.addSourceFolder(vf, true);
                }
                
                VirtualFile resources = LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath + "/src/main/resources");
                if (resources != null) entry.addSourceFolder(resources, JavaResourceRootType.RESOURCE);

                CompilerModuleExtension compilerExtension = model.getModuleExtension(CompilerModuleExtension.class);
                if (compilerExtension != null) {
                    compilerExtension.inheritCompilerOutputPath(false);
                    compilerExtension.setCompilerOutputPath(VfsUtil.pathToUrl(basePath + "/build/classes/java/main"));
                    compilerExtension.setCompilerOutputPathForTests(VfsUtil.pathToUrl(basePath + "/build/classes/java/test"));
                }
            });
        });
    }

    private static java.util.List<String> parseArray(String ednPath, String key) {
        java.util.List<String> res = new ArrayList<>();
        try {
            String content = java.nio.file.Files.readString(java.nio.file.Paths.get(ednPath));
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(key + "\\s*\\[([^\\]]+)\\]").matcher(content);
            if (m.find()) {
                java.util.regex.Matcher sm = java.util.regex.Pattern.compile("\"([^\"]+)\"").matcher(m.group(1));
                while (sm.find()) {
                    res.add(sm.group(1));
                }
            }
        } catch (Exception e) {}
        return res;
    }
}
