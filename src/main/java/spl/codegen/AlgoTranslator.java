package spl.codegen;

import spl.model.TreeNode;

public class AlgoTranslator {
    private final CodegenContext ctx;

    public AlgoTranslator(CodegenContext ctx) {
        this.ctx = ctx;
    }

    public void transAlgo(TreeNode node, RenameEnv env) {
        if (node.children.isEmpty()) {
            return;
        }
        // ALGO -> INSTR ; ALGO
        TreeNode instr = ctx.kid(node, 0);
        TreeNode nextAlgo = ctx.kid(node, 2);

        transInstr(instr, env);
        transAlgo(nextAlgo, env);
    }

    private void transInstr(TreeNode node, RenameEnv env) {
        TreeNode inner = ctx.kid(node, 0);
        String type = inner.contents;

        switch (type) {
            case "print":
                transPrint(ctx.kid(node, 1), env);
                break;
            case "nop":
                ctx.out.line(ctx.out.note("nop"));
                break;
            case "comment":
                TreeNode strNode = ctx.kid(node, 1);
                String comment = strNode.contents.substring(1, strNode.contents.length() - 1);
                ctx.out.line(ctx.out.note(comment));
                break;
            case "ASSIGN":
                ctx.assign.transAssign(inner, env);
                break;
            case "BRANCH":
                ctx.bool.transBranch(inner, env);
                break;
            case "LOOP":
                ctx.bool.transLoop(inner, env);
                break;
            case "CALL":
                ctx.calls.transCall(inner, null, env);
                break;
            default:
                throw new CodeGenException(node.id, "Unknown INSTR type: " + type);
        }
    }

    private void transPrint(TreeNode outp, RenameEnv env) {

        if (ctx.kid(outp, 0).contents.equals("(")) {
            TreeNode term = ctx.kid(outp, 1);
            String tN = ctx.names.temp();

            ctx.out.decl(tN);
            ctx.term.transTerm(term, tN, env);
            ctx.out.line("std::cout << " + tN + " ;");
        } else {
            String token = ctx.kid(outp, 0).contents;
            ctx.out.line("std::cout << " + Emitter.stringLiteral(token) + " ;");
        }
    }
}
