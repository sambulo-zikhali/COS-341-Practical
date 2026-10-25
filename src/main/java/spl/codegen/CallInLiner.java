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

    public void transCall(TreeNode callNode, String contVar, RenameEnv env) {
        // TODO: Sambulo must implement FunctionRegistry.lookup(callNode) 
        // and return a FunctionInfo object. For now, this will not compile 
        // until he adds it.
        FunctionRegistry.FunctionInfo info = ctx.functions.lookup(callNode);
        
        TreeNode nameNode = ctx.kid(callNode, 0);
        TreeNode inputNode = ctx.kid(callNode, 2);
        List<TreeNode> args = collectTerms(inputNode);
        
        List<Symbol> params = info.getParameters();
        if (args.size() != params.size()) {
            throw new CodeGenException(callNode.id, 
                "Argument count mismatch for " + nameNode.contents + 
                ". Expected " + params.size() + ", got " + args.size());
        }

        List<String> pNames = new ArrayList<>();
        for (TreeNode arg : args) {
            String p = ctx.names.capture();
            pNames.add(p);
            ctx.out.decl(p);
            ctx.term.transTerm(arg, p, env);
        }

        // TODO: Sambulo must implement RenameEnv(RenameEnv parent, TreeNode functionNode)
        // and RenameEnv.nameOf(Symbol) for this to work.
        RenameEnv calleeEnv = new RenameEnv(env, info.getFunctionNode());

        for (int i = 0; i < args.size(); i++) {
            Symbol paramSym = params.get(i);
            // Using the helper below until RenameEnv has nameOf(Symbol)
            String paramName = renameSymbol(paramSym, calleeEnv);
            ctx.out.line(paramName + " = " + pNames.get(i) + " ;");
        }

        ctx.program.transBody(info.getBody(), calleeEnv, false);

        if (contVar != null) {
            String r = info.getReturnVar(calleeEnv); 
            ctx.term.transTerm(info.getReturnTerm(), r, calleeEnv);
            ctx.out.line(contVar + " = " + r + " ;");
        }
    }

    private List<TreeNode> collectTerms(TreeNode inputNode) {
        List<TreeNode> terms = new ArrayList<>();
        TreeNode current = inputNode;
        while (!current.children.isEmpty()) {
            terms.add(ctx.kid(current, 0));
            current = ctx.kid(current, 1);
        }
        return terms;
    }

    // Helper method to call your current RenameEnv.lookup(String)
    private String renameSymbol(Symbol sym, RenameEnv env) {
        // Symbol.internalName is already unique from Phase 2a (e.g., "v0").
        // The env.lookup() checks if this copy of the function renames it.
        return env.lookup(sym.internalName);
    }
}
