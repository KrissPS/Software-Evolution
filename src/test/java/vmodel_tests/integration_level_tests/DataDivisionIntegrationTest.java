package vmodel_tests.integration_level_tests;

import ast.ASTUtils;
import interpreter.BabyCobolInterpreter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import preprocessing.BabyCobolParserUtils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * integration level tests for DATA DIVISION
 */
public class DataDivisionIntegrationTest {

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

    // --- positive tests ---

    @Test
    void testNumericFieldInitializedToZero() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 COUNTER PICTURE IS 9(3).

                PROCEDURE DIVISION.
                DISPLAY COUNTER.
                """;

        String processed = BabyCobolParserUtils.preprocess(code);
        ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(processed);
        BabyCobolInterpreter interpreter = new BabyCobolInterpreter(result.symbolTable);

        // verify memory initialization
        Map<String, Object> memory = interpreter.getMemory();
        assertTrue(memory.containsKey("counter"));
        assertEquals(0.0, memory.get("counter"));

        // execute and verify output
        interpreter.execute(result.root);
        assertTrue(outContent.toString().contains("0.0"),
                "Output should contain default value 0.0 for numeric field");
    }

    @Test
    void testAlphanumericFieldInitializedToEmpty() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 NAME-FIELD PICTURE IS X(30).

                PROCEDURE DIVISION.
                DISPLAY NAME-FIELD.
                """;

        String processed = BabyCobolParserUtils.preprocess(code);
        ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(processed);
        BabyCobolInterpreter interpreter = new BabyCobolInterpreter(result.symbolTable);

        Map<String, Object> memory = interpreter.getMemory();
        assertTrue(memory.containsKey("name-field"));
        assertEquals("", memory.get("name-field"));
    }

    @Test
    void testMoveToNumericField() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 COUNTER PICTURE IS 9(3).

                PROCEDURE DIVISION.
                MOVE 42 TO COUNTER
                DISPLAY COUNTER.
                """;

        String processed = BabyCobolParserUtils.preprocess(code);
        ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(processed);
        BabyCobolInterpreter interpreter = new BabyCobolInterpreter(result.symbolTable);
        interpreter.execute(result.root);

        assertTrue(outContent.toString().contains("42.0"),
                "Output should contain 42.0 after MOVE");
    }

    @Test
    void testMoveToAlphanumericField() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 MSG PICTURE IS X(10).

                PROCEDURE DIVISION.
                MOVE "HELLO" TO MSG
                DISPLAY MSG.
                """;

        String processed = BabyCobolParserUtils.preprocess(code);
        ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(processed);
        BabyCobolInterpreter interpreter = new BabyCobolInterpreter(result.symbolTable);
        interpreter.execute(result.root);

        assertTrue(outContent.toString().contains("HELLO"),
                "Output should contain 'HELLO' after MOVE");
    }

    @Test
    void testLikeCopiesType() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 COUNTER PICTURE IS 9(3).
                01 AMOUNT LIKE COUNTER.

                PROCEDURE DIVISION.
                MOVE 99 TO AMOUNT
                DISPLAY AMOUNT.
                """;

        String processed = BabyCobolParserUtils.preprocess(code);
        ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(processed);
        BabyCobolInterpreter interpreter = new BabyCobolInterpreter(result.symbolTable);

        // AMOUNT should be initialized as numeric (since COUNTER is numeric)
        Map<String, Object> memory = interpreter.getMemory();
        assertTrue(memory.containsKey("amount"));
        assertEquals(0.0, memory.get("amount"),
                "AMOUNT should be initialized as Double (0.0) like COUNTER");

        interpreter.execute(result.root);
        assertTrue(outContent.toString().contains("99.0"),
                "Output should contain 99.0 after MOVE to AMOUNT");
    }

    @Test
    void testOccursArrayInitialization() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 ARR PICTURE IS 9(5) OCCURS 3 TIMES.

                PROCEDURE DIVISION.
                DISPLAY ARR.
                """;

        String processed = BabyCobolParserUtils.preprocess(code);
        ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(processed);
        BabyCobolInterpreter interpreter = new BabyCobolInterpreter(result.symbolTable);

        Map<String, Object> memory = interpreter.getMemory();
        assertTrue(memory.containsKey("arr"));
        assertTrue(memory.get("arr") instanceof Object[],
                "ARR should be initialized as an array");
        Object[] arr = (Object[]) memory.get("arr");
        assertEquals(3, arr.length, "ARR should have 3 elements");
        assertEquals(0.0, arr[0], "First element should be 0.0");
        assertEquals(0.0, arr[1], "Second element should be 0.0");
        assertEquals(0.0, arr[2], "Third element should be 0.0");
    }

    @Test
    void testRecordHierarchyFieldsAccessible() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 EMPLOYEE.
                   05 EMP-NAME PICTURE IS X(20).
                   05 EMP-ID PICTURE IS 9(5).

                PROCEDURE DIVISION.
                MOVE "JOHN" TO EMP-NAME
                MOVE 123 TO EMP-ID
                DISPLAY EMP-NAME
                DISPLAY EMP-ID.
                """;

        String processed = BabyCobolParserUtils.preprocess(code);
        ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(processed);

        // verify symbol table relationships
        assertEquals("EMPLOYEE", result.symbolTable.getSymbol("EMP-NAME").getParentName());
        assertEquals("EMPLOYEE", result.symbolTable.getSymbol("EMP-ID").getParentName());

        BabyCobolInterpreter interpreter = new BabyCobolInterpreter(result.symbolTable);
        interpreter.execute(result.root);

        String stdout = outContent.toString();
        assertTrue(stdout.contains("JOHN"), "Output should contain 'JOHN'");
        assertTrue(stdout.contains("123.0"), "Output should contain '123.0'");
    }

    @Test
    void testArithmeticWithDataDivisionFields() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 A PICTURE IS 9(3).
                01 B PICTURE IS 9(3).
                01 C PICTURE IS 9(3).

                PROCEDURE DIVISION.
                MOVE 10 TO A
                MOVE 5 TO B
                ADD A TO B GIVING C
                DISPLAY C.
                """;

        String processed = BabyCobolParserUtils.preprocess(code);
        ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(processed);
        BabyCobolInterpreter interpreter = new BabyCobolInterpreter(result.symbolTable);
        interpreter.execute(result.root);

        assertTrue(outContent.toString().contains("15.0"),
                "C should be 15.0 (10 + 5)");
    }

    @Test
    void testCoerceStringToNumericOnMove() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 COUNTER PICTURE IS 9(5).

                PROCEDURE DIVISION.
                MOVE "42" TO COUNTER
                DISPLAY COUNTER.
                """;

        String processed = BabyCobolParserUtils.preprocess(code);
        ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(processed);
        BabyCobolInterpreter interpreter = new BabyCobolInterpreter(result.symbolTable);
        interpreter.execute(result.root);

        assertTrue(outContent.toString().contains("42.0"),
                "String '42' should be coerced to Double 42.0");
    }

    // --- negative tests ---

    @Test
    void testDisplayUninitializedVariableThrows() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 MYVAR PICTURE IS 9(3).

                PROCEDURE DIVISION.
                DISPLAY UNKNOWNVAR.
                """;

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            String processed = BabyCobolParserUtils.preprocess(code);
            ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(processed);
            BabyCobolInterpreter interpreter = new BabyCobolInterpreter(result.symbolTable);
            interpreter.execute(result.root);
        });

        assertTrue(exception.getMessage().toLowerCase().contains("not initialized") ||
                exception.getMessage().toLowerCase().contains("unknownvar"),
                "Expected 'not initialized' error for unknown variable, got: " + exception.getMessage());
    }

    @Test
    void testMoveStringToNumericFieldInvalidStringGivesZero() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 COUNTER PICTURE IS 9(5).

                PROCEDURE DIVISION.
                MOVE "ABC" TO COUNTER
                DISPLAY COUNTER.
                """;

        String processed = BabyCobolParserUtils.preprocess(code);
        ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(processed);
        BabyCobolInterpreter interpreter = new BabyCobolInterpreter(result.symbolTable);
        interpreter.execute(result.root);

        assertTrue(outContent.toString().contains("0.0"),
                "Non-numeric string MOVE to numeric field should default to 0.0");
    }
}