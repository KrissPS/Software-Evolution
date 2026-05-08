import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class WhitespaceInsignificanceTest {

    @Test
    void testWhitespaceParsing() throws Exception {
        String code = BabyCobolParserUtils.readResource("/examples/test_whitespace.babycob");
        
        String tree = BabyCobolParserUtils.parseTree(code);

        assertNotNull(tree, "Parse tree should not be null");
    }
}
