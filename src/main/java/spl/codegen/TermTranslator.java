package spl.codegen;

import spl.model.TreeNode;
import spl.semantic.Symbol;
import spl.semantic.SymbolTable;
import spl.types.NumericKind;

import java.util.Map;
import java.util.Objects;

public final class TermTranslator {
    private final Emitter out;
    private final NameGen names;
    private final Map<Integer, TreeNode> nodes;
    private final SymbolTable symbols;
    private final String cType;

    public TermTranslator(CodegenContext ctx) {
        this(ctx.out, ctx.names, ctx.nodes, ctx.symbols, ctx.kind);
    }
    public TermTranslator(
            Emitter out,
            NameGen names,
            Map<Integer, TreeNode> nodes,
            SymbolTable symbols,
            NumericKind kind) {
        this.out = Objects.requireNonNull(out);
        this.names = Objects.requireNonNull(names);
        this.nodes = Objects.requireNonNull(nodes);
        this.symbols = Objects.requireNonNull(symbols);
        this.cType = Objects.requireNonNull(kind).cType;
    }

    public void transTerm(TreeNode term, String dest, RenameEnv env) {
        TreeNode form = childAt(term, 0);
        String contents = form.contents;

        if (contents.startsWith("#")) {
            Symbol symbol = symbols.getBinding(form.id);
            if (symbol == null) {
                throw new CodeGenException(form.id, "No symbol binding found for '" + contents + "'");
            }
            out.line(dest + " = " + env.lookup(symbol.internalName) + " ;");
            return;
        }

        if (contents.matches("[-\\u2013]?[0-9]+(\\.[0-9]+)?")) {
            out.line(dest + " = " + Emitter.number(contents) + " ;");
            return;
        }

        switch (contents) {
            case "add":
                transBinary(term, dest, env, "+");
                break;
            case "sub":
                transBinary(term, dest, env, "-");
                break;
            case "mul":
                transBinary(term, dest, env, "*");
                break;
            case "div":
                transBinary(term, dest, env, "/");
                break;
            case "mod":
                transBinary(term, dest, env, "%");
                break;
            case "neg":
                transNeg(term, dest, env);
                break;
            case "CALL":
                throw new UnsupportedOperationException("calls not implemented yet");
            default:
                throw new CodeGenException(term.id, "Unknown TERM type: " + contents);
        }
    }

    private void transBinary(TreeNode term, String dest, RenameEnv env, String operator) {
        String leftTemp = names.temp();
        String rightTemp = names.temp();
        out.line(cType + " " + leftTemp + " ;");
        out.line(cType + " " + rightTemp + " ;");
        transTerm(childAt(term, 2), leftTemp, env);
        transTerm(childAt(term, 3), rightTemp, env);
        out.line(dest + " = " + leftTemp + " " + operator + " " + rightTemp + " ;");
    }

    private void transNeg(TreeNode term, String dest, RenameEnv env) {
        String temp = names.temp();
        out.line(cType + " " + temp + " ;");
        transTerm(childAt(term, 2), temp, env);
        out.line(dest + " = - " + temp + " ;");
    }

    private TreeNode childAt(TreeNode node, int index) {
        if (index >= node.children.size()) {
            throw new CodeGenException(node.id, "Malformed TERM node");
        }
        TreeNode child = nodes.get(node.children.get(index));
        if (child == null) {
            throw new CodeGenException(node.id, "Missing tree node " + node.children.get(index));
        }
        return child;
    }
}
