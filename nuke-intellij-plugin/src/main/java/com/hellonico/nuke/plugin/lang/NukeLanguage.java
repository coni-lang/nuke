package com.hellonico.nuke.plugin.lang;

import com.intellij.lang.Language;

public class NukeLanguage extends Language {
    public static final NukeLanguage INSTANCE = new NukeLanguage();

    private NukeLanguage() {
        super("Nuke");
    }
}
