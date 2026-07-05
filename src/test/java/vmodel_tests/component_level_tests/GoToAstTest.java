package vmodel_tests.component_level_tests;

import ast.ASTUtils;
import org.junit.jupiter.api.Test;
import preprocessing.BabyCobolParserUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class GoToAstTest {

    @Test
    void goToStatementShouldAppearInAst() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. MAIN.

                PROCEDURE DIVISION.
                GO TO FINISH.
                FINISH.
                STOP.
                """;

        ASTUtils.ASTResult result =
                ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));

        String ast = result.root.printTree();

        assertTrue(ast.contains("GoToStmt: FINISH"),
                "GO TO statement should be preserved in AST, got:\n" + ast);
    }

    @Test
    void goToInsideLoopShouldAppearInAst() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. MAIN.

                PROCEDURE DIVISION.
                LOOP
                    GO TO DONE
                END.
                DONE.
                STOP.
                """;

        ASTUtils.ASTResult result =
                ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));

        String ast = result.root.printTree();

        assertTrue(ast.contains("LoopStmt"),
                "Loop should be preserved in AST, got:\n" + ast);
        assertTrue(ast.contains("GoToStmt: DONE"),
                "GO TO inside loop should be preserved in AST, got:\n" + ast);
    }
}
