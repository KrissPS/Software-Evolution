package vmodel_tests.component_level_tests;

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

    @Test
    void testNextSentenceParsingAndExecution() throws Exception {
        // read and preprocess the test file
        String code = BabyCobolParserUtils.readResource("/examples/next_sentence_test.babycob");
        String preprocessed = BabyCobolParserUtils.preprocess(code);

        // parse to AST
        ASTUtils.ASTResult astResult = ASTUtils.buildASTAndSymbolTable(preprocessed);
        assertNotNull(astResult.root, "Program should parse successfully");

        // execute with interpreter and capture output
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            BabyCobolInterpreter interpreter = new BabyCobolInterpreter(astResult.symbolTable);
            interpreter.execute(astResult.root);
        } finally {
            System.setOut(originalOut);
        }

        String output = outContent.toString();

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

        // checl parsing succeeds with proper sentence structure
        String tree = BabyCobolParserUtils.parseTree(preprocessed);
        assertNotNull(tree, "Parse tree should be generated for NEXT SENTENCE statements");
        assertTrue(tree.contains("nextSentenceStmt"),"Parse tree should contain nextSentenceStmt node");
    }
}