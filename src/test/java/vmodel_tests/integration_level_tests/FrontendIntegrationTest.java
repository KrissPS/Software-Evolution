package vmodel_tests.integration_level_tests;

import ast.ASTUtils;
import org.junit.jupiter.api.Test;
import preprocessing.BabyCobolParserUtils;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FrontendIntegrationTest {

    @Test
    void testFrontendIntegrationPipeline() throws Exception {
        // i will use this test case as a template for other integration level test cases
        // source code -> lexer -> parser -> AST -> Symbol Table
        
        // 1. source code
        String source = BabyCobolParserUtils.readResource("/integration_test.babycob");
        assertNotNull(source, "source code should be read successfully");

        // 2. preprocessing
        String processedSource = BabyCobolParserUtils.preprocess(source);
        assertNotNull(processedSource, "preprocessed source should not be null");

        // 3. parser tree (lexer -> parser)
        String tree = BabyCobolParserUtils.parseTree(processedSource);
        assertNotNull(tree, "oarse tree should be built without syntax errors");
        assertTrue(tree.contains("IDENTIFICATION"), "parse tree should contain root level divisions");

        // 4. AST and symbol table construction
        assertDoesNotThrow(() -> {
            ASTUtils.ASTResult astResult = ASTUtils.buildASTAndSymbolTable(processedSource);
            assertNotNull(astResult.root, "AST root should not be null");
            assertNotNull(astResult.symbolTable, "Symbol table should not be null");
            
            String printedAst = astResult.root.printTree();
            assertNotNull(printedAst, "Printed AST should not be null");
            assertTrue(printedAst.length() > 0, "AST should contain nodes");
        }, "AST and Symbol Table construction should not throw exceptions");
    }
}
