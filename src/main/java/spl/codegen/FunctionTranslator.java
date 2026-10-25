package spl.codegen;

import spl.model.TreeNode;
import spl.semantic.Symbol;
import spl.semantic.SymbolTable;
import spl.types.NumericKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FunctionTranslator {
    private final Emitter out;
    private final NameGen names;
    private final Map<Integer, TreeNode> nodes;
    private final SymbolTable symbols;
    private final String cType;
    private final TermTranslator terms;
    private final BodyTranslator body;

    public FunctionTranslator(
            Emitter out,
            NameGen names,
            Map<Integer, TreeNode> nodes,
            SymbolTable symbols,
            NumericKind kind,
            TermTranslator terms,
            BodyTranslator body) {
        this.out = Objects.requireNonNull(out);
        this.names = Objects.requireNonNull(names);
        this.nodes = Objects.requireNonNull(nodes);
        this.symbols = Objects.requireNonNull(symbols);
        this.cType = Objects.requireNonNull(kind).cType;
        this.terms = Objects.requireNonNull(terms);
        this.body = Objects.requireNonNull(body);
    }

    public void transFDecl(TreeNode fDecl) {
        TreeNode current = fDecl;
        while (!current.children.isEmpty()) {
            TreeNode functionType = childAt(current, 0);
            TreeNode nextDeclaration = childAt(current, 1);
            transFunction(functionType);
            out.line("");
            current = nextDeclaration;
        }
    }

    private void transFunction(TreeNode functionType) {
        TreeNode typeLeaf = childAt(functionType, 0);
        TreeNode nameLeaf = childAt(functionType, 1);
        Symbol function = declarationSymbol(nameLeaf);
        List<String> parameters = parameterNames(childAt(functionType, 3));
        TreeNode program = childAt(functionType, 6);
        RenameEnv env = new RenameEnv();

        if ("void".equals(typeLeaf.contents)) {
            out.line("begin of code-mine for void-procedure " + function.internalName);
            out.line("Parameters for inlining:");
            emitParameters(parameters);
            out.line("Body of the procedure:");
            body.transBody(program, env, false);
            out.line("end of code-mine for void-procedure " + function.internalName);
            return;
        }

        if ("num".equals(typeLeaf.contents)) {
            out.line("begin of code-mine for Function " + function.internalName);
            out.line("Parameters for inlining:");
            emitParameters(parameters);
            out.line("Body of the procedure:");
            body.transBody(program, env, false);
            out.line(";");

            String returnName = function.internalName + names.ret();
            out.line(cType + " " + returnName + " ;");
            terms.transTerm(childAt(functionType, 9), returnName, env);
            out.line("end of code-mine for Function " + function.internalName);
            out.line("The result is found in the return value " + returnName);
            return;
        }

        throw new CodeGenException(typeLeaf.id, "Unknown function type: " + typeLeaf.contents);
    }

    private void emitParameters(List<String> parameters) {
        for (String parameter : parameters) {
            out.line(cType + " " + parameter + " ;");
        }
    }

    private List<String> parameterNames(TreeNode declarations) {
        List<String> parameters = new ArrayList<>();
        TreeNode current = declarations;
        while (!current.children.isEmpty()) {
            TreeNode parameterLeaf = childAt(current, 0);
            parameters.add(declarationSymbol(parameterLeaf).internalName);
            current = childAt(current, 1);
        }
        return parameters;
    }

    private Symbol declarationSymbol(TreeNode leaf) {
        Symbol symbol = symbols.getSymbol(leaf.id);
        if (symbol == null) {
            throw new CodeGenException(leaf.id, "No symbol found for declaration '" + leaf.contents + "'");
        }
        return symbol;
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
}
