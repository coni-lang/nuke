package com.hellonico.nuke.plugin;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;

import javax.swing.*;

public class NukeSettingsConfigurable implements Configurable {
    private JBTextField myNukePathField;

    @Override
    public String getDisplayName() {
        return "Nuke Build";
    }

    @Override
    public JComponent createComponent() {
        myNukePathField = new JBTextField();
        return FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Nuke executable path:"), myNukePathField, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    @Override
    public boolean isModified() {
        NukeSettings settings = NukeSettings.getInstance();
        return !myNukePathField.getText().equals(settings.getNukeExecutablePath());
    }

    @Override
    public void apply() throws ConfigurationException {
        NukeSettings settings = NukeSettings.getInstance();
        settings.setNukeExecutablePath(myNukePathField.getText());
    }

    @Override
    public void reset() {
        NukeSettings settings = NukeSettings.getInstance();
        myNukePathField.setText(settings.getNukeExecutablePath());
    }

    @Override
    public void disposeUIResources() {
        myNukePathField = null;
    }
}
