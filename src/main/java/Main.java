import preprocessing.BabyCobolParserUtils;

import ast.ASTNode;
import ast.ASTUtils;

public class Main {
    public static void main(String[] args) throws Exception {

        String source = BabyCobolParserUtils.readResource("/examples/test_insensitive.babycob");

        String processedSource = BabyCobolParserUtils.preprocess(source);

        String tree = BabyCobolParserUtils.parseTree(processedSource);
        
        // building the AST using the visitor
        ASTUtils.ASTResult astResult = ASTUtils.buildASTAndSymbolTable(processedSource);

        System.out.println("<== PROCESSED CODE ==>");
        System.out.println(processedSource);

        System.out.println("\n<== PARSE TREE ==>");
        System.out.println(tree);
        
        System.out.println("\n<== ABSTRACT SYNTAX TREE ==>");
        System.out.println(astResult.root.printTree());
        
        System.out.println("\n<== SYMBOL TABLE ==>");
        System.out.println(astResult.symbolTable);
    }
}
