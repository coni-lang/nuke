package com.hellonico.nuke.plugin;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NukeRunConfiguration extends RunConfigurationBase<NukeRunConfigurationOptions> {
    public NukeRunConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    @NotNull
    @Override
    protected NukeRunConfigurationOptions getOptions() {
        return (NukeRunConfigurationOptions) super.getOptions();
    }

    public String getTaskName() {
        return getOptions().getTaskName();
    }

    public void setTaskName(String taskName) {
        getOptions().setTaskName(taskName);
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new NukeRunConfigurationEditor();
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        return new NukeRunProfileState(environment, this);
    }
}
