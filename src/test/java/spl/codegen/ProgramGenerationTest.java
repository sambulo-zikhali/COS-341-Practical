package spl.codegen;

import org.junit.jupiter.api.Test;
import spl.grammar.Grammar;
import spl.grammar.ParsingTableBuilder;
import spl.lexer.Lexer;
import spl.model.TreeNode;
import spl.parser.NodeIdGenerator;
import spl.parser.Parser;
import spl.semantic.SemanticAnalyzer;
import spl.semantic.SymbolTable;
import spl.types.NumericKind;
import spl.types.TypeAnalyser;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgramGenerationTest {
    @Test
    void printsStringTokenWithQuotes() {
        String output = generate(": : print \"hi\" ;");

        assertTrue(output.contains("std::cout << \"hi\" ;"), output);
    }

    @Test
    void declaresAndPrintsTermVariables() {
        String output = generate("#x : : #x = 5 ; print ( #x ) ;");

        assertTrue(output.contains("int v0 ;"), output);
        assertTrue(output.contains("v0 = t"), output);
        assertTrue(output.contains("std::cout << t"), output);
    }

    @Test
    void emitsExecutableProgramSkeletonInOrder() {
        String output = generate(": :");

        int mineStart = output.indexOf("/* begin of the non-executable code-mine");
        int include = output.indexOf("#include <iostream>");
        int main = output.indexOf("int main()");
        int returnStatement = output.indexOf("return 0 ;");
        int endMarker = output.indexOf("/* end of executable C */");

        assertTrue(mineStart == 0, output);
        assertTrue(mineStart < include, output);
        assertTrue(include < main, output);
        assertTrue(main < returnStatement, output);
        assertTrue(returnStatement < endMarker, output);
    }

    @Test
    void translatesNopAndCommentInExecutableCode() {
        String output = generate(": : nop ; comment \"hello\" ;");

        assertTrue(output.contains("/*nop */"), output);
        assertTrue(output.contains("/*hello */"), output);
    }

    @Test
    void translatesNopAndCommentInsideFunctionMine() {
        String output = generate(": void #f ( ) { : : nop ; comment \"inside\" ; return } :");

        assertTrue(output.contains("[nop]"), output);
        assertTrue(output.contains("[inside]"), output);
        assertMineHasNoCommentTerminator(output);
    }

    @Test
    void emitsMatchingJumpLabelsForBranchAndLoop() {
        String output = generate(
                "#x : : "
                        + "if eq ( #x 0 ) then { nop ; } else { comment \"else\" ; } ; "
                        + "while lesser ( #x 5 ) do { nop ; } ;");

        assertEveryGotoHasLabel(output);
    }

    @Test
    void mineContainsNoPrematureCommentTerminator() {
        String output = generate(": void #f ( ) { : : nop ; comment \"inside\" ; return } :");

        assertMineHasNoCommentTerminator(output);
    }

    private String generate(String source) {
        Grammar grammar = new Grammar();
        Parser parser = new Parser(
                new ParsingTableBuilder(grammar).build(),
                new NodeIdGenerator());
        TreeNode root = parser.parse(new Lexer().tokenize(source));
        Map<Integer, TreeNode> nodes = parser.getNodesById();
        SymbolTable symbols = new SemanticAnalyzer().analyze(root, nodes);
        TypeAnalyser types = new TypeAnalyser(nodes, symbols, grammar);
        types.analyse(root);

        NumericKind kind = types.getNumericKind();
        return CodeGenerator.generate(root, nodes, symbols, kind);
    }

    private void assertEveryGotoHasLabel(String output) {
        Matcher gotos = Pattern.compile("goto (L[0-9]+) ;").matcher(output);
        while (gotos.find()) {
            assertTrue(output.contains(gotos.group(1) + " :"), output);
        }
    }

    private void assertMineHasNoCommentTerminator(String output) {
        String beginMarker = "/* begin of the non-executable code-mine";
        String endMarker = "end of the non-executable code-mine */";
        int mineStart = output.indexOf(beginMarker);
        int mineEnd = output.indexOf(endMarker);

        assertTrue(mineStart >= 0, output);
        assertTrue(mineEnd > mineStart, output);

        int contentsStart = output.indexOf("\r\n", mineStart) + 2;
        String mineContents = output.substring(contentsStart, mineEnd);
        assertFalse(mineContents.contains("*/"), output);
    }
}
