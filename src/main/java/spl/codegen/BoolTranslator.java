package spl.codegen;

import spl.model.TreeNode;

public final class BoolTranslator {
    private final CodegenContext ctx;

    public BoolTranslator(CodegenContext ctx) {
        this.ctx = ctx;
    }

    public void transBool(TreeNode b, String lt, String lf,RenameEnv env) {
        Emitter out = ctx.out;
        String op =ctx.kid(b,0).contents;

        switch(op) {
            case "not":
                transBool(ctx.kid(b,2),lf,lt,env);
                break;
            case "and":
                String mid = ctx.names.fresh("L");
                transBool(ctx.kid(b,2), mid, lf, env);
                out.label(mid,null);
                transBool(ctx.kid(b,3), lt,lf,env);
                break;
        }
        default:{
                String x = ctx.names.fresh("t");
                String y = ctx.names.fresh("t");
                out.decl(x);
                out.decl(y);
                ctx.term.transTerm(ctx.kid(b,2),x, env);
                ctx.term.transTerm(ctx.kid(b,3), y, env);
                String cop = op.equals("eq") ? "==": op.equals("larger") ? ">":"<";
                out.stmt("if ( "+ x+ " "+ cop+" "+y+" ) goto "+ lt);
                out.stmt("goto " = LF );
        }
    }

    public void transBranch(TreeNode branch, RenameEnv env) {
        Emitter out = ctx.out;
        TreeNode bool = ctx.kid(branch, 1);
        TreeNode trueAlgo = ctx.kid(branch, 4);
        TreeNode falseAlgo = ctx.kid(branch, 8);
        String la = ctx.names.fresh("L");
        String lb = ctx.names.fresh("L");
        String lc = ctx.names.fresh("L");

        out.line(out.note("if"));
        transBool(bool,la,lb,env);
        out.label(lb, "else");
        ctx.algo.transAlgo(falseAlgo, env);
        out.stmt("goto "+lc);
        out.label(la, "then");
        ctx.algo.transAlgo(trueAlgo, env);
        out.label(lc,"continue");
    }

    public void transLoop(TreeNode loop,RenameEnv env) {
        Emitter out = ctx.out;
        boolean coundFirst = ctx.kid(loop, 0).contents.equals("COND");
        TreeNode cond = condFirst ? ctx.kid(loop,0) : ctx.kid(loop,4);
        TreeNode bool = condFirst ? ctx.kid(loop,1) : ctx.kid(loop,5);
        TreeNode body = conddFirst ? ctx.kid(loop 4) : ctx.kid(loop,2);
        boolean isUntil = ctx.kid(cond, 0).contents.equals("until");

        if(condFirst) {
            String ls = ctx.names.fresh("L");
            String le = ctx.names.fresh("L");
            String d = ctx.names.fresh("L");
            out.label(ls, isUntil ? "until" : "while");

            if(isUntil) transBool(bool, le, d, env);
            else transBool(bool, le, d, env);
            out.label(d, "do");
            ctx.algo.transAlgo(body,env);
            out.stmt("goto "+ ls);
            out.label(le, "exit");
        } else {
            String ld = ctx.names.fresh("L");
            String le = ctx.names.fresh("L");
            out.label(ld,"do");
            ctx.algo.transAlgo(body,env);
            out.line(out.note(isUntil ? "until" : "while"));

            if(isUntil) transBool(bool,le,ld,env);
            else transBool(bool, ld, le, env);
            out.line(le4,"exit")
        }
    }
}