package vmodel_tests.component_level_tests;

import ast.ASTUtils;
import org.junit.jupiter.api.Test;
import preprocessing.BabyCobolParserUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CallAstTest {

    @Test
    void callWithoutUsingShouldAppearInAst() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. MAIN.

                PROCEDURE DIVISION.
                CALL HELLO.
                """;

        ASTUtils.ASTResult result =
                ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));

        String ast = result.root.printTree();

        assertTrue(ast.contains("CallStmt: HELLO"),
                "CALL statement should be preserved in AST, got:\n" + ast);
    }

    @Test
    void callUsingIdentifiersShouldAppearInAst() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. MAIN.

                DATA DIVISION.
                01 FIELD-A PICTURE IS 9.
                01 FIELD-B PICTURE IS X.

                PROCEDURE DIVISION.
                CALL HELLO USING FIELD-A FIELD-B.
                """;

        ASTUtils.ASTResult result =
                ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));

        String ast = result.root.printTree();

        assertTrue(ast.contains("CallStmt: HELLO"),
                "CALL statement should be preserved in AST, got:\n" + ast);
        assertTrue(ast.contains("UsingCallClause"),
                "CALL USING clause should be preserved in AST, got:\n" + ast);
        assertTrue(ast.contains("ID: FIELD-A"),
                "First CALL USING argument should be preserved in AST, got:\n" + ast);
        assertTrue(ast.contains("ID: FIELD-B"),
                "Second CALL USING argument should be preserved in AST, got:\n" + ast);
    }

    @Test
    void procedureDivisionUsingIdentifiersShouldAppearInAst() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. HELLO.

                DATA DIVISION.
                01 FIELD-A PICTURE IS 9.
                01 FIELD-B PICTURE IS X.

                PROCEDURE DIVISION USING FIELD-A FIELD-B.
                DISPLAY FIELD-A.
                """;

        ASTUtils.ASTResult result =
                ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));

        String ast = result.root.printTree();

        assertTrue(ast.contains("UsingProcedureClause"),
                "PROCEDURE DIVISION USING clause should be preserved in AST, got:\n" + ast);
        assertTrue(ast.contains("ID: FIELD-A"),
                "First PROCEDURE USING parameter should be preserved in AST, got:\n" + ast);
        assertTrue(ast.contains("ID: FIELD-B"),
                "Second PROCEDURE USING parameter should be preserved in AST, got:\n" + ast);
    }
}
