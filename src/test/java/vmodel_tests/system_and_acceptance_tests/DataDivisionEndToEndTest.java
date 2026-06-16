package vmodel_tests.system_and_acceptance_tests;

import ast.ASTUtils;
import interpreter.BabyCobolInterpreter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import preprocessing.BabyCobolParserUtils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * system/acceptance level end to end tests for DATA DIVISION
 */
public class DataDivisionEndToEndTest {

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
    }

    @Test
    void testSystemEndToEnd_DataDivision() throws Exception {
        String path = "src/test/java/vmodel_tests/system_and_acceptance_tests/resources/system_data_division_test.babycob";
        String source = Files.readString(Paths.get(path));

        String processedSource = BabyCobolParserUtils.preprocess(source);
        ASTUtils.ASTResult astResult = ASTUtils.buildASTAndSymbolTable(processedSource);

        BabyCobolInterpreter interpreter = new BabyCobolInterpreter(astResult.symbolTable);
        interpreter.execute(astResult.root);

        String stdout = outContent.toString();

        // assert output contains expected values for each data type
        assertTrue(stdout.contains("DATA E2E START"), "Should contain start marker");
        assertTrue(stdout.contains("COUNTER: 1000.0"), "Numeric field should display 1000.0");
        assertTrue(stdout.contains("MESSAGE: HELLO DATA DIVISION"), "Alphanumeric field should display string");
        assertTrue(stdout.contains("SALARY: 50000.99"), "Decimal field should display 50000.99");
        assertTrue(stdout.contains("FLAG: A"), "Alphabetic field should display 'A'");
        assertTrue(stdout.contains("EMP-NAME: JOHN DOE"), "Record subfield EMP-NAME should display");
        assertTrue(stdout.contains("EMP-ID: 42.0"), "Record subfield EMP-ID should display");
        assertTrue(stdout.contains("TOTAL: 1500.0"), "Math with data division fields should work (1000+500=1500)");
        assertTrue(stdout.contains("DATA E2E END"), "Should contain end marker");
    }

    @Test
    void testSystemEndToEnd_DataDivision_Negative_InvalidPicture() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. BADPIC.

                DATA DIVISION.
                01 BAD PICTURE IS SS9(4)V99V.

                PROCEDURE DIVISION.
                DISPLAY BAD.
                """;

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            String processed = BabyCobolParserUtils.preprocess(code);
            ASTUtils.buildASTAndSymbolTable(processed);
        });

        assertTrue(
                exception.getMessage().contains("multiple S symbols") ||
                        exception.getMessage().contains("multiple V symbols") ||
                        exception.getMessage().contains("Invalid character"),
                "Should reject invalid PICTURE: " + exception.getMessage());
    }

    @Test
    void testSystemEndToEnd_DataDivision_Negative_BadLevelNumber() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. BADLVL.

                DATA DIVISION.
                1 BAD PICTURE IS 9.

                PROCEDURE DIVISION.
                DISPLAY BAD.
                """;

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            String processed = BabyCobolParserUtils.preprocess(code);
            ASTUtils.buildASTAndSymbolTable(processed);
        });

        assertTrue(exception.getMessage().contains("must be exactly two digits"),
                "Should reject single-digit level number: " + exception.getMessage());
    }

    @Test
    void testSystemEndToEnd_DataDivision_Negative_LikeUnknown() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. BADLIKE.

                DATA DIVISION.
                01 BAD LIKE GHOST.

                PROCEDURE DIVISION.
                DISPLAY BAD.
                """;

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            String processed = BabyCobolParserUtils.preprocess(code);
            ASTUtils.buildASTAndSymbolTable(processed);
        });

        assertTrue(exception.getMessage().contains("LIKE references unknown symbol") ||
                exception.getMessage().contains("unknown symbol"),
                "Should reject LIKE to unknown symbol: " + exception.getMessage());
    }

    @Test
    void testSystemEndToEnd_DataDivision_Negative_BelowFirstLevel() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. BADLEVEL.

                DATA DIVISION.
                05 A PICTURE IS 9.
                03 B PICTURE IS X(5).

                PROCEDURE DIVISION.
                DISPLAY A.
                """;

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            String processed = BabyCobolParserUtils.preprocess(code);
            ASTUtils.buildASTAndSymbolTable(processed);
        });

        assertTrue(exception.getMessage().contains("cannot be below the first entry"),
                "Should reject level below first entry: " + exception.getMessage());
    }
}