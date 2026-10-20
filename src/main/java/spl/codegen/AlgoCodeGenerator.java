package spl.codegen;

import spl.model.TreeNode;
import spl.semantic.Symbol;
import spl.semantic.SymbolTable;
import spl.types.TypeAnalyser;

import java.util.Map;

/**
 * ALGO Code Generation.
 * Translates the syntax tree into executable C code.
 */
public class AlgoCodeGenerator {

    private final SymbolTable symbols;
    private final TypeAnalyser types;
    private final Map<Integer, TreeNode> nodesById;
    
    private final StringBuilder output = new StringBuilder();
    private int tempCounter = 0;
    private int labelCounter = 0;

    // Cache the global type so we only scan the tree once.
    private String globalCType = null;

    public AlgoCodeGenerator(SymbolTable symbols, TypeAnalyser types, Map<Integer, TreeNode> nodesById) {
        this.symbols = symbols;
        this.types = types;
        this.nodesById = nodesById;
    }

    public String getGeneratedCode() {
        return output.toString();
    }

    /**
     * Translates an ALGO node into executable C code.
     */
    public void transAlgo(TreeNode node) {
        if (node.children.isEmpty()) {
            return; 
        }
        
        // ALGO -> INSTR ; ALGO
        TreeNode instr = getNode(node.children.get(0));
        TreeNode nextAlgo = getNode(node.children.get(2)); 
        
        transInstr(instr);
        transAlgo(nextAlgo);
    }

    private void transInstr(TreeNode node) {
        TreeNode inner = getNode(node.children.get(0));
        String type = inner.contents;

        switch (type) {
            case "print":
                transPrint(inner);
                break;
            case "nop":
                emit("/* nop */");
                break;
            case "comment":
                TreeNode strNode = getNode(inner.children.get(1));
                emit("/* " + strNode.contents + " */");
                break;
            case "ASSIGN":
                transAssign(inner);
                break;
            case "BRANCH":
                transBranch(inner);
                break;
            case "LOOP":
                transLoop(inner);
                break;
            case "CALL":
                // TODO: Phase 3b (Inlining)
                emit("/* TODO: CALL (Phase 3b Inlining) */");
                break;
            default:
                throw new CodeGenException(node.id, "Unknown INSTR type: " + type);
        }
    }

    private void transPrint(TreeNode node) {
        TreeNode outp = getNode(node.children.get(1));
        
        if (outp.contents.equals("STRING")) {
            TreeNode strNode = getNode(outp.children.get(0));
            // Note: SPL strings include quotes, so we just print them directly.
            emit("std::cout << " + strNode.contents + ";");
        } else {
            TreeNode term = getNode(outp.children.get(1));
            String vN = nextTempVar();
            emit(getCType() + " " + vN + ";");
            transTerm(term, vN);
            emit("std::cout << " + vN + ";");
        }
    }

    private void transAssign(TreeNode node) {
        TreeNode nameNode = getNode(node.children.get(0));
        TreeNode termNode = getNode(node.children.get(2));
        
        String vN = nextTempVar();
        String intern = lookupName(nameNode);
        
        emit(getCType() + " " + vN + ";");
        transTerm(termNode, vN);
        emit(intern + " = " + vN + ";");
    }

    private void transTerm(TreeNode node, String resultVar) {
        String type = node.contents;

        switch (type) {
            case "NAME":
                TreeNode nameNode = getNode(node.children.get(0));
                emit(resultVar + " = " + lookupName(nameNode) + ";");
                break;
                
            case "NUM":
                TreeNode numNode = getNode(node.children.get(0));
                emit(resultVar + " = " + numNode.contents + ";");
                break;
                
            case "add":
            case "sub":
            case "mul":
            case "div":
            case "mod":
                String op = type.equals("mod") ? "%" : 
                            type.equals("div") ? "/" : 
                            type.equals("add") ? "+" : 
                            type.equals("sub") ? "-" : "*";
                            
                TreeNode leftTerm = getNode(node.children.get(2));
                TreeNode rightTerm = getNode(node.children.get(3));
                String t1 = nextTempVar();
                String t2 = nextTempVar();
                
                emit(getCType() + " " + t1 + ";");
                emit(getCType() + " " + t2 + ";");
                
                transTerm(leftTerm, t1);
                transTerm(rightTerm, t2);
                emit(resultVar + " = " + t1 + " " + op + " " + t2 + ";");
                break;
                
            case "neg":
                TreeNode innerTerm = getNode(node.children.get(2));
                String t3 = nextTempVar();
                emit(getCType() + " " + t3 + ";");
                transTerm(innerTerm, t3);
                emit(resultVar + " = -" + t3 + ";");
                break;
                
            case "CALL":
                emit("/* TODO: TERM CALL (Phase 3b Inlining) */");
                break;
                
            default:
                throw new CodeGenException(node.id, "Unknown TERM type: " + type);
        }
    }

    private void transBranch(TreeNode node) {
        TreeNode boolNode = getNode(node.children.get(1));
        TreeNode trueAlgo = getNode(node.children.get(4));
        TreeNode falseAlgo = getNode(node.children.get(8));

        String la = nextLabel();
        String lb = nextLabel();
        String lc = nextLabel();

        emit("/* if */");
        transBool(boolNode, la, lb);
        emit(lb + ": /* else */");
        transAlgo(falseAlgo);
        emit("goto " + lc + ";");
        emit(la + ": /* then */");
        transAlgo(trueAlgo);
        emit(lc + ": /* continue */");
    }

    private void transLoop(TreeNode node) {
        String startLabel = nextLabel();
        String endLabel = nextLabel();
        String dummyLabel = nextLabel();

        if (getNode(node.children.get(0)).contents.equals("COND")) {
            TreeNode condNode = getNode(node.children.get(0));
            TreeNode boolNode = getNode(node.children.get(1));
            TreeNode algoNode = getNode(node.children.get(4));

            emit(startLabel + ":");
            
            boolean isUntil = getNode(condNode.children.get(0)).contents.equals("until");
            
            if (isUntil) {
                transBool(boolNode, endLabel, dummyLabel);
            } else {
                transNot(boolNode, endLabel, dummyLabel);
            }

            transAlgo(algoNode);
            emit("goto " + startLabel + ";");
            emit(endLabel + ":");
            
        } else {
            TreeNode algoNode = getNode(node.children.get(1));
            TreeNode condNode = getNode(node.children.get(4));
            TreeNode boolNode = getNode(node.children.get(5));

            emit(startLabel + ":");
            transAlgo(algoNode);
            
            boolean isUntil = getNode(condNode.children.get(0)).contents.equals("until");
            if (isUntil) {
                transNot(boolNode, startLabel, dummyLabel);
            } else {
                transBool(boolNode, startLabel, dummyLabel);
            }
            emit(endLabel + ":");
        }
    }

    private void transBool(TreeNode node, String targetLabel, String dummyLabel) {
        String type = node.contents;
        
        switch (type) {
            case "not":
                transNot(node, targetLabel, dummyLabel);
                break;
            case "and":
                TreeNode b1 = getNode(node.children.get(2));
                TreeNode b2 = getNode(node.children.get(3));
                transNot(b1, dummyLabel, targetLabel);
                transNot(b2, dummyLabel, targetLabel);
                break;
            case "or":
                TreeNode b3 = getNode(node.children.get(2));
                TreeNode b4 = getNode(node.children.get(3));
                transBool(b3, targetLabel, dummyLabel);
                transBool(b4, targetLabel, dummyLabel);
                break;
            case "eq":
            case "larger":
            case "lesser":
                String op = type.equals("eq") ? "==" : type.equals("larger") ? ">" : "<";
                TreeNode t1 = getNode(node.children.get(2));
                TreeNode t2 = getNode(node.children.get(3));
                String temp1 = nextTempVar();
                String temp2 = nextTempVar();
                
                emit(getCType() + " " + temp1 + ";");
                emit(getCType() + " " + temp2 + ";");
                transTerm(t1, temp1);
                transTerm(t2, temp2);
                emit("if (" + temp1 + " " + op + " " + temp2 + ") goto " + targetLabel + ";");
                break;
            default:
                throw new CodeGenException(node.id, "Unknown BOOL type: " + type);
        }
    }

    private void transNot(TreeNode node, String targetLabel, String dummyLabel) {
        String type = node.contents;
        
        if (type.equals("not")) {
            TreeNode inner = getNode(node.children.get(2));
            transBool(inner, targetLabel, dummyLabel);
        } else if (type.equals("and")) {
            TreeNode b1 = getNode(node.children.get(2));
            TreeNode b2 = getNode(node.children.get(3));
            transNot(b1, targetLabel, dummyLabel);
            transNot(b2, targetLabel, dummyLabel);
        } else if (type.equals("or")) {
            TreeNode b1 = getNode(node.children.get(2));
            TreeNode b2 = getNode(node.children.get(3));
            transNot(b1, dummyLabel, targetLabel);
            transNot(b2, dummyLabel, targetLabel);
        } else {
            String op = type.equals("eq") ? "!=" : type.equals("larger") ? "<=" : ">=";
            TreeNode t1 = getNode(node.children.get(2));
            TreeNode t2 = getNode(node.children.get(3));
            String temp1 = nextTempVar();
            String temp2 = nextTempVar();
            
            emit(getCType() + " " + temp1 + ";");
            emit(getCType() + " " + temp2 + ";");
            transTerm(t1, temp1);
            transTerm(t2, temp2);
            emit("if (" + temp1 + " " + op + " " + temp2 + ") goto " + targetLabel + ";");
        }
    }

    //  Helpers 

    private TreeNode getNode(int id) {
        return nodesById.get(id);
    }

    private void emit(String code) {
        output.append(code).append("\n");
    }

    private String nextTempVar() {
        return "temp" + (tempCounter++);
    }

    private String nextLabel() {
        return "L" + (labelCounter++);
    }

    private String lookupName(TreeNode node) {
        Symbol s = symbols.getBinding(node.id);
        if (s == null) {
            s = symbols.getSymbol(node.id);
        }
        if (s == null) {
            throw new CodeGenException(node.id, "No symbol binding found for '" + node.contents + "'");
        }
        return s.internalName;
    }

    private String getCType() {
        if (globalCType != null) return globalCType;
        
        boolean hasMod = false, hasDiv = false;
        for (TreeNode n : nodesById.values()) {
            if (n.contents.equals("mod")) hasMod = true;
            if (n.contents.equals("div")) hasDiv = true;
        }
        
        globalCType = hasMod ? "int" : (hasDiv ? "float" : "int");
        return globalCType;
    }
}