package ast;

import org.antlr.v4.runtime.*;
import parser.BabyCobolLexer;
import parser.BabyCobolParser;

public class ASTUtils {

    public static class ASTResult {
        public final ASTNode root;
        public final SymbolTable symbolTable;
        
        public ASTResult(ASTNode root, SymbolTable symbolTable) {
            this.root = root;
            this.symbolTable = symbolTable;
        }
    }

    public static ASTResult buildASTAndSymbolTable(String source) {
        CharStream charStream = CharStreams.fromString(source);
        BabyCobolLexer lexer = new BabyCobolLexer(charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        BabyCobolParser parser = new BabyCobolParser(tokens);
        
        parser.removeErrorListeners();
        BabyCobolParser.ProgramContext programCtx = parser.program();
        
        BuildASTVisitor visitor = new BuildASTVisitor();
        ASTNode root = visitor.visit(programCtx);
        return new ASTResult(root, visitor.getSymbolTable());
    }

    public static ASTNode buildAST(String source) {
        return buildASTAndSymbolTable(source).root;
    }
}
