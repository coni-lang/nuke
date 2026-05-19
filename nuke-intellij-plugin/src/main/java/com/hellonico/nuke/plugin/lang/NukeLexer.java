package com.hellonico.nuke.plugin.lang;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;

public class NukeLexer extends LexerBase {
    private CharSequence myBuffer;
    private int myStartOffset;
    private int myEndOffset;
    private int myState;

    private int myTokenStart;
    private int myTokenEnd;
    private IElementType myTokenType;

    @Override
    public void start(CharSequence buffer, int startOffset, int endOffset, int initialState) {
        myBuffer = buffer;
        myStartOffset = startOffset;
        myEndOffset = endOffset;
        myState = initialState;
        myTokenEnd = startOffset;
        advance();
    }

    @Override
    public int getState() {
        return myState;
    }

    @Override
    public IElementType getTokenType() {
        return myTokenType;
    }

    @Override
    public int getTokenStart() {
        return myTokenStart;
    }

    @Override
    public int getTokenEnd() {
        return myTokenEnd;
    }

    @Override
    public void advance() {
        if (myTokenEnd >= myEndOffset) {
            myTokenType = null;
            return;
        }

        myTokenStart = myTokenEnd;
        char c = myBuffer.charAt(myTokenStart);

        if (Character.isWhitespace(c) || c == ',') {
            myTokenType = TokenType.WHITE_SPACE;
            while (myTokenEnd < myEndOffset && (Character.isWhitespace(myBuffer.charAt(myTokenEnd)) || myBuffer.charAt(myTokenEnd) == ',')) {
                myTokenEnd++;
            }
        } else if (c == ';') {
            myTokenType = NukeTokenTypes.COMMENT;
            while (myTokenEnd < myEndOffset && myBuffer.charAt(myTokenEnd) != '\n') {
                myTokenEnd++;
            }
        } else if (c == '"') {
            myTokenType = NukeTokenTypes.STRING;
            myTokenEnd++;
            boolean escape = false;
            while (myTokenEnd < myEndOffset) {
                char nc = myBuffer.charAt(myTokenEnd);
                myTokenEnd++;
                if (escape) {
                    escape = false;
                } else if (nc == '\\') {
                    escape = true;
                } else if (nc == '"') {
                    break;
                }
            }
        } else if (c == '{') {
            myTokenType = NukeTokenTypes.BRACE1;
            myTokenEnd++;
        } else if (c == '}') {
            myTokenType = NukeTokenTypes.BRACE2;
            myTokenEnd++;
        } else if (c == '[') {
            myTokenType = NukeTokenTypes.BRACKET1;
            myTokenEnd++;
        } else if (c == ']') {
            myTokenType = NukeTokenTypes.BRACKET2;
            myTokenEnd++;
        } else if (c == '(') {
            myTokenType = NukeTokenTypes.PAREN1;
            myTokenEnd++;
        } else if (c == ')') {
            myTokenType = NukeTokenTypes.PAREN2;
            myTokenEnd++;
        } else if (c == ':') {
            myTokenType = NukeTokenTypes.KEYWORD;
            myTokenEnd++;
            while (myTokenEnd < myEndOffset && isSymbolChar(myBuffer.charAt(myTokenEnd))) {
                myTokenEnd++;
            }
        } else if (Character.isDigit(c) || (c == '-' && myTokenEnd + 1 < myEndOffset && Character.isDigit(myBuffer.charAt(myTokenEnd + 1)))) {
            myTokenType = NukeTokenTypes.NUMBER;
            myTokenEnd++;
            while (myTokenEnd < myEndOffset && (Character.isDigit(myBuffer.charAt(myTokenEnd)) || myBuffer.charAt(myTokenEnd) == '.')) {
                myTokenEnd++;
            }
        } else {
            myTokenType = NukeTokenTypes.SYMBOL;
            myTokenEnd++;
            while (myTokenEnd < myEndOffset && isSymbolChar(myBuffer.charAt(myTokenEnd))) {
                myTokenEnd++;
            }
        }
    }

    private boolean isSymbolChar(char c) {
        if (Character.isWhitespace(c) || c == ',' || c == ';' || c == '"' || c == '{' || c == '}' || c == '[' || c == ']' || c == '(' || c == ')') {
            return false;
        }
        return true;
    }

    @Override
    public CharSequence getBufferSequence() {
        return myBuffer;
    }

    @Override
    public int getBufferEnd() {
        return myEndOffset;
    }
}
