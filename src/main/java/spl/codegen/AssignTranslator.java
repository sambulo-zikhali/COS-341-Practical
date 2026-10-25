package spl.codegen;

import spl.model.TreeNode;
import spl.semantic.Symbol;
import spl.semantic.SymbolTable;
import spl.types.NumericKind;

import java.util.Map;
import java.util.Objects;

public final class AssignTranslator {
    private final Emitter out;
    private final NameGen names;
    private final Map<Integer, TreeNode> nodes;
    private final SymbolTable symbols;
    private final String cType;
    private final TermTranslator terms;

    public AssignTranslator(CodegenContext ctx) {
        this(ctx.out, ctx.names, ctx.nodes, ctx.symbols, ctx.kind, ctx.term);
    }

    public AssignTranslator(
            Emitter out,
            NameGen names,
            Map<Integer, TreeNode> nodes,
            SymbolTable symbols,
            NumericKind kind,
            TermTranslator terms) {
        this.out = Objects.requireNonNull(out);
        this.names = Objects.requireNonNull(names);
        this.nodes = Objects.requireNonNull(nodes);
        this.symbols = Objects.requireNonNull(symbols);
        this.cType = Objects.requireNonNull(kind).cType;
        this.terms = Objects.requireNonNull(terms);
    }

    public void transAssign(TreeNode assign, RenameEnv env) {
        String temp = names.temp();
        TreeNode nameLeaf = childAt(assign, 0);
        TreeNode termNode = childAt(assign, 2);
        Symbol symbol = symbols.getBinding(nameLeaf.id);
        if (symbol == null) {
            throw new CodeGenException(nameLeaf.id, "No symbol binding found for '" + nameLeaf.contents + "'");
        }

        out.line(cType + " " + temp + " ;");
        terms.transTerm(termNode, temp, env);
        out.line(env.lookup(symbol.internalName) + " = " + temp + " ;");
    }

    private TreeNode childAt(TreeNode node, int index) {
        if (index >= node.children.size()) {
            throw new CodeGenException(node.id, "Malformed ASSIGN node");
        }
        TreeNode child = nodes.get(node.children.get(index));
        if (child == null) {
            throw new CodeGenException(node.id, "Missing tree node " + node.children.get(index));
        }
        return child;
    }
}
