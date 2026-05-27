package vmodel_tests.component_level_tests;

import org.junit.jupiter.api.Test;
import preprocessing.BabyCobolParserUtils;

import static org.junit.jupiter.api.Assertions.*;

public class SyntaxAnalyzerTest {

    @Test
    void testValidASTBuilt() throws Exception {
        String code = "IDENTIFICATION DIVISION. PROGRAM-ID. HELLO. PROCEDURE DIVISION. DISPLAY \"Hello World\" STOP.";
        
        assertDoesNotThrow(() -> {
            String tree = BabyCobolParserUtils.parseTree(code);
            assertNotNull(tree, "Parse tree should represent a valid AST string for correct code");
            assertTrue(tree.contains("IDENTIFICATION"), "AST should contain nodes for IDENTIFICATION DIVISION");
        });
    }

    @Test
    void testSyntaxErrorThrownOnInvalidCode() {
        // Missing the sentence ending DOT after DISPLAY statement
        String code = "IDENTIFICATION DIVISION. PROGRAM-ID. HELLO. PROCEDURE DIVISION. DISPLAY \"Hello\"";
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            BabyCobolParserUtils.parseTree(code);
        }, "Syntax errors should throw RuntimeException as per our BaseErrorListener");

        assertTrue(exception.getMessage().contains("Syntax Error"), "Exception message should indicate a syntax error");
    }

    @Test
    void testDisplayStatements() throws Exception {
        String code = BabyCobolParserUtils.readResource("/examples/parser_display_stmt.babycob");
        String tree = BabyCobolParserUtils.parseTree(BabyCobolParserUtils.preprocess(code));
        assertNotNull(tree);
    }

    @Test
    void testAcceptStatements() throws Exception {
        String code = BabyCobolParserUtils.readResource("/examples/parser_accept_stmt.babycob");
        String tree = BabyCobolParserUtils.parseTree(BabyCobolParserUtils.preprocess(code));
        assertNotNull(tree);
    }

    @Test
    void testArithmeticStatements() throws Exception {
        String code = BabyCobolParserUtils.readResource("/examples/parser_arithmetic_stmt.babycob");
        String tree = BabyCobolParserUtils.parseTree(BabyCobolParserUtils.preprocess(code));
        assertNotNull(tree);
    }

    @Test
    void testSubtractStatements() throws Exception {
        String code = BabyCobolParserUtils.readResource("/examples/parser_subtract_stmt.babycob");
        String tree = BabyCobolParserUtils.parseTree(BabyCobolParserUtils.preprocess(code));
        assertNotNull(tree);
    }

    @Test
    void testEvaluateStatements() throws Exception {
        String code = BabyCobolParserUtils.readResource("/examples/parser_evaluate_stmt.babycob");
        String tree = BabyCobolParserUtils.parseTree(BabyCobolParserUtils.preprocess(code));
        assertNotNull(tree);
    }

    @Test
    void testPerformStatements() throws Exception {
        String code = BabyCobolParserUtils.readResource("/examples/parser_perform_stmt.babycob");
        String tree = BabyCobolParserUtils.parseTree(BabyCobolParserUtils.preprocess(code));
        assertNotNull(tree);
    }

    @Test
    void testDataDivisionClauses() throws Exception {
        String code = BabyCobolParserUtils.readResource("/examples/parser_data_division.babycob");
        String tree = BabyCobolParserUtils.parseTree(BabyCobolParserUtils.preprocess(code));
        assertNotNull(tree);
    }
}
