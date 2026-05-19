package com.hellonico.nuke.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

public class NukeSyncAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project != null) {
            NukeProjectManager.sync(project);
        }
    }
}
