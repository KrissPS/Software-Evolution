import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import preprocessing.CaseInsensitive;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class TestCaseInsensitivity {

    public static void main(String[] args) throws Exception {

        String normalSource = readResource("/examples/test.babycob");
        String insensitiveSource = readResource("/examples/test_insensitive.babycob");

        String normalTree = parse(normalSource);
        String insensitiveTree = parse(CaseInsensitive.process(insensitiveSource));

        System.out.println("<== NORMAL PARSE TREE ==>");
        System.out.println(normalTree);

        System.out.println("\n<== CASE INSENSITIVE PARSE TREE ==>");
        System.out.println(insensitiveTree);

        System.out.println("\n<== COMPARISON ==>");
        if (normalTree.equals(insensitiveTree)) {
            System.out.println("OK: both parse trees are equal.");
        } else {
            System.out.println("ERROR: parse trees are different.");
        }
    }

    private static String readResource(String path) throws Exception {
        InputStream is = TestCaseInsensitivity.class.getResourceAsStream(path);

        if (is == null) {
            throw new RuntimeException("File not found: " + path);
        }

        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String parse(String source) {
        CharStream input = CharStreams.fromString(source);

        BabyCobolLexer lexer = new BabyCobolLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        BabyCobolParser parser = new BabyCobolParser(tokens);

        ParseTree tree = parser.program();

        return tree.toStringTree(parser);
    }
}