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

public class SignalGrammarLexerTest {

    @Test
    void signalShouldUseDedicatedLexerTokens() {
        List<String> tokenNames = tokenNamesFor("SIGNAL HANDLER ON ERROR");

        assertEquals(
                List.of("SIGNAL", "ID", "ON", "ERROR"),
                tokenNames
        );
    }

    @Test
    void signalOffShouldUseDedicatedLexerTokens() {
        List<String> tokenNames = tokenNamesFor("SIGNAL OFF ON ERROR");

        assertEquals(
                List.of("SIGNAL", "OFF", "ON", "ERROR"),
                tokenNames
        );
    }

    @Test
    void parsesSignalHandlerStatement() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. MAIN.

                PROCEDURE DIVISION.
                SIGNAL HANDLER ON ERROR.

                HANDLER.
                STOP.
                """;

        assertDoesNotThrow(() -> BabyCobolParserUtils.parseTree(code));
    }

    @Test
    void parsesSignalOffStatement() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. MAIN.

                PROCEDURE DIVISION.
                SIGNAL OFF ON ERROR.
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
