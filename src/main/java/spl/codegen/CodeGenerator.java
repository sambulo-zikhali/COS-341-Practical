package spl.codegen;

import spl.model.TreeNode;
import spl.semantic.SymbolTable;
import spl.types.NumericKind;

import java.util.Map;

public final class CodeGenerator {
    private CodeGenerator() { }

    public static String generate(TreeNode root,Map<Integer,TreeNode>nodes,SymbolTable symbols,NumericKind kind) {
        CodegenContext ctx = new CodegenContext(nodes,symbols,kind);
        ctx.program = new ProgramTranslator(ctx);
        ctx.bool = new BoolTranslator(ctx);
        ctx.algo = new AlgoTranslator(ctx);
        ctx.calls = new CallInLiner(ctx);
        ctx.term = new TermTranslator(ctx);
        ctx.funcs= new FunctionTranslator(ctx);
        ctx.program.transProgram(root);
        return ctx.out.toString();
    }
}
