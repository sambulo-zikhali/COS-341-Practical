package spl.grammar;

import spl.model.ParseAction;
import spl.model.TokenType;

public class ParsingTableTest {

    public static void main(String[] args) {

        Grammar grammar = new Grammar();

        System.out.println("======================================");
        System.out.println("      PARSING TABLE INFORMAL TEST");
        System.out.println("======================================");

        // --------------------------------------------------
        // 1. Print all grammar rules
        // --------------------------------------------------
        System.out.println("\n--- Grammar Rules ---");

        for (Rule rule : grammar.getRules()) {
            System.out.println(
                    "Rule " + rule.getId()
                    + ": " + rule
                    + " | RHS length = " + rule.length()
            );
        }

        // --------------------------------------------------
        // 2. Test a specific rule
        // --------------------------------------------------
        System.out.println("\n--- Specific Rule Test ---");

        Rule rule = grammar.getRule(23);

        System.out.println("Rule ID: " + rule.getId());
        System.out.println("LHS: " + rule.getLhs());
        System.out.println("RHS: " + rule.getRhs());
        System.out.println("Length: " + rule.length());
        System.out.println("Is epsilon: " + rule.isEpsilon());

        // --------------------------------------------------
        // 3. Test a REDUCE action
        // --------------------------------------------------
        System.out.println("\n--- Reduce Action Test ---");

        ParseAction reduceAction = ParseAction.reduce(23);

        System.out.println("Action: " + reduceAction);
        System.out.println("Kind: " + reduceAction.getKind());
        System.out.println("Rule ID: " + reduceAction.getRuleId());

        // --------------------------------------------------
        // 4. Demonstrate how the parser uses REDUCE
        // --------------------------------------------------
        System.out.println("\n--- Reduce Mapping Test ---");

        int ruleId = reduceAction.getRuleId();

        Rule reduceRule = grammar.getRule(ruleId);

        System.out.println("1. ACTION returned: " + reduceAction);
        System.out.println("2. Rule ID: " + ruleId);
        System.out.println("3. Production: " + reduceRule);
        System.out.println("4. RHS length: " + reduceRule.length());
        System.out.println(
                "5. Parser must pop "
                + reduceRule.length()
                + " symbols"
        );

        // --------------------------------------------------
        // 5. Build the actual parsing table
        // --------------------------------------------------
        System.out.println("\n--- Parsing Table Test ---");

        ParsingTableBuilder builder = new ParsingTableBuilder(grammar);
        ParsingTable table = builder.build();

        System.out.println(
                "Number of states: "
                + table.numberOfStates()
        );

        System.out.println(
                "Start state: "
                + table.getStartState()
        );

        // --------------------------------------------------
        // 6. Query ACTION(state, token)
        // --------------------------------------------------
        System.out.println("\n--- ACTION(state, token) Test ---");

        int state = 0;
        TokenType token = TokenType.NAME;

        ParseAction action = table.get(state, token);

        System.out.println(
                "ACTION(" + state + ", " + token + ") = " + action
        );

        // --------------------------------------------------
        // 7. Print every ACTION in state 0
        // --------------------------------------------------
        System.out.println("\n--- All ACTIONS in State 0 ---");

        for (TokenType tokenType : TokenType.values()) {

            ParseAction stateAction = table.get(0, tokenType);

            if (stateAction.getKind() != ParseAction.Kind.ERROR) {
                System.out.println(
                        "ACTION(0, "
                        + tokenType
                        + ") = "
                        + stateAction
                );
            }
        }

        // --------------------------------------------------
        // 8. Print all rule mappings
        // --------------------------------------------------
        System.out.println("\n--- Rule Number Mapping ---");

        for (Rule r : grammar.getRules()) {

            System.out.println(
                    r.getId()
                    + " -> "
                    + r
                    + " | pop "
                    + r.length()
                    + " symbols"
            );
        }

        System.out.println("\n======================================");
        System.out.println("             TEST COMPLETE");
        System.out.println("======================================");
    }
}
