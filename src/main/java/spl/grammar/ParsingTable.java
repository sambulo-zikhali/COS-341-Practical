package spl.grammar;

import java.util.Map;

import spl.model.ParseAction;
import spl.model.TokenType;

public class ParsingTable {
    // stubs for table endpoints
    public int getStartState() {
        return -1;
    }

    public ParseAction get(int state, TokenType token) {
        return ParseAction.error();
    }

    public int getGoto(int state, String nonTerminal) {
        return -1;
    }

    public Rule getRule(int ruleId) {
        return new Rule();
    }

    public int numberOfStates() {
        return -1;
    }
}
