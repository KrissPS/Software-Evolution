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

    @Test
    void testSubtractStatements() throws Exception {
        String code = BabyCobolParserUtils.readResource("/examples/parser_subtract_stmt.babycob");
        String tree = BabyCobolParserUtils.parseTree(BabyCobolParserUtils.preprocess(code));
        assertNotNull(tree);
    }

    @Test
    void testEvaluateStatements() throws Exception {
        String code = BabyCobolParserUtils.readResource("/examples/parser_evaluate_stmt.babycob");
        String tree = BabyCobolParserUtils.parseTree(BabyCobolParserUtils.preprocess(code));
        assertNotNull(tree);
    }

    @Test
    void testPerformStatements() throws Exception {
        String code = BabyCobolParserUtils.readResource("/examples/parser_perform_stmt.babycob");
        String tree = BabyCobolParserUtils.parseTree(BabyCobolParserUtils.preprocess(code));
        assertNotNull(tree);
    }

    @Test
    void testDataDivisionClauses() throws Exception {
        String code = BabyCobolParserUtils.readResource("/examples/parser_data_division.babycob");
        String tree = BabyCobolParserUtils.parseTree(BabyCobolParserUtils.preprocess(code));
        assertNotNull(tree);
    }
}
