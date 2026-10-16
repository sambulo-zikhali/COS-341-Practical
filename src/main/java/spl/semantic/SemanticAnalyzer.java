package spl.semantic;

import spl.model.TreeNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SemanticAnalyzer {
    public SymbolTable analyze(TreeNode root, Map<Integer, TreeNode> nodes) {
        if (root == null || !"SPL_PROG".equals(root.contents)) {
            throw new SemanticException("Expected SPL_PROG root", root == null ? null : root.id);
        }
        if (nodes == null) {
            throw new IllegalArgumentException("nodes must not be null");
        }

        List<TreeNode> rootChildren = childrenOf(root, nodes);
        if (rootChildren.isEmpty() || !"P".equals(rootChildren.get(0).contents)) {
            throw new SemanticException("SPL_PROG must have P as its first child", root.id);
        }

        SymbolTable symbolTable = new SymbolTable();
        TreeNode topProgram = rootChildren.get(0);
        Scope topScope = new Scope(topProgram.id, 0, null);
        processProgram(topProgram, topScope, symbolTable, nodes);
        return symbolTable;
    }

    private void processProgram(
            TreeNode program,
            Scope scope,
            SymbolTable symbolTable,
            Map<Integer, TreeNode> nodes) {
        List<TreeNode> children = childrenOf(program, nodes);
        if (children.size() < 5
                || !"V_DECL".equals(children.get(0).contents)
                || !"F_DECL".equals(children.get(2).contents)
                || !"ALGO".equals(children.get(4).contents)) {
            throw new SemanticException("Malformed P node", program.id);
        }

        symbolTable.addScope(scope);
        declareVariables(children.get(0), scope, symbolTable, nodes, false);

        List<TreeNode> functionTypes = flattenList(children.get(2), "F_DECL", "F_TYPE", nodes);
        List<TreeNode> names = new ArrayList<>(functionTypes.size());
        for (TreeNode functionType : functionTypes) {
            TreeNode name = childAt(functionType, 1, nodes);
            names.add(name);
            declareFunction(name, scope, symbolTable);
        }

        for (int i = 0; i < functionTypes.size(); i++) {
            processFunction(functionTypes.get(i), names.get(i), scope, symbolTable, nodes);
        }
    }

    private void processFunction(
            TreeNode functionType,
            TreeNode functionName,
            Scope parentScope,
            SymbolTable symbolTable,
            Map<Integer, TreeNode> nodes) {
        List<TreeNode> children = childrenOf(functionType, nodes);
        if (children.size() < 8 || !"V_DECL".equals(children.get(3).contents)
                || !"P".equals(children.get(6).contents)) {
            throw new SemanticException("Malformed F_TYPE node", functionType.id);
        }

        TreeNode body = children.get(6);
        Scope functionScope = new Scope(body.id, parentScope.level + 1, parentScope);
        declareVariables(children.get(3), functionScope, symbolTable, nodes, true);
        processProgram(body, functionScope, symbolTable, nodes);
    }

    private void declareVariables(
            TreeNode declarations,
            Scope scope,
            SymbolTable symbolTable,
            Map<Integer, TreeNode> nodes,
            boolean isParameter) {
        for (TreeNode name : flattenList(declarations, "V_DECL", null, nodes)) {
            String sourceName = name.contents;
            Symbol existing = scope.getVariable(sourceName);
            if (existing != null) {
                String message = existing.isParameter
                        ? "local variable masks parameter " + sourceName
                        : "duplicate variable declaration " + sourceName;
                throw new SemanticException(message, name.id);
            }

            Symbol symbol = new Symbol(
                    sourceName,
                    symbolTable.newInternalName(),
                    Symbol.Kind.VARIABLE,
                    name.id,
                    scope.id,
                    isParameter);
            if (!scope.addVariable(symbol)) {
                throw new IllegalStateException("Variable duplicate check disagreed with scope insertion");
            }
            symbolTable.addSymbol(symbol);
        }
    }

    private void declareFunction(TreeNode name, Scope scope, SymbolTable symbolTable) {
        if (scope.getFunction(name.contents) != null) {
            throw new SemanticException("duplicate function " + name.contents, name.id);
        }

        Symbol symbol = new Symbol(
                name.contents,
                symbolTable.newInternalName(),
                Symbol.Kind.FUNCTION,
                name.id,
                scope.id,
                false);
        if (!scope.addFunction(symbol)) {
            throw new IllegalStateException("Function duplicate check disagreed with scope insertion");
        }
        symbolTable.addSymbol(symbol);
    }

    private List<TreeNode> flattenList(
            TreeNode listNode,
            String listKind,
            String elementKind,
            Map<Integer, TreeNode> nodes) {
        List<TreeNode> elements = new ArrayList<>();
        TreeNode current = listNode;
        while (true) {
            if (!listKind.equals(current.contents)) {
                throw new SemanticException("Malformed " + listKind + " list", current.id);
            }
            List<TreeNode> children = childrenOf(current, nodes);
            if (children.isEmpty()) {
                return elements;
            }
            if (children.size() != 2) {
                throw new SemanticException("Malformed " + listKind + " list", current.id);
            }
            TreeNode element = children.get(0);
            if (elementKind != null && !elementKind.equals(element.contents)) {
                throw new SemanticException("Malformed " + listKind + " element", element.id);
            }
            elements.add(element);
            current = children.get(1);
        }
    }

    private TreeNode childAt(TreeNode node, int index, Map<Integer, TreeNode> nodes) {
        List<TreeNode> children = childrenOf(node, nodes);
        if (index >= children.size()) {
            throw new SemanticException("Malformed " + node.contents + " node", node.id);
        }
        return children.get(index);
    }

    private List<TreeNode> childrenOf(TreeNode node, Map<Integer, TreeNode> nodes) {
        List<TreeNode> children = new ArrayList<>(node.children.size());
        for (int childId : node.children) {
            TreeNode child = nodes.get(childId);
            if (child == null) {
                throw new SemanticException("Missing tree node " + childId, node.id);
            }
            children.add(child);
        }
        return children;
    }
}
