import preprocessing.BabyCobolParserUtils;
import interpreter.BabyCobolInterpreter;
import parser.BabyCobolParser;

import ast.ASTUtils;

public class Main {
    public static void main(String[] args) throws Exception {

        String source = BabyCobolParserUtils.readResource("/examples/test_signal.babycob");

        String processedSource = BabyCobolParserUtils.preprocess(source);

        String tree = BabyCobolParserUtils.parseTree(processedSource);

        ASTUtils.ASTResult astResult =
                ASTUtils.buildASTAndSymbolTable(processedSource);

        BabyCobolParser.ProgramContext program =
                BabyCobolParserUtils.parseProgram(processedSource);

        System.out.println("<== PROCESSED CODE ==>");
        System.out.println(processedSource);

        System.out.println("\n<== PARSE TREE ==>");
        System.out.println(tree);

        System.out.println("\n<== ABSTRACT SYNTAX TREE ==>");
        System.out.println(astResult.root.printTree());

        System.out.println("\n<== SYMBOL TABLE ==>");
        System.out.println(astResult.symbolTable);

        System.out.println("\n<== PROGRAM OUTPUT ==>");
        BabyCobolInterpreter interpreter = new BabyCobolInterpreter(astResult.symbolTable);
        interpreter.execute(astResult.root);

    }
}