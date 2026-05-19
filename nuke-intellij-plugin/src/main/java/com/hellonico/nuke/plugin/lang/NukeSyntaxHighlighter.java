package com.hellonico.nuke.plugin.lang;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class NukeSyntaxHighlighter extends SyntaxHighlighterBase {
    public static final TextAttributesKey KEYWORD = TextAttributesKey.createTextAttributesKey("NUKE_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey STRING = TextAttributesKey.createTextAttributesKey("NUKE_STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey NUMBER = TextAttributesKey.createTextAttributesKey("NUKE_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey COMMENT = TextAttributesKey.createTextAttributesKey("NUKE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey SYMBOL = TextAttributesKey.createTextAttributesKey("NUKE_SYMBOL", DefaultLanguageHighlighterColors.IDENTIFIER);

    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        return new NukeLexer();
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (tokenType.equals(NukeTokenTypes.KEYWORD)) return new TextAttributesKey[]{KEYWORD};
        if (tokenType.equals(NukeTokenTypes.STRING)) return new TextAttributesKey[]{STRING};
        if (tokenType.equals(NukeTokenTypes.NUMBER)) return new TextAttributesKey[]{NUMBER};
        if (tokenType.equals(NukeTokenTypes.COMMENT)) return new TextAttributesKey[]{COMMENT};
        if (tokenType.equals(NukeTokenTypes.SYMBOL)) return new TextAttributesKey[]{SYMBOL};
        return new TextAttributesKey[0];
    }
}
