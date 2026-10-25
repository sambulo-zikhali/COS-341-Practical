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
    public final FunctionRegistry function;

    public ProgramTranslator program;
    public BoolTranslator bool;
    public AlgoTranslator algo;
    public CallInLiner calls;
    public TermTransltor term;
    public FunctionTranslator funcs;
}
