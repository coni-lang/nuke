package com.hellonico.nuke.plugin;

import com.intellij.execution.filters.ConsoleFilterProvider;
import com.intellij.execution.filters.Filter;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class NukeConsoleFilterProvider implements ConsoleFilterProvider {
    @NotNull
    @Override
    public Filter[] getDefaultFilters(@NotNull Project project) {
        return new Filter[]{new NukeConsoleFilter(project)};
    }
}
