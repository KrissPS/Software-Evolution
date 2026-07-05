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

public class GoToGrammarLexerTest {

    @Test
    void goToShouldUseDedicatedLexerTokens() {
        List<String> tokenNames = tokenNamesFor("GO TO FINISH");

        assertEquals(
                List.of("GO", "TO", "ID"),
                tokenNames
        );
    }

    @Test
    void parsesGoToStatement() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. MAIN.

                PROCEDURE DIVISION.
                GO TO FINISH.
                FINISH.
                STOP.
                """;

        assertDoesNotThrow(() -> BabyCobolParserUtils.parseTree(code));
    }

    @Test
    void parsesGoToInsideLoop() {
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

        assertDoesNotThrow(() -> BabyCobolParserUtils.parseTree(code));
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
