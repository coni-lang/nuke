package com.hellonico.nuke.plugin;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class NukeReloadFileAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    private static boolean hasNukeEdn(Project project) {
        if (project == null || project.getBasePath() == null) return false;
        return new File(project.getBasePath(), "nuke.edn").exists();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Show whenever this is a Nuke project (has nuke.edn at the root)
        e.getPresentation().setEnabledAndVisible(hasNukeEdn(e.getProject()));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project != null) {
            NukeProjectManager.sync(project);
        }
    }
}
