package spl.codegen;

import spl.model.TreeNode;
import spl.semantic.Symbol;

import java.util.ArrayList;
import java.util.List;

public class CallInLiner {
    private final CodegenContext ctx;

    public CallInLiner(CodegenContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Translates a CALL node.
     * @param callNode The CALL node
     * @param contVar The continuation variable (null if called as an INSTR)
     * @param env The current renaming environment
     */
    public void transCall(TreeNode callNode, String contVar, RenameEnv env) {
        FunctionRegistry.FunctionInfo info = ctx.registry.lookup(callNode);
        
        // 2. Create a new environment for the callee
        RenameEnv calleeEnv = new RenameEnv(env, info.getFunctionNode());
        
        // 3. Capture arguments
        TreeNode inputNode = ctx.nodes.get(callNode.children.get(2));
        List<TreeNode> args = collectTerms(inputNode);
        
        if (args.size() != info.getParameters().size()) {
            throw new CodeGenException(callNode.id, 
                "Argument count mismatch for " + info.getInternalName() + 
                ". Expected " + info.getParameters().size() + ", got " + args.size());
        }

        List<String> pNames = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            String p = ctx.nameGen.fresh("p");
            pNames.add(p);
            ctx.out.decl(ctx.type, p);
            // Evaluate argument in caller env
            ctx.term.transTerm(args.get(i), p, env);
        }

        // 4. Emit parameter assignments (Section 2, Problem 5: param = p)
        for (int i = 0; i < args.size(); i++) {
            Symbol paramSym = info.getParameters().get(i);
            String paramName = calleeEnv.nameOf(paramSym);
            ctx.out.line(paramName + " = " + pNames.get(i) + ";");
        }

        // 5. Inline the body
        ctx.program.transBody(info.getBody(), calleeEnv, false);

        // 6. Handle return value
        if (contVar != null) {
            String r = info.getReturnVar(calleeEnv); // e.g., "r_f123"
            ctx.term.transTerm(info.getReturnTerm(), r, calleeEnv);
            ctx.out.line(contVar + " = " + r + ";");
        }
    }

    private List<TreeNode> collectTerms(TreeNode inputNode) {
        List<TreeNode> terms = new ArrayList<>();
        TreeNode current = inputNode;
        while (!current.children.isEmpty()) {
            // INPUT -> TERM INPUT
            terms.add(ctx.nodes.get(current.children.get(0)));
            current = ctx.nodes.get(current.children.get(1));
        }
        return terms;
    }
}