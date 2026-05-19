package com.hellonico.nuke.plugin;

import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class NukeModuleBuilder extends ModuleBuilder {
    @Override
    public void setupRootModel(@NotNull ModifiableRootModel modifiableRootModel) throws ConfigurationException {
        doAddContentEntry(modifiableRootModel);
        
        // Ensure directories exist
        String path = getContentEntryPath();
        if (path != null) {
            new File(path, "src/main").mkdirs();
            new File(path, "src/tests").mkdirs();
            new File(path, "src/main/resources").mkdirs();
            File edn = new File(path, "nuke.edn");
            if (!edn.exists()) {
                try {
                    FileWriter w = new FileWriter(edn);
                    w.write("{:name \"my-nuke-project\"\n :version \"1.0.0\"\n :main-class \"com.example.Main\"}");
                    w.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public ModuleType<?> getModuleType() {
        return StdModuleTypes.JAVA;
    }
    
    @Override
    public String getPresentableName() {
        return "Nuke Project";
    }
    
    @Override
    public String getDescription() {
        return "Creates a new Nuke-based Java project with standard directory layout and nuke.edn.";
    }
}
