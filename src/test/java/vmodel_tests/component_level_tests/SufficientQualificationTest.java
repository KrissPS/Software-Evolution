package vmodel_tests.component_level_tests;

import ast.ASTUtils;
import org.junit.jupiter.api.Test;
import preprocessing.BabyCobolParserUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * component level tests for sufficient qualification of field references.
 */
public class SufficientQualificationTest {

    // --- positive tests ---

    @Test
    void testFullyQualifiedReference() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 A.
                   03 B.
                      05 C.
                         07 D PICTURE IS 999.
                      05 E.
                         07 D PICTURE IS 999.

                PROCEDURE DIVISION.
                MOVE 1 TO D OF C OF B OF A.
                """;

        assertDoesNotThrow(() -> ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code)));
    }

    @Test
    void testSufficientlyQualifiedReference() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 A.
                   03 B.
                      05 C.
                         07 D PICTURE IS 999.
                      05 E.
                         07 D PICTURE IS 999.

                PROCEDURE DIVISION.
                MOVE 1 TO D OF C.
                """;

        assertDoesNotThrow(() -> ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code)));
    }

    @Test
    void testLikeWithSufficientlyQualifiedRecord() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 A.
                   03 B.
                      05 C.
                         07 D PICTURE IS 999.
                01 G LIKE C OF B OF A.

                PROCEDURE DIVISION.
                MOVE 1 TO D OF G.
                """;

        assertDoesNotThrow(() -> ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code)));
    }

    // --- negative tests ---

    @Test
    void testUnqualifiedAmbiguousReferenceFails() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 A.
                   03 B.
                      05 C.
                         07 D PICTURE IS 999.
                      05 E.
                         07 D PICTURE IS 999.

                PROCEDURE DIVISION.
                MOVE 1 TO D.
                """;

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));
        });

        assertTrue(exception.getMessage().toLowerCase().contains("ambiguous"),
                "Expected ambiguity error for unqualified D, got: " + exception.getMessage());
    }

    @Test
    void testInsufficientlyQualifiedReferenceFails() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 A.
                   03 B.
                      05 C.
                         07 D PICTURE IS 999.
                      05 E.
                         07 D PICTURE IS 999.

                PROCEDURE DIVISION.
                MOVE 1 TO D OF B.
                """;

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));
        });

        assertTrue(exception.getMessage().toLowerCase().contains("ambiguous") ||
                        exception.getMessage().toLowerCase().contains("insufficient"),
                "Expected ambiguity/insufficient qualification error for D OF B, got: " + exception.getMessage());
    }

    @Test
    void testLikeAmbiguousRecordFails() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 A.
                   03 B.
                      05 C.
                         07 D PICTURE IS 999.
                   03 C.
                      05 D PICTURE IS 999.
                01 G LIKE C.

                PROCEDURE DIVISION.
                DISPLAY G.
                """;

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));
        });

        assertTrue(exception.getMessage().toLowerCase().contains("ambiguous"),
                "Expected ambiguity error for LIKE C, got: " + exception.getMessage());
    }
}
