package com.hellonico.nuke.plugin;

import com.intellij.openapi.options.SettingsEditor;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class NukeRunConfigurationEditor extends SettingsEditor<NukeRunConfiguration> {
    private JBTextField myTaskNameField;
    private JBTextField myArgumentsField;

    @Override
    protected void resetEditorFrom(@NotNull NukeRunConfiguration s) {
        myTaskNameField.setText(s.getTaskName());
        myArgumentsField.setText(s.getArguments());
    }

    @Override
    protected void applyEditorTo(@NotNull NukeRunConfiguration s) {
        s.setTaskName(myTaskNameField.getText());
        s.setArguments(myArgumentsField.getText());
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        myTaskNameField = new JBTextField();
        myArgumentsField = new JBTextField();
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Task name:", myTaskNameField)
                .addLabeledComponent("Arguments:", myArgumentsField)
                .getPanel();
    }
}
