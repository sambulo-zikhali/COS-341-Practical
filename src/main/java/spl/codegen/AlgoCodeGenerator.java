package spl.codegen;

import spl.model.TreeNode;
import spl.semantic.Symbol;
import spl.semantic.SymbolTable;
import spl.types.TypeAnalyser;

import java.util.Map;

public class AlgoCodeGenerator {
    private final SymbolTable symbols;
    private final TypeAnalyser types;
    private final Map<Integer, TreeNode> nodesById;

    private final StringBuilder output = new StringBuilder();
    private int tempCounter = 0;
    private int labelCounter = 0;

    private String globalCType = null;

    public AlgoCodeGenerator(SymbolTable symbols, TypeAnalyser types, Map<Integer, TreeNode> nodesById) {
        this.symbols = symbols;
        this.types = types;
        this.nodesById = nodesById;
    }
}
