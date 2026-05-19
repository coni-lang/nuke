package com.hellonico.nuke.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NukeImportPomAction extends AnAction {

    public NukeImportPomAction() {
        super("Sync from pom.xml", "Import dependencies from pom.xml to nuke.edn", AllIcons.Actions.Download);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.getBasePath() == null) return;

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Syncing from pom.xml...", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setIndeterminate(true);
                    indicator.setText("Scanning pom.xml...");
                    
                    Path pomFile = Paths.get(project.getBasePath(), "pom.xml");
                    Path nukeFile = Paths.get(project.getBasePath(), "nuke.edn");
                    
                    if (!Files.exists(pomFile) || !Files.exists(nukeFile)) {
                        indicator.setText("pom.xml or nuke.edn not found.");
                        Thread.sleep(1000);
                        return;
                    }

                    String content = Files.readString(pomFile);
                    Pattern blockPattern = Pattern.compile("<dependency>\\s*(.*?)\\s*</dependency>", Pattern.DOTALL);
                    Matcher blockMatcher = blockPattern.matcher(content);

                    List<String> deps = new ArrayList<>();
                    List<String> testDeps = new ArrayList<>();

                    while (blockMatcher.find()) {
                        String block = blockMatcher.group(1);
                        String group = extractTag(block, "groupId");
                        String name = extractTag(block, "artifactId");
                        String version = extractTag(block, "version");
                        String scope = extractTag(block, "scope");

                        if (group == null || name == null) continue;
                        if (version == null || version.isEmpty()) version = "LATEST";

                        String depStr = "\"" + group + ":" + name + ":" + version + "\"";
                        if ("test".equals(scope)) {
                            testDeps.add(depStr);
                        } else {
                            deps.add(depStr);
                        }
                    }

                    indicator.setText("Updating nuke.edn...");
                    String ednContent = Files.readString(nukeFile);
                    
                    // Simple injection into nuke.edn (appending)
                    ednContent = ednContent.replaceAll("(?s):dependencies\\s*\\[.*?\\]", "");
                    ednContent = ednContent.replaceAll("(?s):test-dependencies\\s*\\[.*?\\]", "");
                    
                    // Remove trailing brace
                    ednContent = ednContent.trim();
                    if (ednContent.endsWith("}")) {
                        ednContent = ednContent.substring(0, ednContent.length() - 1);
                    }

                    StringBuilder sb = new StringBuilder(ednContent);
                    if (!deps.isEmpty()) {
                        sb.append("\n :dependencies [");
                        sb.append(String.join("\n                ", deps));
                        sb.append("]");
                    }
                    if (!testDeps.isEmpty()) {
                        sb.append("\n :test-dependencies [");
                        sb.append(String.join("\n                     ", testDeps));
                        sb.append("]");
                    }
                    sb.append("\n}");

                    Files.writeString(nukeFile, sb.toString());
                    
                    indicator.setText("Syncing project model...");
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(nukeFile.toString());
                        if (vf != null) {
                            com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().reloadFiles(vf);
                        }
                        NukeProjectManager.sync(project);
                        NukeToolWindowFactory.refresh(project);
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private String extractTag(String xml, String tag) {
        Pattern p = Pattern.compile("<" + tag + ">([^<]+)</" + tag + ">");
        Matcher m = p.matcher(xml);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }
}
