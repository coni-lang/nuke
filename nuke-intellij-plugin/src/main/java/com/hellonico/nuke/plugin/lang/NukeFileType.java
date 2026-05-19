package com.hellonico.nuke.plugin.lang;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.icons.AllIcons;
import javax.swing.Icon;

public class NukeFileType extends LanguageFileType {
    public static final NukeFileType INSTANCE = new NukeFileType();

    private NukeFileType() {
        super(NukeLanguage.INSTANCE);
    }

    @Override
    public String getName() {
        return "Nuke File";
    }

    @Override
    public String getDescription() {
        return "Nuke configuration file";
    }

    @Override
    public String getDefaultExtension() {
        return "edn";
    }

    @Override
    public Icon getIcon() {
        return AllIcons.Nodes.ConfigFolder;
    }
}
