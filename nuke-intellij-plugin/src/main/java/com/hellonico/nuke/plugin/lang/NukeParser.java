package com.hellonico.nuke.plugin.lang;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;

public class NukeParser implements PsiParser {
    @Override
    public ASTNode parse(IElementType root, PsiBuilder builder) {
        PsiBuilder.Marker mark = builder.mark();
        parseList(builder);
        mark.done(root);
        return builder.getTreeBuilt();
    }

    private void parseList(PsiBuilder builder) {
        while (!builder.eof()) {
            IElementType type = builder.getTokenType();
            if (type == NukeTokenTypes.BRACE1 || type == NukeTokenTypes.BRACKET1 || type == NukeTokenTypes.PAREN1) {
                PsiBuilder.Marker m = builder.mark();
                builder.advanceLexer();
                parseList(builder);
                m.done(NukeTokenTypes.LIST);
            } else if (type == NukeTokenTypes.BRACE2 || type == NukeTokenTypes.BRACKET2 || type == NukeTokenTypes.PAREN2) {
                builder.advanceLexer();
                return;
            } else {
                builder.advanceLexer();
            }
        }
    }
}
