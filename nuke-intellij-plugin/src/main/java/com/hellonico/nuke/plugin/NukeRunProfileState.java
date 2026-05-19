package com.hellonico.nuke.plugin;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessHandlerFactory;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import org.jetbrains.annotations.NotNull;

public class NukeRunProfileState extends CommandLineState {
    private final NukeRunConfiguration myConfiguration;

    public NukeRunProfileState(ExecutionEnvironment environment, NukeRunConfiguration configuration) {
        super(environment);
        myConfiguration = configuration;
    }

    @NotNull
    @Override
    protected ProcessHandler startProcess() throws ExecutionException {
        String basePath = myConfiguration.getProject().getBasePath();
        GeneralCommandLine cmd = new GeneralCommandLine(NukeProjectManager.getNukeExecutable(), myConfiguration.getTaskName());
        cmd.setWorkDirectory(basePath);
        
        ProcessHandler processHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(cmd);
        ProcessTerminatedListener.attach(processHandler);
        return processHandler;
    }
}
