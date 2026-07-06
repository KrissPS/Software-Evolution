package vmodel_tests.component_level_tests;

import ast.ASTUtils;
import org.junit.jupiter.api.Test;
import preprocessing.BabyCobolParserUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CopyPreprocessorTest {

    @Test
    void expandsCopyBookBeforeParsingDataDivision() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. COPYTEST.

                DATA DIVISION.
                COPY COMMON.

                PROCEDURE DIVISION.
                DISPLAY X.
                """;

        String processed = BabyCobolParserUtils.preprocess(code);

        assertTrue(processed.contains("01 X PICTURE IS 9."),
                "COPY COMMON should inline the X declaration, got:\n" + processed);
        assertTrue(processed.contains("01 Y PICTURE IS 99."),
                "COPY COMMON should inline the Y declaration, got:\n" + processed);
        assertDoesNotThrow(() -> ASTUtils.buildASTAndSymbolTable(processed));
    }

    @Test
    void expandsCopyBookWithReplacingBeforeParsing() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. COPYTEST.

                DATA DIVISION.
                COPY PERSON REPLACING ===XXX=== BY ===USER===.

                PROCEDURE DIVISION.
                DISPLAY USER-NAME.
                """;

        String processed = BabyCobolParserUtils.preprocess(code);

        assertTrue(processed.contains("01 USER-NAME PICTURE IS A(10)."),
                "COPY REPLACING should replace names in the copybook, got:\n" + processed);
        assertTrue(processed.contains("01 USER-AGE PICTURE IS 99."),
                "COPY REPLACING should replace all occurrences in the copybook, got:\n" + processed);
        assertFalse(processed.contains("XXX"),
                "COPY REPLACING should not leave the old placeholder behind, got:\n" + processed);
        assertDoesNotThrow(() -> ASTUtils.buildASTAndSymbolTable(processed));
    }

    @Test
    void copyDirectiveKeywordsAreCaseInsensitive() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. COPYTEST.

                DATA DIVISION.
                copy person replacing ===XXX=== by ===USER===.

                PROCEDURE DIVISION.
                DISPLAY USER-NAME.
                """;

        String processed = BabyCobolParserUtils.preprocess(code);

        assertTrue(processed.contains("01 USER-NAME PICTURE IS A(10)."),
                "COPY keywords should be case-insensitive, got:\n" + processed);
        assertDoesNotThrow(() -> ASTUtils.buildASTAndSymbolTable(processed));
    }

    @Test
    void missingCopyBookThrowsClearError() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. COPYTEST.

                DATA DIVISION.
                COPY DOES-NOT-EXIST.

                PROCEDURE DIVISION.
                STOP.
                """;

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                BabyCobolParserUtils.preprocess(code)
        );

        assertTrue(exception.getMessage().contains("COPY"),
                "Missing copybook error should mention COPY, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("DOES-NOT-EXIST"),
                "Missing copybook error should mention the copybook name, got: " + exception.getMessage());
    }

    @Test
    void malformedReplacingClauseThrowsClearError() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. COPYTEST.

                DATA DIVISION.
                COPY PERSON REPLACING XXX BY USER.

                PROCEDURE DIVISION.
                STOP.
                """;

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                BabyCobolParserUtils.preprocess(code)
        );

        assertTrue(exception.getMessage().contains("COPY"),
                "Malformed REPLACING error should mention COPY, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("REPLACING"),
                "Malformed REPLACING error should mention REPLACING, got: " + exception.getMessage());
    }
}
