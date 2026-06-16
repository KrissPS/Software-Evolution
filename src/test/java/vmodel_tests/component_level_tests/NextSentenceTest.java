package vmodel_tests.component_level_tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ast.ASTUtils;
import preprocessing.BabyCobolParserUtils;
import interpreter.BabyCobolInterpreter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class NextSentenceTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private PrintStream originalOut;

    @BeforeEach
    public void setUp() {
        originalOut = System.out;
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
        outContent.reset();
    }

    private String runProgram(String resourcePath) throws Exception {
        String code = BabyCobolParserUtils.readResource(resourcePath);
        String preprocessed = BabyCobolParserUtils.preprocess(code);

        ASTUtils.ASTResult astResult = ASTUtils.buildASTAndSymbolTable(preprocessed);
        assertNotNull(astResult.root, "Program should parse successfully");

        BabyCobolInterpreter interpreter = new BabyCobolInterpreter(astResult.symbolTable);
        interpreter.execute(astResult.root);

        return outContent.toString();
    }

    @Test
    void testNextSentenceParsingAndExecution() throws Exception {
        String output = runProgram("/examples/next_sentence_test.babycob");

        // validate output
        assertTrue(output.contains("Start"), "Output should contain 'Start'");
        assertTrue(output.contains("In IF"), "Output should contain 'In IF'");
        assertTrue(output.contains("After IF"), "Output should contain 'After IF'");
        assertFalse(output.contains("Should not print"),
            "Output should NOT contain 'Should not print' (NEXT SENTENCE should skip it)");
    }

    @Test
    void testNextSentenceInNestedContext() throws Exception {
        // check NEXT SENTENCE works within nested structures
        String code = BabyCobolParserUtils.readResource("/examples/next_sentence_test.babycob");
        String preprocessed = BabyCobolParserUtils.preprocess(code);

        // check parsing succeeds with proper sentence structure
        String tree = BabyCobolParserUtils.parseTree(preprocessed);
        assertNotNull(tree, "Parse tree should be generated for NEXT SENTENCE statements");
        assertTrue(tree.contains("nextSentenceStmt"),"Parse tree should contain nextSentenceStmt node");
    }

    /**
     * NEXT SENTENCE as the last statement of the paragraph does nothing
     * because it has to end with a dot (as each paragraph consists of only full sentences),
     * so it ends a sentence itself and transfers control to the sentence after that
     */
    @Test
    void testNextSentenceAsLastStatementOfParagraph() throws Exception {
        String output = runProgram("/examples/next_sentence_last_in_paragraph.babycob");

        assertTrue(output.contains("Before"), "Output should contain 'Before'");
        assertTrue(output.contains("After"), "Output should contain 'After'");
    }

    /**
     * NEXT SENTENCE at the beginning of the paragraph can be used to comment out
     * one or more of the immediately following statements making them dead code,
     * without marking each line as an explicit comment
     */
    @Test
    void testNextSentenceAtBeginningOfParagraph() throws Exception {
        String output = runProgram("/examples/next_sentence_beginning_of_paragraph.babycob");

        assertTrue(output.contains("Alive"), "Output should contain 'Alive'");
        assertFalse(output.contains("Dead code 1"),
            "Output should NOT contain 'Dead code 1' (NEXT SENTENCE should skip it)");
        assertFalse(output.contains("Dead code 2"),
            "Output should NOT contain 'Dead code 2' (NEXT SENTENCE should skip it)");
    }

    /**
     * NEXT SENTENCE. (with a dot) does nothing, since it transfers control to the
     * next sentence that was about to be executed anyway. So it can be used
     * in any place where a sentence is expected but no action is needed
     */
    @Test
    void testNextSentenceAloneDoesNothing() throws Exception {
        String output = runProgram("/examples/next_sentence_alone.babycob");

        assertTrue(output.contains("Before"), "Output should contain 'Before'");
        assertTrue(output.contains("After"), "Output should contain 'After'");
    }

    /**
     * when executed, NEXT SENTENCE should take the computation to the beginning of
     * the next sentence and cleanly exit any IFs and LOOPs that stand in the way
     */
    @Test
    void testNextSentenceExitsLoop() throws Exception {
        String output = runProgram("/examples/next_sentence_in_loop.babycob");

        assertTrue(output.contains("Start"), "Output should contain 'Start'");
        // the loop body runs only once because NEXT SENTENCE exits the loop entirely
        assertTrue(output.contains("Loop"), "Output should contain 'Loop'");
        assertFalse(output.contains("Skipped in loop"),
            "Output should NOT contain 'Skipped in loop' (NEXT SENTENCE should skip it)");
        assertTrue(output.contains("After loop"), "Output should contain 'After loop'");
    }

    /**
     * it is valid to use the NEXT SENTENCE from the last sentence of a paragraph
     * in which case the execution continues in the next paragraph or terminates
     * the program if there are no more statements to execute.
     */
    @Test
    void testNextSentenceAsLastSentenceOfProgram() throws Exception {
        String output = runProgram("/examples/next_sentence_last_sentence_of_program.babycob");

        assertTrue(output.contains("Before"), "Output should contain 'Before'");
        assertFalse(output.contains("After"),
            "Output should NOT contain 'After' (program should terminate cleanly)");
    }
}
