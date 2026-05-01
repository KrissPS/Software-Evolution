import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import java.io.InputStream;

public class Main {
    public static void main(String[] args) throws Exception {

        InputStream is = Main.class.getResourceAsStream("/examples/test.babycob");

        if (is == null) {
            throw new RuntimeException("File not found.");
        }

        CharStream input = CharStreams.fromStream(is);

        BabyCobolLexer lexer = new BabyCobolLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        BabyCobolParser parser = new BabyCobolParser(tokens);

        ParseTree tree = parser.program();

        System.out.println("<== PARSE TREE ==>");
        System.out.println(tree.toStringTree(parser));
    }
}