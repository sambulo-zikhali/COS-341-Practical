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
                transPrint(inner, env);
                break;
            case "nop":
                ctx.out.line("/* nop */");
                break;
            case "comment":
                TreeNode strNode = ctx.kid(inner, 1);
                ctx.out.line("/* " + Emitter.stringLiteral(strNode.contents) + " */");
                break;
            case "ASSIGN":
                ctx.term.transAssign(inner, env);
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

    private void transPrint(TreeNode innerNode, RenameEnv env) {
        TreeNode outp = ctx.kid(innerNode, 1);
        
        if (outp.contents.equals("STRING")) {
            TreeNode strNode = ctx.kid(outp, 0);
            ctx.out.line("std::cout << " + Emitter.stringLiteral(strNode.contents) + " ;");
        } else {
            TreeNode term = ctx.kid(outp, 1);
            String tN = ctx.names.temp();
            
            ctx.out.decl(tN);
            ctx.term.transTerm(term, tN, env);
            ctx.out.line("std::cout << " + tN + " ;");
        }
    }
}