package spl.codegen;

import spl.model.TreeNode;
import spl.semantic.Symbol;
import spl.semantic.SymbolTable;
import spl.types.NumericKind;

import java.util.Map;


public final class CodegenContext {
    public final Map<Integer,TreeNode> nodes;
    public final SymbolTable symbols;
    public final NumericKind kind;
    public final NameGen names = new NameGen();
    public final Emitter out;
    public final FunctionRegistry functions;

    public ProgramTranslator program;
    public BoolTranslator bool;
    public AlgoTranslator algo;
    public CallInLiner calls;
    public TermTranslator term;
    public AssignTranslator assign;
    public FunctionTranslator funcs;

    public CodegenContext(Map<Integer,TreeNode> nodes, SymbolTable symbols,NumericKind kind) {
        this.nodes = nodes;
        this.symbols = symbols;
        this.kind = kind;
        this.out = new Emitter(kind.cType);
        this.functions = new FunctionRegistry(nodes,symbols);
    }

    public TreeNode kid(TreeNode n, int i) {
        return nodes.get(n.children.get(i));
    }

    public Symbol symbolOf(TreeNode nameLeaf) {
        Symbol s = symbols.getBinding(nameLeaf.id);
        if(s == null) s = symbols.getSymbol(nameLeaf.id);
        if(s == null) {
            throw new IllegalStateException("No symbol for name leaf" + nameLeaf.id+ " "+ nameLeaf.contents);
        }
        return s;
    }

    public String nameOf(TreeNode nameLeaf,RenameEnv env) {
        return env.nameOf(symbolOf(nameLeaf));
    }
}
