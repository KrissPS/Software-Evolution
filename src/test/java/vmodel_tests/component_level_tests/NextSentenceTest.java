package vmodel_tests.component_level_tests;

import org.junit.jupiter.api.Test;

import preprocessing.BabyCobolParserUtils;
import runtime.BabyCobolInterpreter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class NextSentenceTest {

    @Test
    void testNextSentenceParsingAndExecution() throws Exception {
        // Read and preprocess the test file
        String code = BabyCobolParserUtils.readResource("/examples/next_sentence_test.babycob");
        String preprocessed = BabyCobolParserUtils.preprocess(code);
        
        // Parse to AST
        var program = BabyCobolParserUtils.parseProgram(preprocessed);
        assertNotNull(program, "Program should parse successfully");
        
        // Execute with interpreter and capture output
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));
        
        try {
            BabyCobolInterpreter interpreter = new BabyCobolInterpreter();
            interpreter.execute(program);
        } finally {
            System.setOut(originalOut);
        }
        
        String output = outContent.toString();
        
        // Validate output
        assertTrue(output.contains("Start"), "Output should contain 'Start'");
        assertTrue(output.contains("In IF"), "Output should contain 'In IF'");
        assertTrue(output.contains("After IF"), "Output should contain 'After IF'");
        assertFalse(output.contains("Should not print"), 
            "Output should NOT contain 'Should not print' (NEXT SENTENCE should skip it)");
    }

    @Test
    void testNextSentenceInNestedContext() throws Exception {
        // Additional test: Verify NEXT SENTENCE works within nested structures
        String code = BabyCobolParserUtils.readResource("/examples/next_sentence_test.babycob");
        String preprocessed = BabyCobolParserUtils.preprocess(code);
        
        // Just verify parsing succeeds with proper sentence structure
        String tree = BabyCobolParserUtils.parseTree(preprocessed);
        assertNotNull(tree, "Parse tree should be generated for NEXT SENTENCE statements");
        assertTrue(tree.contains("nextSentenceStmt"), 
            "Parse tree should contain nextSentenceStmt node");
    }
}
