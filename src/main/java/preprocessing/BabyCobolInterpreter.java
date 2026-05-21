package preprocessing;

import parser.BabyCobolParser;

/**
 * Minimal interpreter that executes BabyCobol parse trees.
 * Showcases NEXT SENTENCE control flow through exception-based mechanism.
 * 
 * Current capabilities:
 * - DISPLAY statements (prints to stdout)
 * - IF statements (executes THEN block)
 * - NEXT SENTENCE (throws exception caught at sentence level)
 * 
 * All other statements are ignored (stub behavior).
 */
public class BabyCobolInterpreter {

    /**
     * Execute a complete BabyCobol program.
     */
    public void execute(BabyCobolParser.ProgramContext program) {
        if (program == null) return;
        
        BabyCobolParser.ProcedureContext procedure = program.procedure();
        if (procedure == null) return;
        
        // Execute each sentence in the procedure
        for (BabyCobolParser.SentenceContext sentence : procedure.sentence()) {
            executeSentence(sentence);
        }
    }

    /**
     * Execute a single sentence.
     * Catches NextSentenceException to allow early exit to next sentence.
     */
    private void executeSentence(BabyCobolParser.SentenceContext sentence) {
        try {
            for (BabyCobolParser.StatementContext stmt : sentence.statement()) {
                executeStatement(stmt);
            }
        } catch (NextSentenceException e) {
            // Gracefully exit this sentence and move to the next one
            return;
        }
    }

    /**
     * Execute a single statement.
     */
    private void executeStatement(BabyCobolParser.StatementContext stmt) {
        if (stmt.displayStmt() != null) {
            handleDisplay(stmt.displayStmt());
        } else if (stmt.ifStmt() != null) {
            handleIf(stmt.ifStmt());
        } else if (stmt.nextSentenceStmt() != null) {
            throw new NextSentenceException();
        }
        // All other statements: stub (ignore)
    }

    /**
     * Handle DISPLAY statement: print atoms separated by spaces.
     */
    private void handleDisplay(BabyCobolParser.DisplayStmtContext ctx) {
        for (BabyCobolParser.AtomicContext atom : ctx.atomic()) {
            System.out.print(atom.getText() + " ");
        }
        System.out.println();
    }

    /**
     * Handle IF statement: execute THEN block.
     * (Simplified: no real expression evaluation, just executes statements)
     */
    private void handleIf(BabyCobolParser.IfStmtContext ctx) {
        // Execute the statements in the THEN block
        for (BabyCobolParser.StatementContext stmt : ctx.statement()) {
            executeStatement(stmt);
        }
        // Note: ELSE block not executed (stub behavior)
    }
}
