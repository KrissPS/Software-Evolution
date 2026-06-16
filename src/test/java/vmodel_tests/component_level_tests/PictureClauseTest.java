package vmodel_tests.component_level_tests;

import ast.ASTUtils;
import org.junit.jupiter.api.Test;
import preprocessing.BabyCobolParserUtils;

import static org.junit.jupiter.api.Assertions.*;

public class PictureClauseTest {

    @Test
    void testValidPictureClausesParse() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 NUM1 PICTURE IS 9.
                01 NUM2 PICTURE IS 99.
                01 NUM3 PICTURE IS 9(3).
                01 NAME PICTURE IS A(10).
                01 TEXT PICTURE IS X(20).
                01 ZEROES PICTURE IS Z(5).
                01 SIGNED PICTURE IS S9(3).
                01 DECIMAL-VALUE PICTURE IS 9V99.
                01 MONEY PICTURE IS S9(3)V99.

                PROCEDURE DIVISION.
                DISPLAY NUM1
                DISPLAY NAME
                DISPLAY MONEY.
                """;

        assertDoesNotThrow(() -> {
            String processed = BabyCobolParserUtils.preprocess(code);
            String tree = BabyCobolParserUtils.parseTree(processed);

            assertNotNull(tree);
            assertTrue(tree.contains("pictureClause"));
            assertTrue(tree.contains("MONEY"));
        });
    }

    @Test
    void testPictureClausesWorkWithAcceptAndDisplay() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 NAME PICTURE IS A(10).
                01 AGE PICTURE IS 99.
                01 MONEY PICTURE IS S9(3)V99.

                PROCEDURE DIVISION.
                ACCEPT NAME
                ACCEPT AGE
                DISPLAY NAME
                DISPLAY AGE
                DISPLAY MONEY.
                """;

        assertDoesNotThrow(() -> {
            String processed = BabyCobolParserUtils.preprocess(code);
            String tree = BabyCobolParserUtils.parseTree(processed);

            assertNotNull(tree);
            assertTrue(tree.contains("acceptStmt"));
            assertTrue(tree.contains("displayStmt"));
        });
    }

    @Test
    void testPictureWithLikeAndOccursParses() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 AGE PICTURE IS 99.
                01 OTHER-AGE LIKE AGE.
                01 SCORES PICTURE IS 9(3) OCCURS 5 TIMES.

                PROCEDURE DIVISION.
                DISPLAY AGE
                DISPLAY OTHER-AGE
                DISPLAY SCORES.
                """;

        assertDoesNotThrow(() -> {
            String processed = BabyCobolParserUtils.preprocess(code);
            String tree = BabyCobolParserUtils.parseTree(processed);

            assertNotNull(tree);
            assertTrue(tree.contains("likeClause"));
            assertTrue(tree.contains("occursClause"));
        });
    }

    @Test
    void testInvalidPictureWithRepeatedSIsRejectedAtParseTime() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 BAD PICTURE IS SS99.

                PROCEDURE DIVISION.
                DISPLAY BAD.
                """;

        assertThrows(RuntimeException.class, () -> {
            String processed = BabyCobolParserUtils.preprocess(code);
            ASTUtils.buildASTAndSymbolTable(processed);
        });
    }

    @Test
    void testInvalidPictureWithRepeatedVIsRejectedAtParseTime() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 BAD PICTURE IS 9V9V9.

                PROCEDURE DIVISION.
                DISPLAY BAD.
                """;

        assertThrows(RuntimeException.class, () -> {
            String processed = BabyCobolParserUtils.preprocess(code);
            ASTUtils.buildASTAndSymbolTable(processed);
        });
    }

    @Test
    void testMoveAndDisplayWithPictureFields() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 NAME PICTURE IS A(10).
                01 AGE PICTURE IS 99.
                01 CODE PICTURE IS X(5).
                01 MONEY PICTURE IS S9(3)V99.

                PROCEDURE DIVISION.
                MOVE "JOHN" TO NAME
                MOVE 25 TO AGE
                MOVE "A1B2" TO CODE
                DISPLAY NAME
                DISPLAY AGE
                DISPLAY CODE
                DISPLAY MONEY.
                """;

        assertDoesNotThrow(() -> {
            String processed = BabyCobolParserUtils.preprocess(code);
            ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(processed);
            assertNotNull(result.root);
            assertNotNull(result.symbolTable);
        });
    }

    @Test
    void testEmptyPictureIsRejected() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 BADVAL PICTURE IS .

                PROCEDURE DIVISION.
                DISPLAY BADVAL.
                """;

        assertThrows(RuntimeException.class, () -> {
            String processed = BabyCobolParserUtils.preprocess(code);
            ASTUtils.buildASTAndSymbolTable(processed);
        });
    }

    @Test
    void testPictureWithInvalidCharacterBBRejected() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 BADVAL PICTURE IS 9BB9.

                PROCEDURE DIVISION.
                DISPLAY BADVAL.
                """;

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            String processed = BabyCobolParserUtils.preprocess(code);
            ASTUtils.buildASTAndSymbolTable(processed);
        });

        assertTrue(ex.getMessage().contains("Invalid character"),
                "Expected 'Invalid character' error, got: " + ex.getMessage());
    }
}