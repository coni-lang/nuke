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

import com.intellij.psi.tree.IElementType;
import com.hellonico.nuke.plugin.lang.NukeTokenTypes;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class NukeRunLineMarkerContributor extends RunLineMarkerContributor {
    @Nullable
    @Override
    public Info getInfo(@NotNull PsiElement element) {
        IElementType type = element.getNode().getElementType();
        if (type == NukeTokenTypes.KEYWORD) {
            String text = element.getText();
            if (text.length() > 1) {
                String taskName = text.substring(1);
                
                if (taskName.equals("main-class")) {
                    AnAction runAction = new AnAction("Run Application", "Execute run task", AllIcons.RunConfigurations.TestState.Run) {
                        @Override
                        public void actionPerformed(@NotNull AnActionEvent e) {
                            RunManager runManager = RunManager.getInstance(element.getProject());
                            ConfigurationFactory factory = new NukeRunConfigurationType().getConfigurationFactories()[0];
                            RunnerAndConfigurationSettings settings = runManager.createConfiguration("Nuke run", factory);
                            ((NukeRunConfiguration) settings.getConfiguration()).setTaskName("run");
                            runManager.addConfiguration(settings);
                            runManager.setSelectedConfiguration(settings);
                            ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
                        }
                    };
                    return new Info(AllIcons.RunConfigurations.TestState.Run, new AnAction[]{runAction}, e -> "Run application");
                }
                
                // Exclude other generic EDN keys used by Nuke
                if (taskName.equals("name") || taskName.equals("version") || taskName.equals("extends") || 
                    taskName.equals("local-dependencies") || taskName.equals("path") || 
                    taskName.equals("javac-opts") || taskName.equals("tasks")) {
                    return null;
                }
                
                AnAction runAction = new AnAction("Run Nuke Task: " + taskName, "Execute " + taskName, AllIcons.RunConfigurations.TestState.Run) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        RunManager runManager = RunManager.getInstance(element.getProject());
                        ConfigurationFactory factory = new NukeRunConfigurationType().getConfigurationFactories()[0];
                        RunnerAndConfigurationSettings settings = runManager.createConfiguration("Nuke " + taskName, factory);
                        ((NukeRunConfiguration) settings.getConfiguration()).setTaskName(taskName);
                        runManager.addConfiguration(settings);
                        runManager.setSelectedConfiguration(settings);
                        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
                    }
                };
                
                return new Info(AllIcons.RunConfigurations.TestState.Run, new AnAction[]{runAction}, e -> "Run " + taskName);
            }
        }
        return null;
    }
}
