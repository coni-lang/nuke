package com.hellonico.nuke.plugin;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

@State(
    name = "NukeSettings",
    storages = @Storage("NukeSettings.xml")
)
public class NukeSettings implements PersistentStateComponent<NukeSettings.State> {
    public static class State {
        public String nukeExecutablePath = "/Users/nico/cool/nuke/nuke";
    }

    private State myState = new State();

    public static NukeSettings getInstance() {
        return ApplicationManager.getApplication().getService(NukeSettings.class);
    }

    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(State state) {
        myState = state;
    }
    
    public String getNukeExecutablePath() {
        return myState.nukeExecutablePath;
    }
    
    public void setNukeExecutablePath(String path) {
        myState.nukeExecutablePath = path;
    }
}
