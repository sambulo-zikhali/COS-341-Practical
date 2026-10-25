package spl.codegen;

import spl.model.TreeNode;
import spl.semantic.Symbol;

public final class ProgramTranslator {
    private final CodegenContext ctx;

    public ProgramTranslator(CodegenContext ctx) {
        this.ctx = ctx;
    }

    public void transProgram(TreeNode splProg) {
        Emitter out = ctx.out;
        TreeNode p = ctx.kid(splProg, 0);

        out.line("/* begin of the non-executable code-mine");
        out.beginMine();
        ctx.funcs.transFDecl(ctx.kid(p,2));
        out.endMine();
        out.line("end of the non-executable code-mine */");

        out.line("/* begin of executaable C code */");
        out.line("#include <iostream>");
        out.line("int main()");
        out.line("{");
        RenameEnv env = RenameEnv.identity();
        transVDecl(ctx.kid(p,0), env);
        ctx.algo.transAlgo(ctx.kid(p,4), env);
        out.line("return 0;");
        out.line("} /* end of executable C */");
    }

    public void transBody(TreeNode p, RenameEnv env, boolean withMine) {
        if(withMine) {
            if(!ctx.out.inMine()) throw new IllegalStateException
            ("withMine outside mine");
                ctx.funcs.transFDecl(ctx.kid(p,2));
        }
        transVDecl(ctx.kid(p,0), env);
        ctx.algo.transAlgo(ctx.kid(p,4), env);
    }

    //one declaration per name and a blank space
    public void transVDecl(TreeNode vDecl, RenameEnv env) {
        TreeNode cur = vDecl;
        while(!cur.children.isEmpty()) {
            Symbol s = ctx.SynbolOf(ctx.kid(cur,0));
            ctx.out.decl(env.nameOf(s));
            cur = ctx.kid(cur, 1);
        }
        ctx.out.line(" ");
    }
}
