package vmodel_tests.component_level_tests;

import ast.ASTUtils;
import org.junit.jupiter.api.Test;
import preprocessing.BabyCobolParserUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AlterAstTest {

    @Test
    void alterStatementShouldAppearInAst() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. MAIN.

                PROCEDURE DIVISION.
                ALTER ROUTE TO PROCEED TO TARGET.

                ROUTE.
                GO TO ORIGINAL.

                ORIGINAL.
                STOP.

                TARGET.
                STOP.
                """;

        ASTUtils.ASTResult result =
                ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));

        String ast = result.root.printTree();

        assertTrue(ast.contains("AlterStmt: ROUTE"),
                "ALTER statement should preserve altered paragraph, got:\n" + ast);
        assertTrue(ast.contains("AlterTarget: TARGET"),
                "ALTER statement should preserve new target, got:\n" + ast);
    }
}
