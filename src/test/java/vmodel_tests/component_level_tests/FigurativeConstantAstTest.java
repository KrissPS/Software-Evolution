package vmodel_tests.component_level_tests;

import ast.ASTUtils;
import org.junit.jupiter.api.Test;
import preprocessing.BabyCobolParserUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FigurativeConstantAstTest {

    @Test
    void moveSpacesShouldAppearAsFigurativeConstantInAst() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. MAIN.

                DATA DIVISION.
                01 TEXT-FIELD PICTURE IS X(5).
                01 NUM-FIELD PICTURE IS 9(3).

                PROCEDURE DIVISION.
                MOVE SPACES TO TEXT-FIELD NUM-FIELD.
                """;

        ASTUtils.ASTResult result =
                ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));

        String ast = result.root.printTree();

        assertTrue(ast.contains("AtomicFigurative: SPACES"),
                "MOVE SPACES should be represented as a figurative constant, got:\n" + ast);
        assertTrue(ast.contains("ToID: TEXT-FIELD"),
                "First MOVE target should be preserved in AST, got:\n" + ast);
        assertTrue(ast.contains("ToID: NUM-FIELD"),
                "Second MOVE target should be preserved in AST, got:\n" + ast);
    }

    @Test
    void moveHighValuesAndLowValuesShouldAppearAsFigurativeConstantsInAst() throws Exception {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. MAIN.

                DATA DIVISION.
                01 HIGH-FIELD PICTURE IS X(5).
                01 LOW-FIELD PICTURE IS 9(3).

                PROCEDURE DIVISION.
                MOVE HIGH-VALUES TO HIGH-FIELD.
                MOVE LOW-VALUES TO LOW-FIELD.
                """;

        ASTUtils.ASTResult result =
                ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(code));

        String ast = result.root.printTree();

        assertTrue(ast.contains("AtomicFigurative: HIGH-VALUES"),
                "MOVE HIGH-VALUES should be represented as a figurative constant, got:\n" + ast);
        assertTrue(ast.contains("AtomicFigurative: LOW-VALUES"),
                "MOVE LOW-VALUES should be represented as a figurative constant, got:\n" + ast);
    }
}
