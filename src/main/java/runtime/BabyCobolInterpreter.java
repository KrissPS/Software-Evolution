package runtime;

import parser.BabyCobolParser;
import preprocessing.NextSentenceException;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class BabyCobolInterpreter {

    private final Memory memory = new Memory();
    private final Scanner scanner = new Scanner(System.in);
    private final Map<String, BabyCobolParser.ParagraphContext> paragraphs = new HashMap<>();

    public void execute(BabyCobolParser.ProgramContext program) {
        if (program == null) return;

        if (program.data() != null) {
            loadDataDivision(program.data());
        }

        BabyCobolParser.ProcedureContext procedure = program.procedure();
        if (procedure == null) return;

        loadParagraphs(procedure);

        try {
            for (BabyCobolParser.SentenceContext sentence : procedure.sentence()) {
                executeSentence(sentence);
            }

            for (BabyCobolParser.ParagraphContext paragraph : procedure.paragraph()) {
                executeParagraph(paragraph);
            }

        } catch (GoToException e) {
            executeParagraphByName(e.target());
        }
    }

    private void loadParagraphs(BabyCobolParser.ProcedureContext procedure) {
        paragraphs.clear();

        for (BabyCobolParser.ParagraphContext paragraph : procedure.paragraph()) {
            String name = paragraph.ID().getText().toUpperCase();
            paragraphs.put(name, paragraph);
        }
    }

    private void executeParagraph(BabyCobolParser.ParagraphContext paragraph) {
        for (BabyCobolParser.SentenceContext sentence : paragraph.sentence()) {
            executeSentence(sentence);
        }
    }

    private void executeParagraphByName(String name) {
        BabyCobolParser.ParagraphContext paragraph = paragraphs.get(name.toUpperCase());

        if (paragraph == null) {
            throw new RuntimeException("Unknown paragraph: " + name);
        }

        executeParagraph(paragraph);
    }

    private void loadDataDivision(BabyCobolParser.DataContext data) {
        for (BabyCobolParser.DataEntryContext entry : data.dataEntry()) {
            String name = entry.ID().getText();

            for (BabyCobolParser.DataClauseContext clause : entry.dataClause()) {
                if (clause.pictureClause() != null) {
                    String picture = clause.pictureClause().getChild(2).getText();
                    memory.declare(name, picture);
                } else if (clause.likeClause() != null) {
                    String likedField = clause.likeClause().ID().getText();
                    memory.declareLike(name, likedField);
                }
            }
        }
    }

    private void executeSentence(BabyCobolParser.SentenceContext sentence) {
        try {
            for (BabyCobolParser.StatementContext stmt : sentence.statement()) {
                executeStatement(stmt);
            }
        } catch (NextSentenceException e) {
            return;
        }
    }

    private void executeStatement(BabyCobolParser.StatementContext stmt) {
        if (stmt.displayStmt() != null) {
            handleDisplay(stmt.displayStmt());
        } else if (stmt.acceptStmt() != null) {
            handleAccept(stmt.acceptStmt());
        } else if (stmt.moveStmt() != null) {
            handleMove(stmt.moveStmt());
        } else if (stmt.ifStmt() != null) {
            handleIf(stmt.ifStmt());
        } else if (stmt.goToStmt() != null) {
            handleGoTo(stmt.goToStmt());
        } else if (stmt.nextSentenceStmt() != null) {
            throw new NextSentenceException();
        }
    }

    private void handleGoTo(BabyCobolParser.GoToStmtContext ctx) {
        String target = ctx.ID().getText().toUpperCase();

        // Normal GO TO: GO TO PARAGRAPH-NAME
        if (paragraphs.containsKey(target)) {
            throw new GoToException(target);
        }

        // Computable GO TO: GO TO FIELD-NAME
        if (memory.exists(target)) {
            String runtimeTarget = memory.getValue(target).toUpperCase();

            if (!paragraphs.containsKey(runtimeTarget)) {
                throw new RuntimeException(
                        "GO TO runtime target does not exist: " + runtimeTarget
                );
            }

            throw new GoToException(runtimeTarget);
        }

        throw new RuntimeException("Unknown GO TO target or field: " + target);
    }

    private void handleDisplay(BabyCobolParser.DisplayStmtContext ctx) {
        StringBuilder output = new StringBuilder();

        for (BabyCobolParser.AtomicContext atom : ctx.atomic()) {
            output.append(resolveAtomic(atom));
        }

        System.out.println(output);
    }

    private void handleAccept(BabyCobolParser.AcceptStmtContext ctx) {
        for (var id : ctx.ID()) {
            String input = scanner.nextLine();
            memory.set(id.getText(), input);
        }
    }

    private void handleMove(BabyCobolParser.MoveStmtContext ctx) {
        String value = resolveAtomic(ctx.atomic());

        for (var id : ctx.ID()) {
            memory.set(id.getText(), value);
        }
    }

    private void handleIf(BabyCobolParser.IfStmtContext ctx) {
        for (BabyCobolParser.StatementContext stmt : ctx.statement()) {
            executeStatement(stmt);
        }
    }

    private String resolveAtomic(BabyCobolParser.AtomicContext atom) {
        if (atom.STRING() != null) {
            String text = atom.STRING().getText();
            return text.substring(1, text.length() - 1);
        }

        if (atom.DECIMAL() != null) {
            return atom.DECIMAL().getText();
        }

        if (atom.INT() != null) {
            return atom.INT().getText();
        }

        if (atom.ID() != null) {
            return memory.getValue(atom.ID().getText());
        }

        throw new RuntimeException("Unknown atomic value: " + atom.getText());
    }
}