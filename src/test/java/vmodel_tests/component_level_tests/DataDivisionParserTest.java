package vmodel_tests.component_level_tests;

import ast.ASTNode;
import ast.ASTUtils;
import ast.Symbol;
import ast.SymbolTable;
import org.junit.jupiter.api.Test;
import preprocessing.BabyCobolParserUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * component level tests for DATA DIVISION parsing and AST construction
 */
public class DataDivisionParserTest {

    // --- positive Tests ---

    @Test
    void testSimplePictureField() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 MYFIELD PICTURE IS 9(5).

                PROCEDURE DIVISION.
                DISPLAY MYFIELD.
                """;

        ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));

        SymbolTable st = result.symbolTable;
        assertTrue(st.contains("MYFIELD"), "MYFIELD should be in symbol table");
        Symbol s = st.getSymbol("MYFIELD");
        assertEquals(1, s.getLevel());
        assertEquals("9(5)", s.getPicture());
        assertEquals(0, s.getOccurs());
    }

    @Test
    void testAlphabeticPicture() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 FLAG PICTURE IS A.

                PROCEDURE DIVISION.
                DISPLAY FLAG.
                """;

        ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));
        assertTrue(result.symbolTable.contains("FLAG"));
        assertEquals("A", result.symbolTable.getSymbol("FLAG").getPicture());
    }

    @Test
    void testAlphanumericPicture() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 NAME-FIELD PICTURE IS X(30).

                PROCEDURE DIVISION.
                DISPLAY NAME-FIELD.
                """;

        ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));
        assertTrue(result.symbolTable.contains("NAME-FIELD"));
        assertEquals("X(30)", result.symbolTable.getSymbol("NAME-FIELD").getPicture());
    }

    @Test
    void testPictureWithDecimalV() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 SALARY PICTURE IS 9(7)V99.

                PROCEDURE DIVISION.
                DISPLAY SALARY.
                """;

        ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));
        assertTrue(result.symbolTable.contains("SALARY"));
        assertEquals("9(7)V99", result.symbolTable.getSymbol("SALARY").getPicture());
    }

    @Test
    void testPictureWithSign() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 SIGNED-VAL PICTURE IS S9(4).

                PROCEDURE DIVISION.
                DISPLAY SIGNED-VAL.
                """;

        ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));
        assertTrue(result.symbolTable.contains("SIGNED-VAL"));
        assertTrue(result.symbolTable.getSymbol("SIGNED-VAL").getPicture().contains("S"));
    }

    @Test
    void testPictureWithZ() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 ZIPPY PICTURE IS Z(5)9.

                PROCEDURE DIVISION.
                DISPLAY ZIPPY.
                """;

        ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));
        assertTrue(result.symbolTable.contains("ZIPPY"));
        assertTrue(result.symbolTable.getSymbol("ZIPPY").getPicture().contains("Z"));
    }

    @Test
    void testLikeClause() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 COUNTER PICTURE IS 9(3).
                01 AMOUNT LIKE COUNTER.

                PROCEDURE DIVISION.
                DISPLAY AMOUNT.
                """;

        ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));
        assertTrue(result.symbolTable.contains("COUNTER"));
        assertTrue(result.symbolTable.contains("AMOUNT"));
        // AMOUNT should have the same picture as COUNTER
        assertEquals("9(3)", result.symbolTable.getSymbol("AMOUNT").getPicture());
    }

    @Test
    void testOccursClause() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 ARR PICTURE IS 9(5) OCCURS 10 TIMES.

                PROCEDURE DIVISION.
                DISPLAY ARR.
                """;

        ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));
        Symbol s = result.symbolTable.getSymbol("ARR");
        assertEquals(10, s.getOccurs());
    }

    @Test
    void testLikeWithOccurs() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 BASE PICTURE IS X(10).
                01 COPYCAT LIKE BASE OCCURS 5 TIMES.

                PROCEDURE DIVISION.
                DISPLAY COPYCAT.
                """;

        ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));
        Symbol s = result.symbolTable.getSymbol("COPYCAT");
        assertEquals("X(10)", s.getPicture());
        assertEquals(5, s.getOccurs());
    }

    @Test
    void testRecordHierarchy() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 EMPLOYEE.
                   05 NAME PICTURE IS X(30).
                   05 ID PICTURE IS 9(5).

                PROCEDURE DIVISION.
                DISPLAY NAME.
                """;

        ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));

        Symbol employee = result.symbolTable.getSymbol("EMPLOYEE");
        assertTrue(employee.isRecord(), "EMPLOYEE should be a record (no picture)");
        assertEquals(1, employee.getLevel());

        Symbol name = result.symbolTable.getSymbol("NAME");
        assertEquals(5, name.getLevel());
        assertEquals("X(30)", name.getPicture());
        assertEquals("EMPLOYEE", name.getParentName(),"NAME should have EMPLOYEE as parent");

        Symbol id = result.symbolTable.getSymbol("ID");
        assertEquals(5, id.getLevel());
        assertEquals("9(5)", id.getPicture());
        assertEquals("EMPLOYEE", id.getParentName());
    }

    @Test
    void testMultiLevelRecordHierarchy() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 CUSTOMER.
                   05 CUST-NAME PICTURE IS X(50).
                   05 CUST-ADDR.
                      10 STREET PICTURE IS X(30).
                      10 CITY PICTURE IS X(20).
                   05 CUST-ID PICTURE IS 9(7).

                PROCEDURE DIVISION.
                DISPLAY STREET.
                """;

        ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));

        Symbol custAddr = result.symbolTable.getSymbol("CUST-ADDR");
        assertTrue(custAddr.isRecord(), "CUST-ADDR should be a record");
        assertEquals(5, custAddr.getLevel());
        assertEquals("CUSTOMER", custAddr.getParentName());

        Symbol street = result.symbolTable.getSymbol("STREET");
        assertEquals(10, street.getLevel());
        assertEquals("CUST-ADDR", street.getParentName());

        Symbol city = result.symbolTable.getSymbol("CITY");
        assertEquals(10, city.getLevel());
        assertEquals("CUST-ADDR", city.getParentName());

        Symbol custId = result.symbolTable.getSymbol("CUST-ID");
        assertEquals(5, custId.getLevel());
        assertEquals("CUSTOMER", custId.getParentName());
    }

    @Test
    void testOccursOnGroupRecord() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 MONTHLY-SALES OCCURS 12 TIMES.
                   05 MONTH-NO PICTURE IS 9(2).
                   05 AMOUNT PICTURE IS 9(7)V99.

                PROCEDURE DIVISION.
                DISPLAY MONTH-NO.
                """;

        ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));

        Symbol monthlySales = result.symbolTable.getSymbol("MONTHLY-SALES");
        assertTrue(monthlySales.isRecord(), "MONTHLY-SALES should be a record");
        assertEquals(12, monthlySales.getOccurs(),"MONTHLY-SALES should have OCCURS 12");

        Symbol monthNo = result.symbolTable.getSymbol("MONTH-NO");
        assertEquals("MONTHLY-SALES", monthNo.getParentName());
    }

    @Test
    void testOptionalIsKeyword() throws Exception {
        // PICTURE IS? means IS is optional
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 VAL PICTURE 9(3).

                PROCEDURE DIVISION.
                DISPLAY VAL.
                """;

        ASTUtils.ASTResult result = ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));
        assertTrue(result.symbolTable.contains("VAL"));
        assertEquals("9(3)", result.symbolTable.getSymbol("VAL").getPicture());
    }

    @Test
    void testFirstLevelNot01() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                05 FIRST-FIELD PICTURE IS 9.
                10 SECOND-FIELD PICTURE IS X(5).
                05 THIRD-FIELD PICTURE IS A(3).

                PROCEDURE DIVISION.
                DISPLAY FIRST-FIELD.
                """;

        assertDoesNotThrow(() -> {
            ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));
        });
    }

    // --- negative Tests ---

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
            ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));
        });

        assertTrue(exception.getMessage().contains("cannot be below the first entry"),
                "Expected 'cannot be below the first entry' error, got: " + exception.getMessage());
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
            ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));
        });

        assertTrue(exception.getMessage().contains("must be exactly two digits"),
                "Expected two-digit level error, got: " + exception.getMessage());
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
            ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));
        });

        assertTrue(exception.getMessage().contains("must be exactly two digits"),
                "Expected two-digit level error, got: " + exception.getMessage());
    }

    @Test
    void testMultipleSPictureRejected() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 BAD-FIELD PICTURE IS SS9(4).

                PROCEDURE DIVISION.
                DISPLAY BAD-FIELD.
                """;

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));
        });

        assertTrue(exception.getMessage().contains("multiple S symbols"),
                "Expected 'multiple S symbols' error, got: " + exception.getMessage());
    }

    @Test
    void testMultipleVPictureRejected() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 BAD-FIELD PICTURE IS 9(3)VV99.

                PROCEDURE DIVISION.
                DISPLAY BAD-FIELD.
                """;

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));
        });

        assertTrue(exception.getMessage().contains("multiple V symbols"),
                "Expected 'multiple V symbols' error, got: " + exception.getMessage());
    }

    @Test
    void testInvalidPictureCharacterRejected() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 BAD-FIELD PICTURE IS 9B(5).

                PROCEDURE DIVISION.
                DISPLAY BAD-FIELD.
                """;

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));
        });

        assertTrue(exception.getMessage().contains("Invalid character"),
                "Expected 'Invalid character' error, got: " + exception.getMessage());
    }

    @Test
    void testLikeReferencesUnknownSymbol() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TEST.

                DATA DIVISION.
                01 BAD-FIELD LIKE NONEXISTENT.

                PROCEDURE DIVISION.
                DISPLAY BAD-FIELD.
                """;

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));
        });

        assertTrue(exception.getMessage().contains("LIKE references unknown symbol") ||
                exception.getMessage().contains("unknown symbol"),
                "Expected 'LIKE references unknown symbol' error, got: " + exception.getMessage());
    }
}