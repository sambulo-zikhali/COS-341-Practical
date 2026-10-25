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
        TreeNode instr = ctx.nodes.get(node.children.get(0));
        TreeNode nextAlgo = ctx.nodes.get(node.children.get(2));

        transInstr(instr, env);
        transAlgo(nextAlgo, env);
    }

    private void transInstr(TreeNode node, RenameEnv env) {
        TreeNode inner = ctx.nodes.get(node.children.get(0));
        String type = inner.contents;

        switch (type) {
            case "print":
                transPrint(node, inner, env);
                break;
            case "nop":
                ctx.out.line("/* nop */");
                break;
            case "comment":
                TreeNode strNode = ctx.nodes.get(inner.children.get(1));
                ctx.out.line("/* " + strNode.contents + " */");
                break;
            case "ASSIGN":
                ctx.term.transAssign(inner, env); // Delegating to TermTranslator
                break;
            case "BRANCH":
                ctx.bool.transBranch(inner, env); // Delegating to Divo's BoolTranslator
                break;
            case "LOOP":
                ctx.bool.transLoop(inner, env);   // Delegating to Divo's BoolTranslator
                break;
            case "CALL":
                ctx.calls.transCall(inner, null, env); // Delegating to your CallInLiner
                break;
            default:
                throw new CodeGenException(node.id, "Unknown INSTR type: " + type);
        }
    }

    private void transPrint(TreeNode instrNode, TreeNode innerNode, RenameEnv env) {
        TreeNode outp = ctx.nodes.get(innerNode.children.get(1));
        
        if (outp.contents.equals("STRING")) {
            TreeNode strNode = ctx.nodes.get(outp.children.get(0));
            ctx.out.line("std::cout << " + strNode.contents + ";");
        } else {
            TreeNode term = ctx.nodes.get(outp.children.get(1));
            String tN = ctx.nameGen.fresh("t");
            
            ctx.out.decl(ctx.type, tN);
            ctx.term.transTerm(term, tN, env);
            ctx.out.line("std::cout << " + tN + ";");
        }
    }
}