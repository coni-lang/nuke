package com.hellonico.nuke.plugin.lang;

import com.intellij.psi.tree.IElementType;

public class NukeTokenType extends IElementType {
    public NukeTokenType(String debugName) {
        super(debugName, NukeLanguage.INSTANCE);
    }
}
