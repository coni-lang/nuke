package com.hellonico.nuke.plugin;

import com.intellij.openapi.options.SettingsEditor;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class NukeRunConfigurationEditor extends SettingsEditor<NukeRunConfiguration> {
    private JBTextField myTaskNameField;

    @Override
    protected void resetEditorFrom(@NotNull NukeRunConfiguration s) {
        myTaskNameField.setText(s.getTaskName());
    }

    @Override
    protected void applyEditorTo(@NotNull NukeRunConfiguration s) {
        s.setTaskName(myTaskNameField.getText());
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        myTaskNameField = new JBTextField();
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Task name:", myTaskNameField)
                .getPanel();
    }
}
