package vmodel_tests.component_level_tests;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.Test;
import parser.BabyCobolLexer;
import preprocessing.BabyCobolParserUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FigurativeConstantGrammarLexerTest {

    @Test
    void figurativeConstantsShouldUseDedicatedLexerTokens() {
        List<String> tokenNames = tokenNamesFor("SPACES HIGH-VALUES LOW-VALUES");

        assertEquals(
                List.of("SPACES", "HIGH_VALUES", "LOW_VALUES"),
                tokenNames
        );
    }

    @Test
    void parsesMoveSpacesToMultipleTargets() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. MAIN.

                DATA DIVISION.
                01 TEXT-FIELD PICTURE IS X(5).
                01 NUM-FIELD PICTURE IS 9(3).

                PROCEDURE DIVISION.
                MOVE SPACES TO TEXT-FIELD NUM-FIELD.
                """;

        assertDoesNotThrow(() -> BabyCobolParserUtils.parseTree(code));
    }

    @Test
    void parsesMoveHighValuesAndLowValues() {
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

        assertDoesNotThrow(() -> BabyCobolParserUtils.parseTree(code));
    }

    @Test
    void rejectsFigurativeConstantsAsArithmeticArguments() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. MAIN.

                DATA DIVISION.
                01 TARGET-FIELD PICTURE IS 9(3).

                PROCEDURE DIVISION.
                ADD SPACES TO TARGET-FIELD.
                """;

        assertThrows(RuntimeException.class, () -> BabyCobolParserUtils.parseTree(code));
    }

    private List<String> tokenNamesFor(String source) {
        BabyCobolLexer lexer = new BabyCobolLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();

        return tokens.getTokens().stream()
                .filter(token -> token.getType() != Token.EOF)
                .map(token -> BabyCobolLexer.VOCABULARY.getSymbolicName(token.getType()))
                .toList();
    }
}
