import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class StatementParsingTest {

    @Test
    void testDisplayStatements() throws Exception {
        String code = BabyCobolParserUtils.readResource("/examples/parser_display_stmt.babycob");
        String tree = BabyCobolParserUtils.parseTree(BabyCobolParserUtils.preprocess(code));
        assertNotNull(tree);
    }

    @Test
    void testAcceptStatements() throws Exception {
        String code = BabyCobolParserUtils.readResource("/examples/parser_accept_stmt.babycob");
        String tree = BabyCobolParserUtils.parseTree(BabyCobolParserUtils.preprocess(code));
        assertNotNull(tree);
    }

    @Test
    void testArithmeticStatements() throws Exception {
        String code = BabyCobolParserUtils.readResource("/examples/parser_arithmetic_stmt.babycob");
        String tree = BabyCobolParserUtils.parseTree(BabyCobolParserUtils.preprocess(code));
        assertNotNull(tree);
    }
}