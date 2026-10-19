package spl.types;

import spl.grammar.Grammar;
import spl.model.TreeNode;
import spl.semantic.SymbolTable;
import spl.semantic.Symbol;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Phase 2b: bottom-up type analysis
 * of rule table. Throws TypeException on the first violation.
 */
public final class TypeAnalyser {
    public static final String FLOAT_INT_CONFLICT = "A float-integer-conflict might perhaps be possible";

    private final Map<Integer, TreeNode> nodes;
    private final SymbolTable symbols;
    private final Set<String> nonTerminals;

    private final Map<Integer, Type> nodeTypes = new HashMap<>();

    public TypeAnalyser(Map<Integer,TreeNode> nodesById, SymbolTable symbols, Grammar grammar) {
        this.nodes = nodesById;
        this.symbols = symbols;
        this.nonTerminals = grammar.getNonTerminals();
    }

    //Run the analyis, return normal if tree is okay
    public void analyse(TreeNode root){
        preCheck();
        visit(root);
    }

    public Type getNodeType(int nodeId) {
        return nodeTypes.getOrDefault(nodeId, Type.UNKNOWN);
    }

    public Type getSymbolType(Symbol symbol) {
        return Type.fromString(symbol.getType());
    }

    //crud mod/div

    private void preCheck() {
        boolean hasMod = false, hasDiv = false;
        for (TreeNode n : nodes.values()) {
            if(isLeaf(n)) {
                if (n.contents.equals("mod")) hasMod = true;
                if (n.contents.equals("div")) hasDiv = true;
            }
        }
        if(hasMod && hasDiv) {
            throw new TypeException(FLOAT_INT_CONFLICT);
        }
        if(hasMod) {
            for(TreeNode n : nodes.values()) {
                if(isLeaf(n) && isNumLeaf(n) && n.contents.contains(".")) {
                    throw new TypeException(n.id, "Decimal number '"+n.contents
                    +"' is not allowed because the program uses 'mod' "
                    +"(all numbers must then be integers)");
                }
            }
        }
    }
    //tree caller

    private Type visit(TreeNode n) {
        switch(n.contents) {
            case "SPL_PROG": {
                need(visit(kid(n,0)), Type.OK,n, "program is not well-typed");
                return set(n,Type.OK);
            }
            case "P": {
                need(visit(kid(n,0)), Type.OK,n, "varisble declarations are not well-typed");
                need(visit(kid(n,2)), Type.OK,n, "function declarations are not well-typed");
                need(visit(kid(n,4)), Type.OK,n, "algorithm is not well-typed");
                return set(n,Type.OK);
            }
            case "V_DECLS": {
                if(n.children.isEmpty()) return set(n,Type.OK);
                need(visit(kid(n,1)),Type.OK,n, "variable declarations are not well-typed");
                setNameType(kid(n,0), Type.NUMERIC);
                return set(n,Type.OK);
            }
            case "F_DECL": {
                if(n.children.isEmpty()) return set(n,Type.OK);
                need(visit(kid(n,0)), Type.OK,n, "function is not well-typed");
                need(visit(kid(n,2)), Type.OK,n, "function declarations are not well-typed");
                return set(n,Type.OK);
            }
            case "F_TYPE": 
                return visitFType(n);
            case "ALGO": {
                if(n.children.isEmpty()) return set(n,Type.OK);
                need(visit(kid(n,0)), Type.OK,n, "instruction is not well-typed");
                need(visit(kid(n,2)), Type.OK,n, "algorithm is not well-typed");
                return set(n,Type.OK);
            }
            case "OUTP":{
                if(n.children.size() == 3) {
                    need(visit(kid(n,1)), Type.OK,n, "print (....) needs a numeric term");
                }
                return set(n,Type.OK);
            }
            case "INSTR":{
                return visitInstr(n);
            }
            case "CALL": {
                need(visit(kid(n,0)), Type.OK,n, "function call has badly typed arguments");
                TreeNode name= kid(n,0);
                Type t = funcType(name);
                if(t == Type.UNKNOWN) {
                    throw new TypeException(name.id, "Call to '"+name.contents+"' whose type is unknown "+"(is it a declared function?)");
                }
                return set(n,t);
            }
            case "INPUT": {
                if(n.children.isEmpty()) return set(n,Type.OK);
                need(visit(kid(n,0)), Type.NUMERIC,n, "function arguments must be numeric");
                need(visit(kid(n,1)), Type.OK,n, "function arguments are not well-typed");
                return set(n,Type.OK);
            }
            case "ASSIGN": {
                need(visit(kid(n,2)), Type.NUMERIC,n, "assignment value must be numeric");
                TreeNode name = kid(n,0);
                need(varType(name), Type.NUMERIC,name, "'"+name.contents+"' is not a numeric variable");
                return set(n,Type.OK);
            }
            case "TERM": return visitTerm(n);
            case "BRANCH": {
                need(visit(kid(n,1)), Type.BOOLEAN,n, "if-condition must be boolean");
                need(visit(kid(n,4)), Type.OK,n, "then branch is not well-typed");
                need(visit(kid(n,8)), Type.OK,n, "else branch is not well-typed");
                return set(n,Type.OK);
            }
            case "BOOL": 
                return visitBool(n);
            case "COND":
                return set(n,Type.OK);
            case "LOOP": {
                TreeNode first = kid(n,0);
                boolean condFirst = first.contents.equals("COND");
                TreeNode cond = condFirst ? kid(n,0) : kid(n,4);
                TreeNode bool = condFirst ? kid(n,1) : kid(n,5);
                TreeNode body = condFirst ? kid(n,4) : kid(n,2);
                need(visit(cond), Type.OK,n, "loop condition keyword is not well-typed");
                need(visit(body), Type.OK,n, "loop body is not well-typed");
                need(visit(bool), Type.BOOLEAN,n, "loop condition must be boolean");
                return set(n,Type.OK);
            }
            default:
                return Type.UNKNOWN;
        }
    }

    //type checking based on the grammar rules
    private Type visitFType(TreeNode n) {
        boolean isNum = kid(n,0).contents.equals("num");
        TreeNode name = kid(n,1);
        need(visit(kid(n,3)), Type.OK,n, "function parameters are not well-typed");
        need(visit(kid(n,6)), Type.OK,n, "function return type is not well-typed");
        if(isNum) {
            need(visit(kid(n,9)), Type.NUMERIC,n, "num function '"+name.contents+"' must return a numeric term");
            setNameType(name, Type.NUMERIC);
        } 
        else 
        {
            setNameType(name, Type.PROCEDURE);
        }
        return set(n,Type.OK);
    }

    private Type visitInstr(TreeNode n) {
        TreeNode first = kid(n,0);
        switch(first.contents) {
            case "print" -> {
                need(visit(kid(n,1)), Type.OK,n, "print argument is not well-typed");
                break;
            }
            case "nop" -> {
            }
            case "comment" -> {
                break;
            }
            default -> {
                Type t = visit(first);
                if(first.contents.equals("CALL")) {
                    need(t,Type.PROCEDURE,n, "a function call used as an instruction must be a procedure "+"(void function), but it has type "+ t);
                }
                else {
                    need(t,Type.OK,n, "instruction is not well-typed");
                }
            }
        }
        return set(n,Type.OK);
    }

    private Type visitTerm(TreeNode n) {
        TreeNode first = kid(n,0);
        String c = first.contents;
        if(nonTerminals.contains(c)) {
            need(visit(first), Type.NUMERIC, n, "a call used inside a term must return a number");
            return set(n,Type.NUMERIC);
        }
        switch(c)  {
            case "mod": case "div": case "add": case "sub": case "mul":
                need(visit(kid(n,2)), Type.NUMERIC,n, "first operand of '"+c+"' must be numereic");
                need(visit(kid(n,3)), Type.NUMERIC,n, "second operand of '"+c+"' must be numereic");
                return set(n,Type.NUMERIC);
            case "neg":
                need(visit(kid(n,2)), Type.NUMERIC,n, "operand of 'neg' must be numereic");
                return set(n,Type.NUMERIC);
            default:
                if(c.startsWith("#")) {
                    need(varType(first), Type.NUMERIC,first, "'"+c+"' is not a numeric variable");
                }
                return set(n,Type.NUMERIC);
        }
    }

    private Type visitBool(TreeNode n) {
        String c = kid(n,0).contents;
        switch(c) {
            case "not":
                need(visit(kid(n,2)), Type.BOOLEAN,n, "operand of 'not' must be boolean");
                break;
            case "and": case "or":
                need(visit(kid(n,2)), Type.BOOLEAN,n, "first operand of '"+c+"' must be boolean");
                need(visit(kid(n,3)), Type.BOOLEAN,n, "second operand of '"+c+"' must be boolean");
                break;
            default:
                need(visit(kid(n,2)), Type.NUMERIC,n, "first operand of '"+c+"' must be numeric");
                need(visit(kid(n,3)), Type.NUMERIC,n, "second operand of '"+c+"' must be numeric");
        }
        return set(n,Type.BOOLEAN);
    }

    //HELPERS
    private TreeNode kid(TreeNode n,int i) {
        return nodes.get(n.children.get(i));
    }

    private boolean isLeaf(TreeNode n) {
        return !nonTerminals.contains(n.contents);
    }

    private boolean isNumLeaf(TreeNode n) {
        return n.contents.matches("[-\u2013]?[0-9].*");
    }

    private Type set(TreeNode n, Type t) {
        nodeTypes.put(n.id, t);
        return t;
    }

    private void need(Type actual,Type wanted,TreeNode where,String msg) {
        if(actual != wanted) {
            throw new TypeException(where.id, "Type error: "+msg+" (expected "+wanted+", got "+actual+")");
        }
    }

    private Type nameType(TreeNode nameLeaf) {
        return Type.fromString(symbolOf(nameLeaf).getType());
    }

    private void setNameType(TreeNode nameLeaf, Type t) {
        symbolOf(nameLeaf).setType(t.toString());
    }

    private Type varType(TreeNode leaf) {
        Symbol s = symbolOf(leaf);
        if(s.kind != Symbol.Kind.VARIABLE) {
            throw new TypeException(leaf.id, "'"+leaf.contents+"' is a function but is used as a variable");
        }
        return Type.fromString(s.getType());
    }

    private Type funcType(TreeNode leaf) {
        Symbol s = symbolOf(leaf);
        if(s.kind != Symbol.Kind.FUNCTION) {
            throw new TypeException(leaf.id, "'"+leaf.contents+"' is a variable but is called like a function");
        }
        return Type.fromString(s.getType());
    }

    private Symbol symbolOf(TreeNode leaf) {
        Symbol s = symbols.getBinding(leaf.id);
        if(s == null)  s = symbols.getSymbol(leaf.id);
        if(s == null) {
            throw new TypeException(leaf.id, "'"+leaf.contents+"' is not declared");
        }
        return s;
    }
}
