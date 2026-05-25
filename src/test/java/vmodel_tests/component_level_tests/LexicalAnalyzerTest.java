package vmodel_tests.component_level_tests;

import org.antlr.v4.runtime.*;
import org.junit.jupiter.api.Test;
import parser.BabyCobolLexer;
import preprocessing.BabyCobolParserUtils;
import preprocessing.CaseInsensitive;
import preprocessing.KeywordResolver;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LexicalAnalyzerTest {

    @Test
    void testValidTokens() {
        String code = "DISPLAY \"Hello\" ";
        CharStream input = CharStreams.fromString(code);
        BabyCobolLexer lexer = new BabyCobolLexer(input);
        
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();
        
        List<Token> tokenList = tokens.getTokens();
        assertFalse(tokenList.isEmpty(), "Should generate tokens");
        
        boolean hasDisplay = tokenList.stream().anyMatch(t -> t.getType() == BabyCobolLexer.DISPLAY);
        boolean hasString = tokenList.stream().anyMatch(t -> t.getType() == BabyCobolLexer.STRING);
        
        assertTrue(hasDisplay, "Should contain DISPLAY token");
        assertTrue(hasString, "Should contain STRING token");
    }

    @Test
    void testLexicalErrorInvalidCharacter() {
        // Using an invalid character that is not defined in the lexer rules
        String code = "DISPLAY @ INVALID ";
        CharStream input = CharStreams.fromString(code);
        BabyCobolLexer lexer = new BabyCobolLexer(input);
        
        List<String> errors = new ArrayList<>();
        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                errors.add(msg);
            }
        });

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();

        assertFalse(errors.isEmpty(), "Lexer should flag lexical error for invalid character");
    }

    @Test
    void compareParseTrees() throws Exception {
        String normal = BabyCobolParserUtils.readResource("/examples/normal.babycob");
        String insensitive = BabyCobolParserUtils.readResource("/examples/insensitive.babycob");
        String normalTree = BabyCobolParserUtils.parseTree(normal);
        String insensitiveTree = BabyCobolParserUtils.parseTree(CaseInsensitive.process(insensitive));
        assertEquals(normalTree, insensitiveTree);
    }

    @Test
    void testUnambiguousSingleLowercaseKeyword() throws Exception {
        String code = BabyCobolParserUtils.readResource("/examples/keyword_unambiguous_single.babycob");
        String preprocessed = BabyCobolParserUtils.preprocess(code);
        String resolved = KeywordResolver.resolve(preprocessed);
        assertNotNull(resolved, "Resolved code should not be null");
        String tree = BabyCobolParserUtils.parseTree(resolved);
        assertNotNull(tree, "Parse tree should not be null");
    }

    @Test
    void testUnambiguousMultipleLowercaseKeywords() throws Exception {
        String code = BabyCobolParserUtils.readResource("/examples/keyword_unambiguous_multiple.babycob");
        String preprocessed = BabyCobolParserUtils.preprocess(code);
        String resolved = KeywordResolver.resolve(preprocessed);
        assertNotNull(resolved, "Resolved code should not be null");
        String tree = BabyCobolParserUtils.parseTree(resolved);
        assertNotNull(tree, "Parse tree should not be null");
    }

    @Test
    void testAmbiguousKeywordResolution() throws Exception {
        String code = BabyCobolParserUtils.readResource("/examples/keyword_ambiguous.babycob");
        String preprocessed = BabyCobolParserUtils.preprocess(code);
        String resolved = KeywordResolver.resolve(preprocessed);
        assertNotNull(resolved, "Resolved code should not be null");
        String tree = BabyCobolParserUtils.parseTree(resolved);
        assertNotNull(tree, "Parse tree should not be null");
    }

    @Test
    void testWhitespaceParsing() throws Exception {
        String code = BabyCobolParserUtils.readResource("/examples/test_whitespace.babycob");
        String tree = BabyCobolParserUtils.parseTree(code);
        assertNotNull(tree, "Parse tree should not be null");
    }

    @Test
    void testWhitespaceParsingFailure() throws Exception {
        String code = BabyCobolParserUtils.readResource("/examples/test_whitespace_invalid.babycob");
        assertThrows(RuntimeException.class, () -> {
            BabyCobolParserUtils.parseTree(code);
        }, "Throws syntax error due to invalid spacing/split words");
    }
}
