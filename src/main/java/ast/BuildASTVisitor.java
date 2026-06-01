package ast;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import parser.BabyCobolParserBaseVisitor;
import parser.BabyCobolParser;

public class BuildASTVisitor extends BabyCobolParserBaseVisitor<ASTNode> {

    private SymbolTable symbolTable = new SymbolTable();

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    @Override
    public ASTNode visitProgram(BabyCobolParser.ProgramContext ctx) {
        ASTNode node = new ASTNode("Program");
        if (ctx.identification() != null) {
            node.addChild(visit(ctx.identification()));
        }
        if (ctx.data() != null) {
            node.addChild(visit(ctx.data()));
        }
        if (ctx.procedure() != null) {
            node.addChild(visit(ctx.procedure()));
        }
        return node;
    }

    @Override
    public ASTNode visitIdentification(BabyCobolParser.IdentificationContext ctx) {
        ASTNode node = new ASTNode("Identification");
        for (TerminalNode id : ctx.ID()) {
            node.addChild(new ASTNode("ID", id.getText()));
        }
        return node;
    }

    @Override
    public ASTNode visitData(BabyCobolParser.DataContext ctx) {
        ASTNode node = new ASTNode("Data");
        for (BabyCobolParser.DataEntryContext entry : ctx.dataEntry()) {
            node.addChild(visit(entry));
        }
        return node;
    }

    @Override
    public ASTNode visitDataEntry(BabyCobolParser.DataEntryContext ctx) {
        ASTNode node = new ASTNode("DataEntry");

        int level = Integer.parseInt(ctx.INT().getText());
        String id = ctx.ID().getText();

        node.addChild(new ASTNode("Level", String.valueOf(level)));
        node.addChild(new ASTNode("ID", id));

        String picture = "";
        String like = "";
        int occurs = 0;

        for (BabyCobolParser.DataClauseContext clause : ctx.dataClause()) {
            if (clause.pictureClause() != null) {
                picture = clause.pictureClause().pictureValue().getText();
            } else if (clause.likeClause() != null) {
                like = clause.likeClause().ID().getText();
            } else if (clause.occursClause() != null) {
                occurs = Integer.parseInt(clause.occursClause().INT().getText());
            }

            node.addChild(visit(clause));
        }

        symbolTable.addSymbol(new Symbol(id, level, picture, like, occurs));

        return node;
    }

    @Override
    public ASTNode visitDataClause(BabyCobolParser.DataClauseContext ctx) {
        ASTNode node = new ASTNode("DataClause");
        if (ctx.pictureClause() != null) {
            node.addChild(visit(ctx.pictureClause()));
        }
        if (ctx.likeClause() != null) {
            node.addChild(visit(ctx.likeClause()));
        }
        if (ctx.occursClause() != null) {
            node.addChild(visit(ctx.occursClause()));
        }
        return node;
    }

    @Override
    public ASTNode visitPictureClause(BabyCobolParser.PictureClauseContext ctx) {
        ASTNode node = new ASTNode("PICTURE_CLAUSE");

        String pictureValue = ctx.pictureValue().getText();

        node.addChild(new ASTNode("PICTURE_VALUE", pictureValue));

        return node;
    }

    @Override
    public ASTNode visitLikeClause(BabyCobolParser.LikeClauseContext ctx) {
        ASTNode node = new ASTNode("LikeClause");
        node.addChild(new ASTNode("ID", ctx.ID().getText()));
        return node;
    }

    @Override
    public ASTNode visitOccursClause(BabyCobolParser.OccursClauseContext ctx) {
        ASTNode node = new ASTNode("OccursClause", ctx.INT().getText());
        return node;
    }

    @Override
    public ASTNode visitProcedure(BabyCobolParser.ProcedureContext ctx) {
        ASTNode node = new ASTNode("Procedure");
        for (BabyCobolParser.SentenceContext sentence : ctx.sentence()) {
            node.addChild(visit(sentence));
        }
        return node;
    }

    @Override
    public ASTNode visitSentence(BabyCobolParser.SentenceContext ctx) {
        ASTNode node = new ASTNode("Sentence");
        for (BabyCobolParser.StatementContext stmt : ctx.statement()) {
            node.addChild(visit(stmt));
        }
        return node;
    }

    @Override
    public ASTNode visitStatement(BabyCobolParser.StatementContext ctx) {
        if (ctx.getChild(0) instanceof ParseTree) {
            return visit(ctx.getChild(0));
        }
        return null; // should not happen (is an error)
    }

    @Override
    public ASTNode visitDisplayStmt(BabyCobolParser.DisplayStmtContext ctx) {
        ASTNode node = new ASTNode("DisplayStmt");
        for (BabyCobolParser.AtomicContext atomic : ctx.atomic()) {
            node.addChild(visit(atomic));
        }
        if (ctx.ADVANCING() != null) {
            node.addChild(new ASTNode("NO ADVANCING"));
        }
        return node;
    }
    
    @Override
    public ASTNode visitAcceptStmt(BabyCobolParser.AcceptStmtContext ctx) {
        ASTNode node = new ASTNode("AcceptStmt");
        for (TerminalNode id : ctx.ID()) {
            node.addChild(new ASTNode("ID", id.getText()));
        }
        return node;
    }

    @Override
    public ASTNode visitAddStmt(BabyCobolParser.AddStmtContext ctx) {
        ASTNode node = new ASTNode("AddStmt");
        for (BabyCobolParser.AtomicContext atomic : ctx.atomic()) {
            node.addChild(visit(atomic));
        }
        
        BabyCobolParser.AtomicContext toAtomic = ctx.atomic(ctx.atomic().size() - 1);
        if ((toAtomic.INT() != null || toAtomic.STRING() != null) && ctx.givingClause() == null) {
            throw new IllegalArgumentException("If the second argument of ADD is a literal, the GIVING clause is mandatory.");
        }
        
        if (ctx.givingClause() != null) {
            node.addChild(visit(ctx.givingClause()));
        }
        return node;
    }

    @Override
    public ASTNode visitDivideStmt(BabyCobolParser.DivideStmtContext ctx) {
        ASTNode node = new ASTNode("DivideStmt");
        for (BabyCobolParser.AtomicContext atomic : ctx.atomic()) {
            node.addChild(visit(atomic));
        }
        
        for (int i = 1; i < ctx.atomic().size(); i++) {
            BabyCobolParser.AtomicContext secondArg = ctx.atomic(i);
            if ((secondArg.INT() != null || secondArg.STRING() != null) && ctx.givingRemainderClause() == null) {
                throw new IllegalArgumentException("If the second argument is a literal, the third argument is mandatory.");
            }
        }

        if (ctx.givingRemainderClause() != null) {
            if (ctx.atomic().size() > 2) {
                throw new IllegalArgumentException("If the third argument is present, there can be only one second argument.");
            }
            if (ctx.givingRemainderClause().remainderClause() != null && ctx.givingRemainderClause().ID().size() > 1) {
                throw new IllegalArgumentException("If the fourth argument is present, there can be only one third argument.");
            }
            node.addChild(visit(ctx.givingRemainderClause()));
        }
        return node;
    }

    @Override
    public ASTNode visitMulStmt(BabyCobolParser.MulStmtContext ctx) {
        ASTNode node = new ASTNode("MulStmt");
        for (BabyCobolParser.AtomicContext atomic : ctx.atomic()) {
            node.addChild(visit(atomic));
        }

        BabyCobolParser.AtomicContext toAtomic = ctx.atomic(ctx.atomic().size() - 1);
        if ((toAtomic.INT() != null || toAtomic.STRING() != null) && ctx.givingClause() == null) {
            throw new IllegalArgumentException("If the second argument of MULTIPLY is a literal, the GIVING clause is mandatory.");
        }

        if (ctx.givingClause() != null) {
            node.addChild(visit(ctx.givingClause()));
        }
        return node;
    }

    @Override
    public ASTNode visitIfStmt(BabyCobolParser.IfStmtContext ctx) {
        ASTNode node = new ASTNode("IfStmt");
        node.addChild(visit(ctx.anyExpression()));
        
        ASTNode thenNode = new ASTNode("Then");
        for (BabyCobolParser.StatementContext stmt : ctx.statement()) {
            thenNode.addChild(visit(stmt));
        }
        node.addChild(thenNode);
        
        if (ctx.elseStmt() != null) {
            node.addChild(visit(ctx.elseStmt()));
        }
        return node;
    }

    @Override
    public ASTNode visitElseStmt(BabyCobolParser.ElseStmtContext ctx) {
        ASTNode node = new ASTNode("ElseStmt");
        for (BabyCobolParser.StatementContext stmt : ctx.statement()) {
            node.addChild(visit(stmt));
        }
        return node;
    }

    @Override
    public ASTNode visitMoveStmt(BabyCobolParser.MoveStmtContext ctx) {
        ASTNode node = new ASTNode("MoveStmt");
        node.addChild(visit(ctx.atomic()));
        for (TerminalNode id : ctx.ID()) {
            node.addChild(new ASTNode("ToID", id.getText()));
        }
        return node;
    }

    @Override
    public ASTNode visitPerformStmt(BabyCobolParser.PerformStmtContext ctx) {
        ASTNode node = new ASTNode("PerformStmt");
        node.addChild(new ASTNode("ID", ctx.ID().getText()));
        if (ctx.throughClause() != null) {
            node.addChild(visit(ctx.throughClause()));
        }
        if (ctx.timesClause() != null) {
            node.addChild(visit(ctx.timesClause()));
        }
        return node;
    }

    @Override
    public ASTNode visitThroughClause(BabyCobolParser.ThroughClauseContext ctx) {
        return new ASTNode("ThroughClause", ctx.ID().getText());
    }

    @Override
    public ASTNode visitTimesClause(BabyCobolParser.TimesClauseContext ctx) {
        ASTNode node = new ASTNode("TimesClause");
        node.addChild(visit(ctx.atomic()));
        return node;
    }

    @Override
    public ASTNode visitEvaluateStmt(BabyCobolParser.EvaluateStmtContext ctx) {
        ASTNode node = new ASTNode("EvaluateStmt");
        node.addChild(visit(ctx.anyExpression()));
        for (BabyCobolParser.AlsoClauseContext also : ctx.alsoClause()) {
            node.addChild(visit(also));
        }
        for (BabyCobolParser.WhenClauseStatementContext when : ctx.whenClauseStatement()) {
            node.addChild(visit(when));
        }
        return node;
    }

    @Override
    public ASTNode visitAlsoClause(BabyCobolParser.AlsoClauseContext ctx) {
        ASTNode node = new ASTNode("AlsoClause");
        node.addChild(visit(ctx.anyExpression()));
        return node;
    }

    @Override
    public ASTNode visitWhenClauseStatement(BabyCobolParser.WhenClauseStatementContext ctx) {
        ASTNode node = new ASTNode("WhenClauseStatement");
        node.addChild(visit(ctx.whenClause()));
        for (BabyCobolParser.StatementContext stmt : ctx.statement()) {
            node.addChild(visit(stmt));
        }
        return node;
    }

    @Override
    public ASTNode visitWhenClause(BabyCobolParser.WhenClauseContext ctx) {
        ASTNode node = new ASTNode("WhenClause");
        if (ctx.OTHER() != null) {
            node.addChild(new ASTNode("OTHER"));
        } else if (ctx.whenSubject() != null) {
            node.addChild(visit(ctx.whenSubject()));
        }
        return node;
    }

    @Override
    public ASTNode visitWhenSubject(BabyCobolParser.WhenSubjectContext ctx) {
        ASTNode node = new ASTNode("WhenSubject");
        for (BabyCobolParser.SubWhenSubjectContext sub : ctx.subWhenSubject()) {
            node.addChild(visit(sub));
        }
        return node;
    }

    @Override
    public ASTNode visitSubWhenSubject(BabyCobolParser.SubWhenSubjectContext ctx) {
        ASTNode node = new ASTNode("SubWhenSubject");
        for (BabyCobolParser.AnyExpressionContext expr : ctx.anyExpression()) {
            node.addChild(visit(expr));
        }
        return node;
    }

    @Override
    public ASTNode visitAnyExpression(BabyCobolParser.AnyExpressionContext ctx) {
        ASTNode node = new ASTNode("AnyExpression");
        node.addChild(visit(ctx.logicalExpression()));
        return node;
    }

    @Override
    public ASTNode visitLogicalExpression(BabyCobolParser.LogicalExpressionContext ctx) {
        ASTNode node = new ASTNode("LogicalExpression");
        node.addChild(visit(ctx.relationalExpression()));
        if (ctx.contractedRelationalExpression() != null && !ctx.contractedRelationalExpression().isEmpty()) {
            for (int i = 0; i < ctx.contractedRelationalExpression().size(); i++) {
                String op = ctx.getChild(2 * i + 1).getText();
                node.addChild(new ASTNode("LogicalOp", op));
                node.addChild(visit(ctx.contractedRelationalExpression(i)));
            }
        }
        return node;
    }
    
    @Override
    public ASTNode visitContractedRelationalExpression(BabyCobolParser.ContractedRelationalExpressionContext ctx) {
        ASTNode node = new ASTNode("ContractedRelationalExpression");
        if (ctx.relationalExpression() != null) {
            node.addChild(visit(ctx.relationalExpression()));
        } else if (ctx.relationalOperator() != null) {
            node.addChild(visit(ctx.relationalOperator()));
            node.addChild(visit(ctx.additiveExpression()));
        } else if (ctx.additiveExpression() != null) {
            node.addChild(visit(ctx.additiveExpression()));
        }
        return node;
    }
    
    @Override
    public ASTNode visitRelationalExpression(BabyCobolParser.RelationalExpressionContext ctx) {
        ASTNode node = new ASTNode("RelationalExpression");
        node.addChild(visit(ctx.additiveExpression(0)));
        for (int i = 1; i < ctx.additiveExpression().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            node.addChild(new ASTNode("RelationalOp", op));
            node.addChild(visit(ctx.additiveExpression(i)));
        }
        return node;
    }

    @Override
    public ASTNode visitAdditiveExpression(BabyCobolParser.AdditiveExpressionContext ctx) {
        ASTNode node = new ASTNode("AdditiveExpression");
        node.addChild(visit(ctx.multiplicativeExpression(0)));
        for (int i = 1; i < ctx.multiplicativeExpression().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            node.addChild(new ASTNode("AdditiveOp", op));
            node.addChild(visit(ctx.multiplicativeExpression(i)));
        }
        return node;
    }

    @Override
    public ASTNode visitMultiplicativeExpression(BabyCobolParser.MultiplicativeExpressionContext ctx) {
        ASTNode node = new ASTNode("MultiplicativeExpression");
        node.addChild(visit(ctx.unaryExpression(0)));
        for (int i = 1; i < ctx.unaryExpression().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            node.addChild(new ASTNode("MultiplicativeOp", op));
            node.addChild(visit(ctx.unaryExpression(i)));
        }
        return node;
    }

    @Override
    public ASTNode visitUnaryExpression(BabyCobolParser.UnaryExpressionContext ctx) {
        ASTNode node = new ASTNode("UnaryExpression");
        if (ctx.PLUS() != null || ctx.MINUS() != null || ctx.NOT() != null) {
            node.addChild(new ASTNode("UnaryOp", ctx.getChild(0).getText()));
        }
        node.addChild(visit(ctx.primaryExpression()));
        return node;
    }

    @Override
    public ASTNode visitPrimaryExpression(BabyCobolParser.PrimaryExpressionContext ctx) {
        ASTNode node = new ASTNode("PrimaryExpression");
        if (ctx.atomic() != null) {
            node.addChild(visit(ctx.atomic()));
        } else if (ctx.anyExpression() != null) {
            node.addChild(visit(ctx.anyExpression()));
        }
        return node;
    }

    @Override
    public ASTNode visitStopStmt(BabyCobolParser.StopStmtContext ctx) {
        return new ASTNode("StopStmt");
    }

    @Override
    public ASTNode visitSubtractStmt(BabyCobolParser.SubtractStmtContext ctx) {
        ASTNode node = new ASTNode("SubtractStmt");
        for (BabyCobolParser.AtomicContext atomic : ctx.atomic()) {
            node.addChild(visit(atomic)); // this collects both FROM and terms.
        }

        BabyCobolParser.AtomicContext toAtomic = ctx.atomic(ctx.atomic().size() - 1);
        if ((toAtomic.INT() != null || toAtomic.STRING() != null) && ctx.givingClause() == null) {
            throw new IllegalArgumentException("If the second argument of SUBTRACT is a literal, the GIVING clause is mandatory.");
        }

        if (ctx.givingClause() != null) {
            node.addChild(visit(ctx.givingClause()));
        }
        return node;
    }

    @Override
    public ASTNode visitGivingClause(BabyCobolParser.GivingClauseContext ctx) {
        ASTNode node = new ASTNode("GivingClause");
        for (TerminalNode id : ctx.ID()) {
            node.addChild(new ASTNode("ID", id.getText()));
        }
        return node;
    }

    @Override
    public ASTNode visitGivingRemainderClause(BabyCobolParser.GivingRemainderClauseContext ctx) {
        ASTNode node = new ASTNode("GivingRemainderClause");
        for (TerminalNode id : ctx.ID()) {
            node.addChild(new ASTNode("ID", id.getText()));
        }
        if (ctx.remainderClause() != null) {
            node.addChild(visit(ctx.remainderClause()));
        }
        return node;
    }

    @Override
    public ASTNode visitRemainderClause(BabyCobolParser.RemainderClauseContext ctx) {
        return new ASTNode("RemainderClause", ctx.ID().getText());
    }

    @Override
    public ASTNode visitLoopStmt(BabyCobolParser.LoopStmtContext ctx) {
        ASTNode node = new ASTNode("LoopStmt");
        for (BabyCobolParser.LoopElementContext el : ctx.loopElement()) {
            node.addChild(visit(el));
        }
        return node;
    }

    @Override
    public ASTNode visitLoopElement(BabyCobolParser.LoopElementContext ctx) {
        if (ctx.getChild(0) instanceof ParseTree) {
            return visit(ctx.getChild(0));
        }
        return null;
    }

    @Override
    public ASTNode visitVaryingClause(BabyCobolParser.VaryingClauseContext ctx) {
        ASTNode node = new ASTNode("VaryingClause");
        node.addChild(new ASTNode("ID", ctx.ID().getText()));
        
        // track position in atomic() since some clauses can be omitted
        int atomicIndex = 0;
        
        if (ctx.FROM() != null) {
            ASTNode fromNode = new ASTNode("From");
            fromNode.addChild(visit(ctx.atomic(atomicIndex++)));
            node.addChild(fromNode);
        }
        
        if (ctx.TO() != null) {
            ASTNode toNode = new ASTNode("To");
            toNode.addChild(visit(ctx.atomic(atomicIndex++)));
            node.addChild(toNode);
        }
        
        if (ctx.BY() != null) {
            ASTNode byNode = new ASTNode("By");
            byNode.addChild(visit(ctx.atomic(atomicIndex++)));
            node.addChild(byNode);
        }
        
        return node;
    }

    @Override
    public ASTNode visitWhileClause(BabyCobolParser.WhileClauseContext ctx) {
        ASTNode node = new ASTNode("WhileClause");
        node.addChild(visit(ctx.anyExpression()));
        return node;
    }

    @Override
    public ASTNode visitUntilClause(BabyCobolParser.UntilClauseContext ctx) {
        ASTNode node = new ASTNode("UntilClause");
        node.addChild(visit(ctx.anyExpression()));
        return node;
    }

    @Override
    public ASTNode visitAtomic(BabyCobolParser.AtomicContext ctx) {
        if (ctx.ID() != null) {
            return new ASTNode("AtomicID", ctx.ID().getText());
        } else if (ctx.INT() != null) {
            return new ASTNode("AtomicInt", ctx.INT().getText());
        } else if (ctx.STRING() != null) {
            return new ASTNode("AtomicString", ctx.STRING().getText());
        }
        return new ASTNode("Atomic");
    }

    @Override
    public ASTNode visitRelationalOperator(BabyCobolParser.RelationalOperatorContext ctx) {
        return new ASTNode("RelationalOperator", ctx.getText());
    }
}
