package vmodel_tests.component_level_tests;

import org.junit.jupiter.api.Test;
import ast.BuildASTVisitor;
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

    @Test
    void testTwoDigitLevelNumbersValid() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 FIELD-A PICTURE IS 9.
                05 FIELD-B PICTURE IS X(10).
                10 FIELD-C PICTURE IS A(5).
                99 FIELD-D PICTURE IS 99.

                PROCEDURE DIVISION.
                DISPLAY FIELD-A.
                """;

        assertDoesNotThrow(() -> {
            var program = BabyCobolParserUtils.parseProgram(BabyCobolParserUtils.preprocess(code));
            BuildASTVisitor visitor = new BuildASTVisitor();
            visitor.visit(program);
        });
    }

    @Test
    void testSingleDigitLevelNumberRejected() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                1 FIELD-A PICTURE IS 9.

                PROCEDURE DIVISION.
                DISPLAY FIELD-A.
                """;

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            var program = BabyCobolParserUtils.parseProgram(BabyCobolParserUtils.preprocess(code));
            BuildASTVisitor visitor = new BuildASTVisitor();
            visitor.visit(program);
        });

        assertTrue(exception.getMessage().contains("must be exactly two digits"),
                "Expected message about two-digit level numbers, got: " + exception.getMessage());
    }

    @Test
    void testThreeDigitLevelNumberRejected() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                001 FIELD-A PICTURE IS 9.

                PROCEDURE DIVISION.
                DISPLAY FIELD-A.
                """;

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            var program = BabyCobolParserUtils.parseProgram(BabyCobolParserUtils.preprocess(code));
            BuildASTVisitor visitor = new BuildASTVisitor();
            visitor.visit(program);
        });

        assertTrue(exception.getMessage().contains("must be exactly two digits"),
                "Expected message about two-digit level numbers, got: " + exception.getMessage());
    }

    @Test
    void testLevelNeverBelowFirstLevelValid() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                05 GROUP-A.
                10 FIELD-B PICTURE IS 9.
                05 FIELD-C PICTURE IS X(5).
                10 FIELD-D PICTURE IS A(3).

                PROCEDURE DIVISION.
                DISPLAY FIELD-C.
                """;

        assertDoesNotThrow(() -> {
            var program = BabyCobolParserUtils.parseProgram(BabyCobolParserUtils.preprocess(code));
            BuildASTVisitor visitor = new BuildASTVisitor();
            visitor.visit(program);
        });
    }

    @Test
    void testLevelBelowFirstLevelRejected() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                05 FIELD-A PICTURE IS 9.
                03 FIELD-B PICTURE IS X(5).

                PROCEDURE DIVISION.
                DISPLAY FIELD-A.
                """;

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            var program = BabyCobolParserUtils.parseProgram(BabyCobolParserUtils.preprocess(code));
            BuildASTVisitor visitor = new BuildASTVisitor();
            visitor.visit(program);
        });

        assertTrue(exception.getMessage().contains("cannot be below the first entry"),
                "Expected message about level below first entry, got: " + exception.getMessage());
    }
}
