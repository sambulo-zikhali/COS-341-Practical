package spl.grammar;


import java.util.List;
import java.util.Map;

import spl.model.ParseAction;
import spl.model.TokenType;


public class ParsingTable {

    private final int startState;
    private final Map<Integer,Map<TokenType,ParseAction>> actionTable;
    private final Map<Integer,Map<String, Integer>> gotoTable;
    private final List<Rule> rules;

    ParsingTable(int startState,Map<Integer,Map<TokenType,ParseAction>>actionTable,Map<Integer,Map<String,Integer>> gotoTable,List<Rule> rules)
    {
        this.startState = startState;
        this.actionTable = actionTable;
        this.gotoTable= gotoTable;
        this.rules=rules;
    }

    public int getStartState() {
        return startState;
    }

/** ACTION(state, token). Returns ParseAction.error(). */
    public ParseAction get(int state,TokenType token) {
        Map<TokenType,ParseAction> row = actionTable.get(state);
        if(row == null)
        {
            return ParseAction.error();
        }
        return row.getOrDefault(token, ParseAction.error());
    }

/** GOTO(state, nonTerminal), used after a reduce to find the next state*/
    public int getGoto(int state,String nonTerminal) {
        Map<String,Integer> row = gotoTable.get(state);
        if(row == null)
        {
            return -1;
        }
        return row.getOrDefault(nonTerminal, -1);
    }

/** Look up a production by its rules and stuff */
    public Rule getRule(int ruleId) {
        return rules.get(ruleId);
    }

    public int numberOfStates() {
        return actionTable.size();
    }
}
