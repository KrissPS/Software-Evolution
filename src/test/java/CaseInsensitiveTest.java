import org.junit.jupiter.api.Test;
import preprocessing.CaseInsensitive;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CaseInsensitiveTest {

    @Test
    void compareParseTrees() throws Exception {

        String normal =
                BabyCobolParserUtils.readResource(
                        "/examples/normal.babycob"
                );

        String insensitive =
                BabyCobolParserUtils.readResource(
                        "/examples/insensitive.babycob"
                );

        String normalTree =
                BabyCobolParserUtils.parseTree(normal);

        String insensitiveTree =
                BabyCobolParserUtils.parseTree(
                        CaseInsensitive.process(insensitive)
                );

        assertEquals(normalTree, insensitiveTree);
    }
}