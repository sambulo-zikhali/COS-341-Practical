package spl.codegen;

import spl.model.TreeNode;
import spl.semantic.Symbol;
import spl.semantic.SymbolTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FunctionRegistry {
    private final Map<Integer, TreeNode> nodes;
    private final SymbolTable symbols;

    public FunctionRegistry(Map<Integer, TreeNode> nodes, SymbolTable symbols) {
        this.nodes = Objects.requireNonNull(nodes);
        this.symbols = Objects.requireNonNull(symbols);
    }

    public FunctionInfo lookup(TreeNode callNode) {
        TreeNode nameLeaf = childAt(callNode, 0);
        Symbol function = symbols.getBinding(nameLeaf.id);
        if (function == null) {
            throw new CodeGenException(nameLeaf.id, "No function binding found for '" + nameLeaf.contents + "'");
        }

        TreeNode declarationLeaf = nodes.get(function.declarationNodeId);
        if (declarationLeaf == null || declarationLeaf.parent == null) {
            throw new CodeGenException(nameLeaf.id, "Missing declaration for function '" + nameLeaf.contents + "'");
        }
        TreeNode functionType = nodes.get(declarationLeaf.parent);
        if (functionType == null) {
            throw new CodeGenException(nameLeaf.id, "Missing function declaration for '" + nameLeaf.contents + "'");
        }

        TreeNode parametersNode = childAt(functionType, 3);
        TreeNode body = childAt(functionType, 6);
        List<Symbol> parameters = new ArrayList<>();
        TreeNode current = parametersNode;
        while (!current.children.isEmpty()) {
            TreeNode parameterLeaf = childAt(current, 0);
            Symbol parameter = symbols.getSymbol(parameterLeaf.id);
            if (parameter == null) {
                throw new CodeGenException(parameterLeaf.id,
                        "No symbol found for parameter '" + parameterLeaf.contents + "'");
            }
            parameters.add(parameter);
            current = childAt(current, 1);
        }

        TreeNode returnTerm = null;
        if ("num".equals(childAt(functionType, 0).contents)) {
            returnTerm = childAt(functionType, 9);
        }

        List<Symbol> ownVariables = new ArrayList<>();
        for (Symbol symbol : symbols.getSymbolsByDeclarationNodeId().values()) {
            if (symbol.kind == Symbol.Kind.VARIABLE && symbol.scopeId == body.id) {
                ownVariables.add(symbol);
            }
        }

        return new FunctionInfo(function, functionType, parameters, body, returnTerm, ownVariables);
    }

    private TreeNode childAt(TreeNode node, int index) {
        if (index >= node.children.size()) {
            throw new CodeGenException(node.id, "Malformed " + node.contents + " node");
        }
        TreeNode child = nodes.get(node.children.get(index));
        if (child == null) {
            throw new CodeGenException(node.id, "Missing tree node " + node.children.get(index));
        }
        return child;
    }

    public static final class FunctionInfo {
        public final Symbol function;
        public final TreeNode functionType;
        public final List<Symbol> parameters;
        public final TreeNode body;
        public final TreeNode returnTerm;
        public final List<Symbol> ownVariables;

        public FunctionInfo(
                Symbol function,
                TreeNode functionType,
                List<Symbol> parameters,
                TreeNode body,
                TreeNode returnTerm,
                List<Symbol> ownVariables) {
            this.function = function;
            this.functionType = functionType;
            this.parameters = parameters;
            this.body = body;
            this.returnTerm = returnTerm;
            this.ownVariables = ownVariables;
        }
    }
}
