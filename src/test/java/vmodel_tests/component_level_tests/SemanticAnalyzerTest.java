package vmodel_tests.component_level_tests;

import ast.ASTNode;
import ast.BuildASTVisitor;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import parser.BabyCobolLexer;
import parser.BabyCobolParser;

import static org.junit.jupiter.api.Assertions.*;

public class SemanticAnalyzerTest {

    /**
     * ADD statement tests ----------
     * if the second argument is a literal, the third argument is mandatory
     */
    @Test
    void testAddWithLiteralTargetAndGivingClause() {
        String code = "IDENTIFICATION DIVISION. PROGRAM-ID. TEST. PROCEDURE DIVISION. ADD 1 TO 2 GIVING A.";
        assertDoesNotThrow(() -> ast.ASTUtils.buildAST(code));
    }

    @Test
    void testAddWithLiteralTargetMissingGivingClause() {
        String code = "IDENTIFICATION DIVISION. PROGRAM-ID. TEST. PROCEDURE DIVISION. ADD 1 TO 2.";
        Exception exception = assertThrows(IllegalArgumentException.class, () -> ast.ASTUtils.buildAST(code));
        assertEquals("If the second argument of ADD is a literal, the GIVING clause is mandatory.", exception.getMessage());
    }

    @Test
    void testAddWithIdentifierTargetMissingGivingClause() {
        String code = "IDENTIFICATION DIVISION. PROGRAM-ID. TEST. PROCEDURE DIVISION. ADD 1 TO A.";
        assertDoesNotThrow(() -> ast.ASTUtils.buildAST(code));
    }


    /**
     * DIVIDE statement tests ----------
     * if the second argument is a literal, the third argument is mandatory
     * if the third argument is present, there can be only one second argument
     * if the fourth argument is present, there can be only one third argument
     */
    @Test
    void testDivideWithLiteralTargetMissingGivingClause() {
        String code = "IDENTIFICATION DIVISION. PROGRAM-ID. TEST. PROCEDURE DIVISION. DIVIDE 1 INTO 2.";
        Exception exception = assertThrows(IllegalArgumentException.class, () -> ast.ASTUtils.buildAST(code));
        assertEquals("If the second argument is a literal, the third argument is mandatory.", exception.getMessage());
    }

    @Test
    void testDivideWithLiteralTargetAndGivingClause() {
        String code = "IDENTIFICATION DIVISION. PROGRAM-ID. TEST. PROCEDURE DIVISION. DIVIDE 1 INTO 2 GIVING A.";
        assertDoesNotThrow(() -> ast.ASTUtils.buildAST(code));
    }

    @Test
    void testDivideWithThirdArgAndMultipleSecondArgs() {
        String code = "IDENTIFICATION DIVISION. PROGRAM-ID. TEST. PROCEDURE DIVISION. DIVIDE 1 INTO A B GIVING C.";
        Exception exception = assertThrows(IllegalArgumentException.class, () -> ast.ASTUtils.buildAST(code));
        assertEquals("If the third argument is present, there can be only one second argument.", exception.getMessage());
    }

    @Test
    void testDivideWithFourthArgAndMultipleThirdArgs() {
        String code = "IDENTIFICATION DIVISION. PROGRAM-ID. TEST. PROCEDURE DIVISION. DIVIDE 1 INTO A GIVING B C REMAINDER D.";
        Exception exception = assertThrows(IllegalArgumentException.class, () -> ast.ASTUtils.buildAST(code));
        assertEquals("If the fourth argument is present, there can be only one third argument.", exception.getMessage());
    }


    /**
     * MULTIPLY statement tests ----------
     * if the second argument is a literal, the third argument is mandatory
     */
    @Test
    void testMultiplyWithLiteralTargetMissingGivingClause() {
        String code = "IDENTIFICATION DIVISION. PROGRAM-ID. TEST. PROCEDURE DIVISION. MULTIPLY 1 BY 2.";
        Exception exception = assertThrows(IllegalArgumentException.class, () -> ast.ASTUtils.buildAST(code));
        assertEquals("If the second argument of MULTIPLY is a literal, the GIVING clause is mandatory.", exception.getMessage());
    }

    @Test
    void testMultiplyWithLiteralTargetAndGivingClause() {
        String code = "IDENTIFICATION DIVISION. PROGRAM-ID. TEST. PROCEDURE DIVISION. MULTIPLY 1 BY 2 GIVING A.";
        assertDoesNotThrow(() -> ast.ASTUtils.buildAST(code));
    }


    /**
     * SUBTRACT statement tests ----------
     * if the second argument is a literal, the third argument is mandatory
     */
    @Test
    void testSubtractWithLiteralTargetMissingGivingClause() {
        String code = "IDENTIFICATION DIVISION. PROGRAM-ID. TEST. PROCEDURE DIVISION. SUBTRACT 1 FROM 2.";
        Exception exception = assertThrows(IllegalArgumentException.class, () -> ast.ASTUtils.buildAST(code));
        assertEquals("If the second argument of SUBTRACT is a literal, the GIVING clause is mandatory.", exception.getMessage());
    }

    @Test
    void testSubtractWithLiteralTargetAndGivingClause() {
        String code = "IDENTIFICATION DIVISION. PROGRAM-ID. TEST. PROCEDURE DIVISION. SUBTRACT 1 FROM 2 GIVING A.";
        assertDoesNotThrow(() -> ast.ASTUtils.buildAST(code));
    }

}
