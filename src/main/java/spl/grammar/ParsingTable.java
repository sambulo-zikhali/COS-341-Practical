package spl.grammar;

import spl.model.ParseAction;
import spl.model.TokenType;

public class ParsingTable {
    // stub for function returning parse action for a given token and dfa state
    public ParseAction get(int state, TokenType type) {
        return new ParseAction(null, state);
    }
}
