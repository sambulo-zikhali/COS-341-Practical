package spl.codegen;

import java.util.Map;

import spl.model.TreeNode;
import spl.semantic.SymbolTable;
import spl.types.TypeAnalyser;

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

    public String getGeneratedCode() {
    }

    public void transAlgo(TreeNode node) {
    }

    private void transInstr(TreeNode node) {
    }

    private void transPrint(TreeNode node) {

    }

    private void transAssign(TreeNode node) {
    }

    private void transTerm(TreeNode node, String resultVar) {
    }

    private void transBranch(TreeNode node) {

    }

    private void transLoop(TreeNode node) {
    }

    private void transBool(TreeNode node, String targetLabel, String dummyLabel) {

    }

    private void transNot(TreeNode node, String targetLabel, String dummyLabel) {
    }

    // Helpers

    private TreeNode getNode(int id) {
    }

    private void emit(String code) {
    }

    private String nextTempVar() {

    }

    private String nextLabel() {

    }

    private String lookupName(TreeNode node) {

    }

    private String getCType() {

    }
}
