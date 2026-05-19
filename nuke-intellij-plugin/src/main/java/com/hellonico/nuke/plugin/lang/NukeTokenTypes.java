package com.hellonico.nuke.plugin.lang;

import com.intellij.psi.tree.IElementType;

public interface NukeTokenTypes {
    IElementType KEYWORD = new NukeTokenType("KEYWORD"); // e.g. :name
    IElementType STRING = new NukeTokenType("STRING"); // "hello"
    IElementType NUMBER = new NukeTokenType("NUMBER");
    IElementType BRACE1 = new NukeTokenType("BRACE1"); // {
    IElementType BRACE2 = new NukeTokenType("BRACE2"); // }
    IElementType BRACKET1 = new NukeTokenType("BRACKET1"); // [
    IElementType BRACKET2 = new NukeTokenType("BRACKET2"); // ]
    IElementType PAREN1 = new NukeTokenType("PAREN1"); // (
    IElementType PAREN2 = new NukeTokenType("PAREN2"); // )
    IElementType SYMBOL = new NukeTokenType("SYMBOL"); // any identifier
    IElementType COMMENT = new NukeTokenType("COMMENT"); // ; comment
    IElementType LIST = new NukeTokenType("LIST"); // grouped node
}
