package com.hellonico.nuke.plugin;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.icons.AllIcons;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

import com.intellij.psi.tree.IElementType;
import com.hellonico.nuke.plugin.lang.NukeTokenTypes;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class NukeRunLineMarkerContributor extends RunLineMarkerContributor {
    
    private String getParentMapName(PsiElement element) {
        PsiElement parent = element.getParent();
        if (parent != null && parent.getNode().getElementType().toString().equals("LIST")) {
            PsiElement prev = parent.getPrevSibling();
            while (prev != null && prev.getText().trim().isEmpty()) {
                prev = prev.getPrevSibling();
            }
            if (prev != null && prev.getText().startsWith(":")) {
                return prev.getText().substring(1);
            }
        }
        return null;
    }

    private List<String> getCustomTasks(PsiElement tasksKeyword) {
        List<String> customTasks = new ArrayList<>();
        PsiElement next = tasksKeyword.getNextSibling();
        while (next != null && (next.getText().trim().isEmpty() || next.getNode().getElementType().toString().equals("WHITE_SPACE"))) {
            next = next.getNextSibling();
        }
        if (next != null && next.getNode().getElementType().toString().equals("LIST")) {
            PsiElement child = next.getFirstChild();
            while (child != null) {
                if (child.getNode().getElementType().toString().equals("KEYWORD") && child.getText().startsWith(":")) {
                    customTasks.add(child.getText().substring(1));
                }
                child = child.getNextSibling();
            }
        }
        return customTasks;
    }

    private AnAction createRunAction(PsiElement element, String taskName, String displayName) {
        return createRunAction(element, taskName, "", displayName);
    }

    private AnAction createRunAction(PsiElement element, String taskName, String arguments, String displayName) {
        String description = "Execute " + taskName + (arguments != null && !arguments.isEmpty() ? " " + arguments : "");
        return new AnAction("Run " + displayName, description, AllIcons.RunConfigurations.TestState.Run) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                RunManager runManager = RunManager.getInstance(element.getProject());
                ConfigurationFactory factory = new NukeRunConfigurationType().getConfigurationFactories()[0];
                String name = "Nuke " + taskName + (arguments != null && !arguments.isEmpty() ? " " + arguments : "");
                RunnerAndConfigurationSettings settings = runManager.createConfiguration(name, factory);
                ((NukeRunConfiguration) settings.getConfiguration()).setTaskName(taskName);
                ((NukeRunConfiguration) settings.getConfiguration()).setArguments(arguments != null ? arguments : "");
                runManager.addConfiguration(settings);
                runManager.setSelectedConfiguration(settings);
                ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
            }
        };
    }

    @Nullable
    @Override
    public Info getInfo(@NotNull PsiElement element) {
        IElementType type = element.getNode().getElementType();
        if (type == NukeTokenTypes.KEYWORD) {
            String text = element.getText();
            if (text.length() > 1) {
                String taskName = text.substring(1);
                
                if (taskName.equals("main-class")) {
                    AnAction runAction = createRunAction(element, "run", "Application");
                    return new Info(AllIcons.RunConfigurations.TestState.Run, new AnAction[]{runAction}, e -> "Run application");
                }

                if (taskName.equals("deploy")) {
                    List<AnAction> actions = new ArrayList<>();
                    PsiElement value = element.getNextSibling();
                    while (value != null && (value.getText().trim().isEmpty() || value.getNode().getElementType().toString().equals("WHITE_SPACE") || value.getNode().getElementType() == NukeTokenTypes.COMMENT)) {
                        value = value.getNextSibling();
                    }
                    if (value != null) {
                        IElementType valueType = value.getNode().getElementType();
                        if (valueType.toString().equals("LIST")) {
                            List<String> keys = new ArrayList<>();
                            PsiElement child = value.getFirstChild();
                            boolean isKey = true;
                            while (child != null) {
                                IElementType childType = child.getNode().getElementType();
                                if (childType == NukeTokenTypes.BRACE1 || childType == NukeTokenTypes.BRACE2 || childType.toString().equals("WHITE_SPACE") || childType == NukeTokenTypes.COMMENT) {
                                    child = child.getNextSibling();
                                    continue;
                                }
                                if (isKey) {
                                    if (childType == NukeTokenTypes.KEYWORD || childType == NukeTokenTypes.STRING) {
                                        String keyText = child.getText();
                                        if (childType == NukeTokenTypes.KEYWORD && keyText.startsWith(":")) {
                                            keyText = keyText.substring(1);
                                        } else if (childType == NukeTokenTypes.STRING) {
                                            if (keyText.startsWith("\"") && keyText.endsWith("\"") && keyText.length() >= 2) {
                                                keyText = keyText.substring(1, keyText.length() - 1);
                                            }
                                        }
                                        keys.add(keyText);
                                    }
                                    isKey = false;
                                } else {
                                    isKey = true;
                                }
                                child = child.getNextSibling();
                            }
                            if (!keys.isEmpty()) {
                                for (String key : keys) {
                                    actions.add(createRunAction(element, "upload", key, "upload to " + key));
                                    actions.add(createRunAction(element, "upload-uberjar", key, "upload-uberjar to " + key));
                                }
                            } else {
                                actions.add(createRunAction(element, "upload", "upload"));
                                actions.add(createRunAction(element, "upload-uberjar", "upload-uberjar"));
                            }
                        } else {
                            actions.add(createRunAction(element, "upload", "upload"));
                            actions.add(createRunAction(element, "upload-uberjar", "upload-uberjar"));
                        }
                    } else {
                        actions.add(createRunAction(element, "upload", "upload"));
                        actions.add(createRunAction(element, "upload-uberjar", "upload-uberjar"));
                    }
                    return new Info(AllIcons.RunConfigurations.TestState.Run, actions.toArray(new AnAction[0]), e -> "Run Deployment Tasks");
                }
                
                if (taskName.equals("dependencies") || taskName.equals("test-dependencies")) {
                    AnAction runAction = createRunAction(element, "download-deps", "download-deps");
                    return new Info(AllIcons.RunConfigurations.TestState.Run, new AnAction[]{runAction}, e -> "Run download-deps");
                } 
                
                if (taskName.equals("analysis")) {
                    List<AnAction> actions = new ArrayList<>();
                    actions.add(createRunAction(element, "analyze", "All Analysis Tools"));
                    actions.add(createRunAction(element, "metrics", "Metrics (JaCoCo)"));
                    actions.add(createRunAction(element, "spotbugs", "SpotBugs"));
                    actions.add(createRunAction(element, "pmd", "PMD"));
                    actions.add(createRunAction(element, "checkstyle", "Checkstyle"));
                    actions.add(createRunAction(element, "sonarqube", "SonarQube"));
                    return new Info(AllIcons.RunConfigurations.TestState.Run, actions.toArray(new AnAction[0]), e -> "Run Analysis Tasks");
                } 
                
                if (taskName.equals("tasks")) {
                    List<AnAction> actions = new ArrayList<>();
                    String[] stdTasks = {"clean", "template", "download-deps", "classpath", "compile", "test", "run", "jar", "uberjar", "zip", "upload", "build"};
                    for (String t : stdTasks) {
                        actions.add(createRunAction(element, t, t));
                    }
                    List<String> customTasks = getCustomTasks(element);
                    for (String t : customTasks) {
                        actions.add(createRunAction(element, t, t + " (custom)"));
                    }
                    return new Info(AllIcons.RunConfigurations.TestState.Run, actions.toArray(new AnAction[0]), e -> "Run Nuke Tasks");
                }
                
                String parentMapName = getParentMapName(element);
                if ("tasks".equals(parentMapName)) {
                    AnAction a = createRunAction(element, taskName, taskName);
                    return new Info(AllIcons.RunConfigurations.TestState.Run, new AnAction[]{a}, e -> "Run " + taskName);
                }
            }
        }
        return null;
    }
}
