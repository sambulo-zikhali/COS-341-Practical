package spl.parser;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import spl.grammar.ParsingTable;
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

        Token lookahead = tokens.isEmpty() ? EOF_TOKEN : tokens.peek();

        while (true) {
            int state = stateStack.peek();
            ParseAction action = table.get(state, lookahead.type);

            if (action == null || action.kind == ParseAction.Kind.ERROR) {
                throw new ParserException(lookahead.line, null, lookahead.text);
            }

            switch (action.kind) {
                case SHIFT:
                    break;
                case REDUCE:
                    break;
                case ACCEPT:
                    break;
                case GOTO:
                case ERROR:
                default:
                    break;
            }
        }
    }
}
