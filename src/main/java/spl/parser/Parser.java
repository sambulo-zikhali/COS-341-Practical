package spl.parser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import spl.grammar.ParsingTable;
import spl.grammar.Rule;
import spl.model.ParseAction;
import spl.model.Token;
import spl.model.TokenType;
import spl.model.TreeNode;

public class Parser {
    private static final Token EOF_TOKEN = new Token(TokenType.EOF, "EOF", -1);

    private final ParsingTable table;
    private final NodeIdGenerator ids;

    private final Map<Integer, TreeNode> nodesById = new HashMap<>();

    public Parser(ParsingTable table, NodeIdGenerator ids) {
        this.table = table;
        this.ids = ids;
    }

    public Map<Integer, TreeNode> getNodesById() {
        return nodesById;
    }

    public TreeNode parse(List<Token> tokenList) {
        nodesById.clear();

        Deque<Token> tokens = new ArrayDeque<>(tokenList);

        Stack<Integer> stateStack = new Stack<>();
        Stack<Integer> nodeStack = new Stack<>();

        // Push the initial state so the first peek() doesn't crash
        stateStack.push(table.getStartState());

        Token lookahead = tokens.isEmpty() ? EOF_TOKEN : tokens.peek();

        while (true) {
            int state = stateStack.peek();
            ParseAction action = table.get(state, lookahead.type);

            if (action == null || action.kind == ParseAction.Kind.ERROR) {
                String expected = expectedFrom(state);
                throw new ParserException(lookahead.line, expected, lookahead.text);
            }

            switch (action.kind) {
                case SHIFT:
                    int id = ids.next();
                    TreeNode leaf = new TreeNode(id, lookahead.text);
                    nodesById.put(id, leaf);

                    nodeStack.push(id);
                    stateStack.push(action.value);

                    tokens.poll();
                    lookahead = tokens.isEmpty() ? EOF_TOKEN : tokens.peek();

                    break;
                case REDUCE:
                    Rule rule = table.getRule(action.value);
                    int rhsLen = rule.length();

                    int[] childIds = new int[rhsLen];
                    for (int i = rhsLen - 1; i >= 0; i--) {
                        childIds[i] = nodeStack.pop();
                    }

                    int newId = ids.next();
                    TreeNode inner = new TreeNode(newId, rule.getLhs());
                    for (int childId : childIds) {
                        inner.children.add(childId);

                        TreeNode child = nodesById.get(childId);
                        if (child == null) {
                            throw new IllegalStateException("Parser bug: node" + childId + " was never registered.");
                        }
                        child.parent = newId;
                    }
                    nodesById.put(newId, inner);
                    nodeStack.push(newId);

                    for (int i = 0; i < rhsLen; i++) {
                        stateStack.pop();
                    }
                    int gotoState = table.getGoto(stateStack.peek(), rule.getLhs());
                    if (gotoState < 0) {
                        throw new ParserException(lookahead.line, "valid GOTO for " + rule.getLhs(), lookahead.text);
                    }
                    stateStack.push(gotoState);

                    break;
                case ACCEPT:
                    if (nodeStack.size() != 1) {
                        throw new IllegalStateException(
                                "Parser bug: ACCEPT with " + nodeStack.size() + " nodes on stack (expected 1).");
                    }
                    int rootId = nodeStack.pop();
                    return nodesById.get(rootId);

                case GOTO:
                case ERROR:
                default:
                    throw new ParserException(lookahead.line, "SHIFT, REDUCE, or ACCEPT", lookahead.text);
            }
        }
    }

    /**
     * Inspects the ACTION table for the given state and returns a
     * human-readable string listing every token that has a valid
     * (non-error) action — i.e. what the parser was expecting to see.
     */
    private String expectedFrom(int state) {
        List<String> expected = new ArrayList<>();
        for (TokenType tt : TokenType.values()) {
            ParseAction a = table.get(state, tt);
            if (a != null && a.kind != ParseAction.Kind.ERROR) {
                // Use the grammar symbol (e.g. "print", "NUM", "(") rather than the enum name
                expected.add(tt.getGrammarType());
            }
        }
        if (expected.isEmpty()) {
            return "<nothing — parser is in an invalid state>";
        }
        return String.join(", ", expected);
    }
}
