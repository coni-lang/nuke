package com.hellonico.nuke.plugin;

import com.intellij.execution.configurations.RunConfigurationOptions;
import com.intellij.openapi.components.StoredProperty;

public class NukeRunConfigurationOptions extends RunConfigurationOptions {
    private final StoredProperty<String> myTaskName = string("").provideDelegate(this, "taskName");

    public String getTaskName() {
        return myTaskName.getValue(this);
    }

    public void setTaskName(String taskName) {
        myTaskName.setValue(this, taskName);
    }
}
