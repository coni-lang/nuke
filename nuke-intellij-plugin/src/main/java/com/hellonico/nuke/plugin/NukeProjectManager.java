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
        boolean isMac = os.contains("mac");
        String binName = isWindows ? "nuke.exe" : (isMac ? "nuke-mac" : "nuke-linux");
        
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
        return binName;
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
                // Extract the :local-dependencies vector content
                java.util.regex.Matcher section = java.util.regex.Pattern
                        .compile(":local-dependencies\\s*\\[([^]]+)]")
                        .matcher(content);
                if (section.find()) {
                    String block = section.group(1);
                    // Match {:path "..."} format
                    java.util.regex.Matcher pathMatcher = java.util.regex.Pattern
                            .compile(":path\\s+\"([^\"]+)\"")
                            .matcher(block);
                    while (pathMatcher.find()) deps.add(pathMatcher.group(1));
                    // Match plain string format: "..." (not inside a map)
                    // Remove map entries first, then pick up bare strings
                    String stripped = block.replaceAll("\\{[^}]*}", "");
                    java.util.regex.Matcher strMatcher = java.util.regex.Pattern
                            .compile("\"([^\"]+)\"")
                            .matcher(stripped);
                    while (strMatcher.find()) deps.add(strMatcher.group(1));
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

    // Phase 1: collect all local dependency directories recursively (no write action needed)
    private static void collectDependencies(String moduleBasePath, java.util.Set<String> processed, java.util.List<File> collectedDirs, java.util.Set<String> localProjectNames) {
        if (processed.contains(moduleBasePath)) return;
        processed.add(moduleBasePath);

        List<String> localDeps = getLocalDependencies(moduleBasePath);
        for (String relPath : localDeps) {
            try {
                File depDir = new File(moduleBasePath, relPath).getCanonicalFile();
                if (depDir.exists() && depDir.isDirectory()) {
                    localProjectNames.add(getProjectName(depDir.getAbsolutePath()));
                    collectedDirs.add(depDir);
                    collectDependencies(depDir.getAbsolutePath(), processed, collectedDirs, localProjectNames);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void updateClasspath(Project project) {
        String basePath = project.getBasePath();
        if (basePath == null) return;

        // --- Phase 1: collect dep dirs without touching IntelliJ models ---
        java.util.Set<String> processed = new java.util.HashSet<>();
        java.util.List<File> depDirs = new java.util.ArrayList<>();
        java.util.Set<String> localProjectNames = new java.util.HashSet<>();
        collectDependencies(basePath, processed, depDirs, localProjectNames);

        // --- Phase 2: create / find all modules in ONE write action with ONE commit ---
        ApplicationManager.getApplication().runWriteAction(() -> {
            // Ensure root module exists
            Module[] modules = ModuleManager.getInstance(project).getModules();
            Module rootModule = null;
            com.intellij.openapi.module.ModifiableModuleModel moduleModel = ModuleManager.getInstance(project).getModifiableModel();
            String expectedRootName = project.getName();
            for (Module m : modules) {
                if (m.getName().equals(expectedRootName)) {
                    rootModule = m;
                    break;
                }
            }
            if (rootModule == null) {
                rootModule = moduleModel.newModule(basePath + "/" + expectedRootName + ".iml", "JAVA_MODULE");
            }

            // Create all dep modules that don't exist yet
            java.util.Map<File, Module> depModuleMap = new java.util.LinkedHashMap<>();
            for (File depDir : depDirs) {
                String depName = depDir.getName();
                Module depModule = moduleModel.findModuleByName(depName);
                if (depModule == null) {
                    depModule = moduleModel.newModule(depDir.getAbsolutePath() + "/" + depName + ".iml", "JAVA_MODULE");
                }
                depModuleMap.put(depDir, depModule);
            }
            moduleModel.commit(); // single commit for all module creations

            // Set JDK
            Sdk projectSdk = ProjectRootManager.getInstance(project).getProjectSdk();
            if (projectSdk == null) {
                for (Sdk sdk : ProjectJdkTable.getInstance().getAllJdks()) {
                    if (sdk.getSdkType() instanceof JavaSdk) {
                        ProjectRootManager.getInstance(project).setProjectSdk(sdk);
                        break;
                    }
                }
            }

            // Remove stale modules
            java.util.Set<String> validModuleNames = new java.util.HashSet<>();
            validModuleNames.add(rootModule.getName());
            for (File d : depDirs) validModuleNames.add(d.getName());

            com.intellij.openapi.module.ModifiableModuleModel pruneModel = ModuleManager.getInstance(project).getModifiableModel();
            for (Module m : pruneModel.getModules()) {
                if (!validModuleNames.contains(m.getName())) pruneModel.disposeModule(m);
            }
            pruneModel.commit();

            // --- Phase 2.5: configure third party jars (excluding local project jars) ---
            List<String> jarUrls = new ArrayList<>();
            List<String> classpathJars = getProjectClasspath(basePath);
            if (!classpathJars.isEmpty()) {
                for (String path : classpathJars) {
                    File f = new File(path);
                    if (f.exists() && f.getName().endsWith(".jar")) {
                        boolean isLocal = false;
                        for (String lpn : localProjectNames) {
                            if (f.getName().startsWith(lpn + "-")) { isLocal = true; break; }
                        }
                        if (!isLocal) {
                            jarUrls.add(VfsUtil.getUrlForLibraryRoot(f));
                        }
                    }
                }
            } else {
                File libsDir = new File(basePath, "libs");
                if (libsDir.exists() && libsDir.isDirectory()) {
                    File[] libFiles = libsDir.listFiles();
                    if (libFiles != null) {
                        for (File f : libFiles) {
                            if (!f.getName().endsWith(".jar")) continue;
                            boolean isLocal = false;
                            for (String lpn : localProjectNames) {
                                if (f.getName().startsWith(lpn + "-")) { isLocal = true; break; }
                            }
                            if (!isLocal) jarUrls.add(VfsUtil.getUrlForLibraryRoot(f));
                        }
                    }
                }
            }

            // --- Phase 3: configure content roots and add module dependencies ---
            for (java.util.Map.Entry<File, Module> entry : depModuleMap.entrySet()) {
                File depDir = entry.getKey();
                Module depModule = entry.getValue();
                ModuleRootModificationUtil.updateModel(depModule, depModel -> {
                    depModel.inheritSdk();
                    
                    LibraryTable table = depModel.getModuleLibraryTable();
                    Library library = table.getLibraryByName("NukeDeps");
                    if (library != null) table.removeLibrary(library);
                    library = table.createLibrary("NukeDeps");
                    Library.ModifiableModel libModel = library.getModifiableModel();
                    for (String url : jarUrls) libModel.addRoot(url, OrderRootType.CLASSES);
                    libModel.commit();

                    for (ContentEntry e : depModel.getContentEntries()) {
                        depModel.removeContentEntry(e);
                    }
                    VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByPath(depDir.getAbsolutePath());
                    ContentEntry ce = root != null ? depModel.addContentEntry(root) : depModel.addContentEntry(VfsUtil.pathToUrl(depDir.getAbsolutePath()));
                    ce.clearSourceFolders();
                    java.util.List<String> srcDirs = parseArray(depDir.getAbsolutePath() + "/nuke.edn", ":src-dirs");
                    if (srcDirs.isEmpty()) {
                        if (new File(depDir, "src/main/java").exists()) {
                            srcDirs.add("src/main/java");
                        } else {
                            srcDirs.add("src/main");
                        }
                    }
                    for (String dir : srcDirs) {
                        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(depDir.getAbsolutePath() + "/" + dir);
                        if (vf != null) ce.addSourceFolder(vf, false);
                    }
                    java.util.List<String> testDirs = parseArray(depDir.getAbsolutePath() + "/nuke.edn", ":test-dirs");
                    if (testDirs.isEmpty()) {
                        if (new File(depDir, "src/test/java").exists()) {
                            testDirs.add("src/test/java");
                        } else {
                            testDirs.add("src/tests");
                        }
                    }
                    for (String dir : testDirs) {
                        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(depDir.getAbsolutePath() + "/" + dir);
                        if (vf != null) ce.addSourceFolder(vf, true);
                    }
                    VirtualFile resources = LocalFileSystem.getInstance().refreshAndFindFileByPath(depDir.getAbsolutePath() + "/src/main/resources");
                    if (resources != null) ce.addSourceFolder(resources, JavaResourceRootType.RESOURCE);
                    CompilerModuleExtension compilerExtension = depModel.getModuleExtension(CompilerModuleExtension.class);
                    if (compilerExtension != null) {
                        compilerExtension.inheritCompilerOutputPath(false);
                        compilerExtension.setCompilerOutputPath(VfsUtil.pathToUrl(depDir.getAbsolutePath() + "/build/classes/java/main"));
                        compilerExtension.setCompilerOutputPathForTests(VfsUtil.pathToUrl(depDir.getAbsolutePath() + "/build/classes/java/test"));
                    }
                });
                ModuleRootModificationUtil.addDependency(rootModule, depModule);
            }

            // --- Phase 4: configure root module jars ---
            ModuleRootModificationUtil.updateModel(rootModule, model -> {
                model.inheritSdk();
                LibraryTable table = model.getModuleLibraryTable();
                Library library = table.getLibraryByName("NukeDeps");
                if (library != null) table.removeLibrary(library);
                library = table.createLibrary("NukeDeps");
                Library.ModifiableModel libModel = library.getModifiableModel();
                for (String url : jarUrls) libModel.addRoot(url, OrderRootType.CLASSES);
                libModel.commit();

                for (ContentEntry e : model.getContentEntries()) {
                    model.removeContentEntry(e);
                }
                VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath);
                ContentEntry entry = root != null ? model.addContentEntry(root) : model.addContentEntry(VfsUtil.pathToUrl(basePath));
                entry.clearSourceFolders();
                java.util.List<String> srcDirs = parseArray(basePath + "/nuke.edn", ":src-dirs");
                if (srcDirs.isEmpty()) {
                    if (new File(basePath, "src/main/java").exists()) {
                        srcDirs.add("src/main/java");
                    } else {
                        srcDirs.add("src/main");
                    }
                }
                for (String dir : srcDirs) {
                    VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath + "/" + dir);
                    if (vf != null) entry.addSourceFolder(vf, false);
                }
                java.util.List<String> testDirs = parseArray(basePath + "/nuke.edn", ":test-dirs");
                if (testDirs.isEmpty()) {
                    if (new File(basePath, "src/test/java").exists()) {
                        testDirs.add("src/test/java");
                    } else {
                        testDirs.add("src/tests");
                    }
                }
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

    private static List<String> getProjectClasspath(String basePath) {
        List<String> paths = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(getNukeExecutable(), "classpath");
            pb.directory(new File(basePath));
            Process p = pb.start();
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
                String[] parts = line.trim().split(":");
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        paths.add(part);
                    }
                }
            }
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return paths;
    }
}
