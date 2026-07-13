package com.hellonico.nuke.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class NukeStartupActivity implements StartupActivity.DumbAware {
    @Override
    public void runActivity(@NotNull Project project) {
        com.intellij.openapi.project.DumbService.getInstance(project).runWhenSmart(() -> {
            NukeProjectManager.sync(project);
        });
    }
}
