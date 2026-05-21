package preprocessing;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import parser.BabyCobolLexer;
import parser.BabyCobolParser;

public class BabyCobolParserUtils {

    public static String readResource(String path) throws Exception {
        InputStream is = BabyCobolParserUtils.class.getResourceAsStream(path);

        if (is == null) {
            throw new RuntimeException("File not found: " + path);
        }

        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    public static String preprocess(String source) {
        String processed = source;

        if (isFixedFormat(source)) {
            processed = CodeCleaner.cleanCode(processed);
        }

        return CaseInsensitive.process(processed);
    }

    private static boolean isFixedFormat(String source) {
        boolean hasFixed = false;
        boolean hasFree = false;

        for (String line : source.lines().collect(Collectors.toList())) {
            if (line.isBlank()) {
                continue;
            }

            if (line.matches("^\\d{6}([ *-].*)?$")) {
                hasFixed = true;
            } else {
                hasFree = true;
            }
        }

        if (hasFixed && hasFree) {
            throw new IllegalArgumentException(
                    "Mixed fixed-format and free-format lines are not allowed."
            );
        }

        return hasFixed;
    }

    public static BabyCobolParser.ProgramContext parseProgram(String source) {
        CharStream input = CharStreams.fromString(source);

        BabyCobolLexer lexer = new BabyCobolLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        BabyCobolParser parser = new BabyCobolParser(tokens);
        
        // setting error listener to fail fast if syntax errors are present
        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(
                Recognizer<?, ?> recognizer, 
                Object offendingSymbol, int line, 
                int charPositionInLine, 
                String msg, RecognitionException e) 
            {
                throw new RuntimeException("Syntax Error at line " + line + ":" + charPositionInLine + " - " + msg);
            }
        });

        return parser.program();
    }

    public static String parseTree(String source) {

        CharStream input = CharStreams.fromString(source);

        BabyCobolLexer lexer = new BabyCobolLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        BabyCobolParser parser = new BabyCobolParser(tokens);
        
        // setting error listener to fail fast if syntax errors are present
        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(
                Recognizer<?, ?> recognizer, 
                Object offendingSymbol, int line, 
                int charPositionInLine, 
                String msg, RecognitionException e) 
            {
                throw new RuntimeException("Syntax Error at line " + line + ":" + charPositionInLine + " - " + msg);
            }
        });

        return parser.program().toStringTree(parser);
    }
}
