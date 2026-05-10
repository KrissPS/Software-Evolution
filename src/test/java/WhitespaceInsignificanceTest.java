import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WhitespaceInsignificanceTest {

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
