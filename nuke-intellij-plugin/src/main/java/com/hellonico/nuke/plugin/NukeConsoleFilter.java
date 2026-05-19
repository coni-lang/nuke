package com.hellonico.nuke.plugin;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NukeConsoleFilter implements Filter {
    private final Project project;
    // Regex matches /absolute/path/file.ext:line:column
    // Example: /Users/nico/cool/npkm/nuke/example-java-app/src/main/com/example/Main.java:8:41
    private final Pattern pattern = Pattern.compile("(/[^:]+\\.[a-zA-Z0-9]+):(\\d+):(\\d+)");

    public NukeConsoleFilter(Project project) {
        this.project = project;
    }

    @Nullable
    @Override
    public Result applyFilter(String line, int entireLength) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String path = matcher.group(1);
            int lineNumber = Integer.parseInt(matcher.group(2)) - 1; // 0-indexed
            int column = Integer.parseInt(matcher.group(3)) - 1;

            VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
            if (file != null) {
                int startPoint = entireLength - line.length() + matcher.start(1);
                int endPoint = entireLength - line.length() + matcher.end(3);
                
                return new Result(startPoint, endPoint, new OpenFileHyperlinkInfo(project, file, lineNumber, column));
            }
        }
        return null;
    }
}
