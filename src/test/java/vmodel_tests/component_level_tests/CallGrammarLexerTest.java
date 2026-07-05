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

public class CallGrammarLexerTest {

    @Test
    void callAndUsingShouldBeDedicatedLexerTokens() {
        List<String> tokenNames = tokenNamesFor("CALL HELLO USING FIELD-A FIELD-B");

        assertEquals(
                List.of("CALL", "ID", "USING", "ID", "ID"),
                tokenNames
        );
    }

    @Test
    void parsesCallWithoutUsingArguments() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. MAIN.

                PROCEDURE DIVISION.
                CALL HELLO.
                """;

        assertDoesNotThrow(() -> BabyCobolParserUtils.parseTree(code));
    }

    @Test
    void parsesCallWithUsingIdentifiers() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. MAIN.

                DATA DIVISION.
                01 FIELD-A PICTURE IS 9.
                01 FIELD-B PICTURE IS X.

                PROCEDURE DIVISION.
                CALL HELLO USING FIELD-A FIELD-B.
                """;

        assertDoesNotThrow(() -> BabyCobolParserUtils.parseTree(code));
    }

    @Test
    void parsesProcedureDivisionUsingIdentifiers() {
        String code = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. HELLO.

                DATA DIVISION.
                01 FIELD-A PICTURE IS 9.
                01 FIELD-B PICTURE IS X.

                PROCEDURE DIVISION USING FIELD-A FIELD-B.
                DISPLAY FIELD-A.
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
