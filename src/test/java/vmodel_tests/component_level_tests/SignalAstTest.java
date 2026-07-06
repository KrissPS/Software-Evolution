package vmodel_tests.component_level_tests;

import ast.ASTUtils;
import org.junit.jupiter.api.Test;
import preprocessing.BabyCobolParserUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SignalAstTest {

    @Test
    void signalHandlerStatementShouldAppearInAst() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. MAIN.

                PROCEDURE DIVISION.
                SIGNAL HANDLER ON ERROR.

                HANDLER.
                STOP.
                """;

        ASTUtils.ASTResult result =
                ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));

        String ast = result.root.printTree();

        assertTrue(ast.contains("SignalStmt: HANDLER"),
                "SIGNAL handler should be preserved in AST, got:\n" + ast);
    }

    @Test
    void signalOffStatementShouldAppearInAst() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. MAIN.

                PROCEDURE DIVISION.
                SIGNAL OFF ON ERROR.
                STOP.
                """;

        ASTUtils.ASTResult result =
                ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));

        String ast = result.root.printTree();

        assertTrue(ast.contains("SignalOffStmt"),
                "SIGNAL OFF should be preserved in AST, got:\n" + ast);
    }

    @Test
    void signalOffIsAmbiguousWhenOffParagraphExists() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. MAIN.

                PROCEDURE DIVISION.
                SIGNAL OFF ON ERROR.

                OFF.
                STOP.
                """;

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code))
        );

        assertTrue(exception.getMessage().contains("SIGNAL OFF"),
                "Ambiguous SIGNAL OFF error should explain the problem, got: " + exception.getMessage());
    }
}
