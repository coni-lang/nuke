package com.hellonico.nuke.plugin;

import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.Project;

import java.util.List;

public class NukeFileListener implements BulkFileListener {
    @Override
    public void after(List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
            if (event.getFile() != null && event.getFile().getName().equals("nuke.edn")) {
                for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                    String basePath = project.getBasePath();
                    if (basePath != null && event.getFile().getPath().startsWith(basePath)) {
                        NukeProjectManager.sync(project);
                        break;
                    }
                }
            }
        }
    }
}
