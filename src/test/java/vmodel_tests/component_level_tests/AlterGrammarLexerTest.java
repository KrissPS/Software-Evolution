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

public class AlterGrammarLexerTest {

    @Test
    void alterShouldUseDedicatedLexerTokens() {
        List<String> tokenNames = tokenNamesFor("ALTER SOURCE TO PROCEED TO TARGET");

        assertEquals(
                List.of("ALTER", "ID", "TO", "PROCEED", "TO", "ID"),
                tokenNames
        );
    }

    @Test
    void parsesAlterStatement() {
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
