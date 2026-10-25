package spl.codegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Assumptions;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompileAndRunTest {
    private static final String[] STANDARDS = {"c++11", "gnu++17"};

    @Test
    void printsString(@TempDir Path tempDir) throws Exception {
        assertProgramOutput(": : print \"hello\" ;", "hello", tempDir);
    }

    @Test
    void assignsAndPrintsInteger(@TempDir Path tempDir) throws Exception {
        assertProgramOutput("#x : : #x = 5 ; print ( #x ) ;", "5", tempDir);
    }

    @Test
    void evaluatesNestedArithmetic(@TempDir Path tempDir) throws Exception {
        assertProgramOutput(
                "#a : : #a = add ( 1 mul ( 2 3 ) ) ; print ( #a ) ;",
                "7",
                tempDir);
    }

    @Test
    void runsWhileLoop(@TempDir Path tempDir) throws Exception {
        assertProgramOutput(
                "#i : : #i = 0 ; while lesser ( #i 3 ) do { "
                        + "print ( #i ) ; #i = add ( #i 1 ) ; } ;",
                "012",
                tempDir);
    }

    @Test
    void runsDoUntilLoop(@TempDir Path tempDir) throws Exception {
        assertProgramOutput(
                "#i : : #i = 0 ; do { print ( #i ) ; #i = add ( #i 1 ) ; } "
                        + "until eq ( #i 3 ) ;",
                "012",
                tempDir);
    }

    @Test
    void evaluatesNotCondition(@TempDir Path tempDir) throws Exception {
        assertProgramOutput(
                ": : if not ( eq ( 1 2 ) ) then { print \"yes\" ; } "
                        + "else { print \"no\" ; } ;",
                "yes",
                tempDir);
    }

    @Test
    void evaluatesTrueAndCondition(@TempDir Path tempDir) throws Exception {
        assertProgramOutput(
                ": : if and ( larger ( 2 1 ) lesser ( 1 2 ) ) then { print \"yes\" ; } "
                        + "else { print \"no\" ; } ;",
                "yes",
                tempDir);
    }

    @Test
    void evaluatesFalseAndCondition(@TempDir Path tempDir) throws Exception {
        assertProgramOutput(
                ": : if and ( larger ( 2 1 ) lesser ( 2 1 ) ) then { print \"yes\" ; } "
                        + "else { print \"no\" ; } ;",
                "no",
                tempDir);
    }

    @Test
    void evaluatesOrCondition(@TempDir Path tempDir) throws Exception {
        assertProgramOutput(
                ": : if or ( eq ( 1 2 ) eq ( 3 3 ) ) then { print \"yes\" ; } "
                        + "else { print \"no\" ; } ;",
                "yes",
                tempDir);
    }

    @Test
    void callsNumericFunctionTwice(@TempDir Path tempDir) throws Exception {
        assertProgramOutput(
                "#r : num #inc ( #n ) { : : return ( add ( #n 1 ) ) } : "
                        + "#r = #inc ( 1 ) ; print ( #r ) ; "
                        + "#r = #inc ( #r ) ; print ( #r ) ;",
                "23",
                tempDir);
    }

    @Test
    void callsFunctionWithLoopTwice(@TempDir Path tempDir) throws Exception {
        assertProgramOutput(
                "#a #b : num #sum ( #n ) { #s #i : : #s = 0 ; #i = 1 ; "
                        + "while lesser ( #i add ( #n 1 ) ) do { "
                        + "#s = add ( #s #i ) ; #i = add ( #i 1 ) ; } ; "
                        + "return ( #s ) } : #a = #sum ( 3 ) ; #b = #sum ( 4 ) ; "
                        + "print ( #a ) ; print ( #b ) ;",
                "610",
                tempDir);
    }

    @Test
    void voidFunctionChangesOuterVariable(@TempDir Path tempDir) throws Exception {
        assertProgramOutput(
                "#g : void #bump ( ) { : : #g = add ( #g 1 ) ; return } : "
                        + "#g = 0 ; #bump ( ) ; #bump ( ) ; print ( #g ) ;",
                "2",
                tempDir);
    }

    @Test
    void normalizesEnDashNegativeNumber(@TempDir Path tempDir) throws Exception {
        assertProgramOutput("#x : : #x = \u20135 ; print ( #x ) ;", "-5", tempDir);
    }

    @Test
    void performsFloatingPointDivision(@TempDir Path tempDir) throws Exception {
        assertProgramOutput("#x : : #x = div ( 1 4 ) ; print ( #x ) ;", "0.25", tempDir);
    }

    @Test
    void printsQuestionMarkInString(@TempDir Path tempDir) throws Exception {
        assertProgramOutput(": : print \"what?\" ;", "what?", tempDir);
    }

    @Test
    void inlinesNestedSubfunction(@TempDir Path tempDir) throws Exception {
        assertProgramOutput(
                "#r : num #f ( #a ) { : num #g ( #b ) { : : "
                        + "return ( add ( #b 1 ) ) } : #a = #g ( #a ) ; "
                        + "return ( #a ) } : #r = #f ( 1 ) ; print ( #r ) ;",
                "2",
                tempDir);
    }

    private void assertProgramOutput(String source, String expected, Path tempDir) throws Exception {
        assumeGppAvailable();
        for (String standard : STANDARDS) {
            assertEquals(expected, compileAndRun(source, standard, tempDir));
        }
    }

    private void assumeGppAvailable() throws IOException, InterruptedException {
        Process process;
        try {
            process = new ProcessBuilder("g++", "--version")
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } catch (IOException exception) {
            Assumptions.assumeTrue(false, "g++ is not available on PATH");
            return;
        }

        boolean completed = process.waitFor(Duration.ofSeconds(60).toMillis(), TimeUnit.MILLISECONDS);
        if (!completed) {
            process.destroyForcibly();
            Assumptions.assumeTrue(false, "g++ --version did not complete within 60 seconds");
        }
        Assumptions.assumeTrue(process.exitValue() == 0, "g++ is not available on PATH");
    }

    private String compileAndRun(String source, String standard, Path tempDir) throws Exception {
        String generated = generate(source);
        Path buildDir = Files.createDirectories(tempDir.resolve(standard));
        Path sourceFile = buildDir.resolve("output.cpp");
        Path compilerLog = buildDir.resolve("compiler.log");
        Path programOutput = buildDir.resolve("program.out");
        Path programError = buildDir.resolve("program.err");
        Files.writeString(sourceFile, generated, StandardCharsets.UTF_8);

        Process compiler = new ProcessBuilder(
                "g++", "-std=" + standard, "-Wall", "-Wextra", "-o", "prog", "output.cpp")
                .directory(buildDir.toFile())
                .redirectErrorStream(true)
                .redirectOutput(compilerLog.toFile())
                .start();
        boolean compilerCompleted = compiler.waitFor(60, TimeUnit.SECONDS);
        assertTrue(compilerCompleted, "Compiler timed out for " + standard + "\n" + generated);
        String compilerOutput = Files.readString(compilerLog, StandardCharsets.UTF_8);
        assertEquals(0, compiler.exitValue(),
                "Compiler output:\n" + compilerOutput + "\nGenerated source:\n" + generated);

        String executable = isWindows() ? "prog.exe" : "prog";
        Process program = new ProcessBuilder(buildDir.resolve(executable).toString())
                .directory(buildDir.toFile())
                .redirectOutput(programOutput.toFile())
                .redirectError(programError.toFile())
                .start();
        boolean programCompleted = program.waitFor(60, TimeUnit.SECONDS);
        assertTrue(programCompleted, "Program timed out for " + standard + "\n" + generated);
        String programStdout = Files.readString(programOutput, StandardCharsets.UTF_8);
        String programStderr = Files.readString(programError, StandardCharsets.UTF_8);
        assertEquals(0, program.exitValue(),
                "Program stderr:\n" + programStderr + "\nGenerated source:\n" + generated);
        return programStdout;
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

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
    }
}
