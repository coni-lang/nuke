package com.hellonico.nuke.plugin;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.icons.AllIcons;

public class NukeRunConfigurationType extends ConfigurationTypeBase {
    public NukeRunConfigurationType() {
        super("NukeRunConfiguration", "Nuke Task", "Execute a Nuke task", AllIcons.Nodes.Plugin);
        addFactory(new ConfigurationFactory(this) {
            @Override
            public String getId() {
                return "Nuke Task";
            }

            @Override
            public com.intellij.execution.configurations.RunConfiguration createTemplateConfiguration(com.intellij.openapi.project.Project project) {
                return new NukeRunConfiguration(project, this, "Nuke");
            }

            @Override
            public Class<? extends com.intellij.execution.configurations.RunConfigurationOptions> getOptionsClass() {
                return NukeRunConfigurationOptions.class;
            }
        });
    }
}
