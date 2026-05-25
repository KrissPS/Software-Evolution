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

    @Test
    void testAddWithLiteralTargetAndGivingClause() {
        String code = "IDENTIFICATION DIVISION. PROGRAM-ID. TEST. PROCEDURE DIVISION. ADD 1 TO 2 GIVING A.";
        assertDoesNotThrow(() -> ast.ASTUtils.buildAST(code));
    }

    @Test
    void testAddWithLiteralTargetMissingGivingClause() {
        String code = "IDENTIFICATION DIVISION. PROGRAM-ID. TEST. PROCEDURE DIVISION. ADD 1 TO 2.";
        Exception exception = assertThrows(IllegalArgumentException.class, () -> ast.ASTUtils.buildAST(code));
        assertEquals("If the second argument of ADD is a literal the GIVING clause is mandatory.", exception.getMessage());
    }

    @Test
    void testAddWithIdentifierTargetMissingGivingClause() {
        String code = "IDENTIFICATION DIVISION. PROGRAM-ID. TEST. PROCEDURE DIVISION. ADD 1 TO A.";
        assertDoesNotThrow(() -> ast.ASTUtils.buildAST(code));
    }

    // @Test
    // void testSemanticValidTypes() {
    //     // TODO: add test 
    //     return true;
    // }

    // @Test
    // void testSemanticErrorInvalidTypeBinding() {
    //     // TODO: add test
    //     return true;
    // }
}
