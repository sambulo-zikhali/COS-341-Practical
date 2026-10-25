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

    public void transCall(TreeNode call, String contVar, RenameEnv env) {
        FunctionRegistry.FunctionInfo info = ctx.functions.lookup(call);
        TreeNode input = ctx.kid(call, 2);
        List<TreeNode> args = collectTerms(input);
        if (args.size() != info.parameters.size()) {
            throw new CodeGenException(call.id, "Argument count mismatch: expected "
                    + info.parameters.size() + ", got " + args.size());
        }

        List<String> captures = new ArrayList<>();
        for (TreeNode arg : args) {
            String capture = ctx.names.capture();
            captures.add(capture);
            ctx.out.decl(capture);
            ctx.term.transTerm(arg, capture, env);
        }

        RenameEnv calleeEnv = env.child();
        for (Symbol symbol : info.ownVariables) {
            calleeEnv.bind(symbol, symbol.internalName + ctx.names.next("k"));
        }

        for (Symbol parameter : info.parameters) {
            ctx.out.decl(calleeEnv.nameOf(parameter));
        }
        for (int i = 0; i < info.parameters.size(); i++) {
            String parameterName = calleeEnv.nameOf(info.parameters.get(i));
            ctx.out.line(parameterName + " = " + captures.get(i) + " ;");
        }

        ctx.program.transBody(info.body, calleeEnv, false);

        if (info.returnTerm != null) {
            String returnName = ctx.names.ret();
            ctx.out.decl(returnName);
            ctx.term.transTerm(info.returnTerm, returnName, calleeEnv);
            if (contVar != null) {
                ctx.out.line(contVar + " = " + returnName + " ;");
            }
        }
    }

    private List<TreeNode> collectTerms(TreeNode input) {
        List<TreeNode> terms = new ArrayList<>();
        TreeNode current = input;
        while (!current.children.isEmpty()) {
            terms.add(ctx.kid(current, 0));
            current = ctx.kid(current, 1);
        }
        return terms;
    }
}
