package spl.codegen;

import spl.model.TreeNode;
import spl.semantic.SymbolTable;
import spl.types.NumericKind;

import java.util.Map;

public final class CodeGenerator {
    private CodeGenerator() { }

    public static String generate(TreeNode root,Map<Integer,TreeNode>nodes,SymbolTable symbols,NumericKind kind) {
        throw new UnsupportedOperationException("code generation not wired up yet");
    }
}
