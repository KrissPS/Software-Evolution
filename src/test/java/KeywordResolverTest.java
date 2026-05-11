import org.junit.jupiter.api.Test;

import preprocessing.BabyCobolParserUtils;
import preprocessing.KeywordResolver;

import static org.junit.jupiter.api.Assertions.*;

public class KeywordResolverTest {

    @Test
    void testUnambiguousSingleLowercaseKeyword() throws Exception {
        String code = BabyCobolParserUtils.readResource("/examples/keyword_unambiguous_single.babycob");
        String preprocessed = BabyCobolParserUtils.preprocess(code);
        
        // Should auto-fix the single lowercase "to" keyword
        String resolved = KeywordResolver.resolve(preprocessed);
        
        assertNotNull(resolved, "Resolved code should not be null");
        
        // Verify the resolved code parses successfully
        String tree = BabyCobolParserUtils.parseTree(resolved);
        assertNotNull(tree, "Parse tree should not be null");
    }

    @Test
    void testUnambiguousMultipleLowercaseKeywords() throws Exception {
        String code = BabyCobolParserUtils.readResource("/examples/keyword_unambiguous_multiple.babycob");
        String preprocessed = BabyCobolParserUtils.preprocess(code);
        
        // Should auto-fix the multiple lowercase keywords
        String resolved = KeywordResolver.resolve(preprocessed);
        
        assertNotNull(resolved, "Resolved code should not be null");
        
        // Verify the resolved code parses successfully
        String tree = BabyCobolParserUtils.parseTree(resolved);
        assertNotNull(tree, "Parse tree should not be null");
    }

    @Test
    void testAmbiguousKeywordResolution() throws Exception {
        String code = BabyCobolParserUtils.readResource("/examples/keyword_ambiguous.babycob");
        String preprocessed = BabyCobolParserUtils.preprocess(code);
        
        // Should successfully resolve the code
        String resolved = KeywordResolver.resolve(preprocessed);
        
        assertNotNull(resolved, "Resolved code should not be null");
        
        // Verify the resolved code parses successfully
        String tree = BabyCobolParserUtils.parseTree(resolved);
        assertNotNull(tree, "Parse tree should not be null");
    }
}
