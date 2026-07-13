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
    private static String getResourceHash(String resourcePath) {
        try (java.io.InputStream in = NukeProjectManager.class.getResourceAsStream(resourcePath)) {
            if (in == null) return "unknown";
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] block = new byte[4096];
            int length;
            while ((length = in.read(block)) > 0) {
                digest.update(block, 0, length);
            }
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.substring(0, 12);
        } catch (Exception e) {
            return "unknown";
        }
    }

    public static String getNukeExecutable() {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
        String path = NukeSettings.getInstance().getNukeExecutablePath();

        if (isWindows) {
            if (path != null && !path.isEmpty() && !path.equals("nuke")) {
                File f = new File(path);
                if (f.exists() && f.isFile() && path.endsWith(".exe")) {
                    return path;
                }
                File fExe = new File(path + ".exe");
                if (fExe.exists() && fExe.isFile()) {
                    return fExe.getAbsolutePath();
                }
            }
        } else {
            if (path != null && !path.isEmpty() && !path.equals("nuke")) {
                File f = new File(path);
                if (f.exists() && f.isFile()) {
                    return path;
                }
            }
        }

        String binName = isWindows ? "nuke.exe" : (isMac ? "nuke-mac" : "nuke-linux");
        String resourcePath = "/bin/" + binName;
        String hash = getResourceHash(resourcePath);
        String finalBinName = isWindows ? ("nuke_" + hash + ".exe") : (isMac ? ("nuke-mac_" + hash) : ("nuke-linux_" + hash));

        try {
            File tmpDir = new File(System.getProperty("java.io.tmpdir"), "nuke-intellij-plugin");
            tmpDir.mkdirs();
            File binFile = new File(tmpDir, finalBinName);
            
            if (binFile.exists() && binFile.isFile() && binFile.length() > 0) {
                return binFile.getAbsolutePath();
            }

            File[] files = tmpDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().startsWith("nuke_") || f.getName().startsWith("nuke-mac_") || f.getName().startsWith("nuke-linux_")) {
                        if (!f.getName().equals(finalBinName)) {
                            f.delete();
                        }
                    }
                }
            }

            java.io.InputStream in = NukeProjectManager.class.getResourceAsStream(resourcePath);
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
                        int jarCount = updateClasspath(project);
                        notifySyncComplete(project, jarCount);
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

    private static void notifySyncComplete(Project project, int jarCount) {
        com.intellij.notification.NotificationGroupManager.getInstance()
            .getNotificationGroup("Nuke Notifications")
            .createNotification("Nuke Sync Complete", "Synced " + jarCount + " dependencies.", com.intellij.notification.NotificationType.INFORMATION)
            .notify(project);
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

    private static int updateClasspath(Project project) {
        String basePath = project.getBasePath();
        if (basePath == null) return 0;

        // --- Phase 1: collect dep dirs without touching IntelliJ models ---
        java.util.Set<String> processed = new java.util.HashSet<>();
        java.util.List<File> depDirs = new java.util.ArrayList<>();
        java.util.Set<String> localProjectNames = new java.util.HashSet<>();
        collectDependencies(basePath, processed, depDirs, localProjectNames);

        java.util.concurrent.atomic.AtomicInteger jarCount = new java.util.concurrent.atomic.AtomicInteger(0);

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
                    // Resolve relative paths (e.g. "libs/foo.jar") against basePath
                    if (!f.isAbsolute()) f = new File(basePath, path);
                    if (f.exists() && f.getName().endsWith(".jar")) {
                        boolean isLocal = false;
                        for (String lpn : localProjectNames) {
                            if (f.getName().startsWith(lpn + "-")) { isLocal = true; break; }
                        }
                        if (!isLocal) {
                            jarUrls.add("jar://" + f.getAbsolutePath().replace('\\', '/') + "!/");
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
                    if (library == null) {
                        library = table.createLibrary("NukeDeps");
                    }
                    Library.ModifiableModel libModel = library.getModifiableModel();
                    for (String url : libModel.getUrls(OrderRootType.CLASSES)) {
                        libModel.removeRoot(url, OrderRootType.CLASSES);
                    }
                    for (String url : jarUrls) libModel.addRoot(url, OrderRootType.CLASSES);
                    libModel.commit();

                    for (ContentEntry e : depModel.getContentEntries()) {
                        depModel.removeContentEntry(e);
                    }
                    VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByPath(depDir.getAbsolutePath());
                    ContentEntry ce = root != null ? depModel.addContentEntry(root) : depModel.addContentEntry(VfsUtil.pathToUrl(depDir.getAbsolutePath()));
                    ce.clearSourceFolders();
                    java.util.List<String> srcDirs = parseArray(depDir.getAbsolutePath() + "/nuke.edn", ":src-dir");
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
                    
                    java.util.List<String> resourceDirs = parseArray(depDir.getAbsolutePath() + "/nuke.edn", ":resource-dir");
                    if (resourceDirs.isEmpty()) {
                        resourceDirs.add("src/main/resources");
                    }
                    for (String dir : resourceDirs) {
                        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(depDir.getAbsolutePath() + "/" + dir);
                        if (vf != null) ce.addSourceFolder(vf, JavaResourceRootType.RESOURCE);
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
                if (library == null) {
                    library = table.createLibrary("NukeDeps");
                }
                Library.ModifiableModel libModel = library.getModifiableModel();
                for (String url : libModel.getUrls(OrderRootType.CLASSES)) {
                    libModel.removeRoot(url, OrderRootType.CLASSES);
                }
                for (String url : jarUrls) libModel.addRoot(url, OrderRootType.CLASSES);
                libModel.commit();

                for (ContentEntry e : model.getContentEntries()) {
                    model.removeContentEntry(e);
                }
                VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath);
                ContentEntry entry = root != null ? model.addContentEntry(root) : model.addContentEntry(VfsUtil.pathToUrl(basePath));
                entry.clearSourceFolders();
                java.util.List<String> srcDirs = parseArray(basePath + "/nuke.edn", ":src-dir");
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
                java.util.List<String> testDirs = parseArray(basePath + "/nuke.edn", ":test-dir");
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
                
                java.util.List<String> resourceDirs = parseArray(basePath + "/nuke.edn", ":resource-dir");
                if (resourceDirs.isEmpty()) {
                    resourceDirs.add("src/main/resources");
                }
                for (String dir : resourceDirs) {
                    VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath + "/" + dir);
                    if (vf != null) entry.addSourceFolder(vf, JavaResourceRootType.RESOURCE);
                }
                
                CompilerModuleExtension compilerExtension = model.getModuleExtension(CompilerModuleExtension.class);
                if (compilerExtension != null) {
                    compilerExtension.inheritCompilerOutputPath(false);
                    compilerExtension.setCompilerOutputPath(VfsUtil.pathToUrl(basePath + "/build/classes/java/main"));
                    compilerExtension.setCompilerOutputPathForTests(VfsUtil.pathToUrl(basePath + "/build/classes/java/test"));
                }
            });
            jarCount.set(jarUrls.size());
        });
        return jarCount.get();
    }

    private static java.util.List<String> parseArray(String ednPath, String key) {
        java.util.List<String> res = new ArrayList<>();
        try {
            String content = java.nio.file.Files.readString(java.nio.file.Paths.get(ednPath));
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(key + "s?\\s*\\[([^\\]]+)\\]").matcher(content);
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
            pb.redirectErrorStream(true);
            Process p = pb.start();
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                if (!line.trim().isEmpty() && line.contains(".jar")) {
                    String[] parts = line.trim().split(":");
                    for (String part : parts) {
                        if (!part.isEmpty()) {
                            paths.add(part);
                        }
                    }
                }
            }
            p.waitFor();
            com.intellij.openapi.diagnostic.Logger.getInstance(NukeProjectManager.class).info("Nuke classpath output for " + basePath + ":\n" + output.toString());
        } catch (Exception e) {
            com.intellij.openapi.diagnostic.Logger.getInstance(NukeProjectManager.class).error("Error getting classpath", e);
        }
        return paths;
    }
}
