package vmodel_tests.system_and_acceptance_tests;

import ast.ASTUtils;
import interpreter.BabyCobolInterpreter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import preprocessing.BabyCobolParserUtils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class EndToEndCompilerTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private PrintStream originalOut;

    @BeforeEach
    public void setUp() {
        originalOut = System.out;
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void testSystemEndToEnd_E2E() throws Exception {
        String path = "src/test/java/vmodel_tests/system_and_acceptance_tests/resources/system_e2e_test.babycob";
        String source = Files.readString(Paths.get(path));
        
        String processedSource = BabyCobolParserUtils.preprocess(source);
        ASTUtils.ASTResult astResult = ASTUtils.buildASTAndSymbolTable(processedSource);
        
        BabyCobolInterpreter interpreter = new BabyCobolInterpreter(astResult.symbolTable);
        interpreter.execute(astResult.root);
        
        String stdout = outContent.toString();
        
        // Assert output contains what we expect from black-box behavior
        assertTrue(stdout.contains("E2E START"));
        assertTrue(stdout.contains("MSG IS: STDOUT"));
        assertTrue(stdout.contains("IF THEN BLOCK"));
        assertTrue(stdout.contains("LOOP IDX: 1.0"));
        assertTrue(stdout.contains("LOOP IDX: 2.0"));
        assertTrue(stdout.contains("LOOP IDX: 3.0"));
        assertTrue(stdout.contains("EVALUATE MATCH"));
        assertTrue(stdout.contains("IN SECTION1"));
    }

    @Test
    void testSystemEndToEnd_Math() throws Exception {
        String path = "src/test/java/vmodel_tests/system_and_acceptance_tests/resources/system_math_test.babycob";
        String source = Files.readString(Paths.get(path));
        
        String processedSource = BabyCobolParserUtils.preprocess(source);
        ASTUtils.ASTResult astResult = ASTUtils.buildASTAndSymbolTable(processedSource);
        
        BabyCobolInterpreter interpreter = new BabyCobolInterpreter(astResult.symbolTable);
        interpreter.execute(astResult.root);
        
        String stdout = outContent.toString();
        
        // Assert mathematical results
        assertTrue(stdout.contains("B AFTER ADD: 15.0"));
        assertTrue(stdout.contains("C AFTER MULTIPLY: 20.0"));
        assertTrue(stdout.contains("A AFTER SUBTRACT: 1.0"));
    }

}
