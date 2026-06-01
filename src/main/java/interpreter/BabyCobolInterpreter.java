package interpreter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Scanner;

import ast.*;

public class BabyCobolInterpreter {
    private SymbolTable symbolTable;
    private Map<String, Object> memory;

    public BabyCobolInterpreter(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.memory = new HashMap<>();
        initializeMemory();
    }

    /**
     * initializes the runtime memory the symbol table
     * numeric pictures default to 0, alphanumeric default to ""
     */
    private void initializeMemory() {
        for (Symbol symbol : symbolTable.getSymbols().values()) {
            String pic = symbol.getPicture();
            if (pic != null && (pic.contains("9"))) {
                memory.put(symbol.getName().toLowerCase(), 0.0);
            } else {
                memory.put(symbol.getName().toLowerCase(), "");
            }
        }
    }

    /**
     * entry point for execution, it finds procedure division and runs it
     */
    public void execute(ASTNode programNode) {
        if (!programNode.getType().equals("Program")) {
            throw new IllegalArgumentException("Expected Program root node");
        }

        for (ASTNode child : programNode.getChildren()) {
            if (child.getType().equals("Procedure")) {
                executeProcedure(child);
            }
        }
    }

    private void executeProcedure(ASTNode procedureNode) {
        for (ASTNode sentence : procedureNode.getChildren()) {
            if (sentence.getType().equals("Sentence")) {
                for (ASTNode statement : sentence.getChildren()) {
                    executeStatement(statement);
                }
            }
        }
    }

    private void executeStatement(ASTNode statement) {
        switch (statement.getType()) {
            case "DisplayStmt":
                executeDisplay(statement);
                break;
            case "AcceptStmt":
                executeAccept(statement);
                break;
            case "MoveStmt":
                executeMove(statement);
                break;
            case "IfStmt":
                executeIf(statement);
                break;
            case "AddStmt":
                executeMath(statement, "+");
                break;
            case "SubtractStmt":
                executeMath(statement, "-");
                break;
            case "MulStmt":
                executeMath(statement, "*");
                break;
            case "DivideStmt":
                executeMath(statement, "/");
                break;
            case "LoopStmt":
                executeLoop(statement); // not complete
                break;
            case "EvaluateStmt":
                executeEvaluate(statement); // not complete
                break;
            case "PerformStmt":
                executePerform(statement); // not complete
                break;
            case "StopStmt":
                System.exit(0);
                break;
            default:
                System.err.println("Unimplemented statement type: " + statement.getType());
        }
    }

    // --- statement implementations ---

    private void executeDisplay(ASTNode node) {
        boolean noAdvancing = false;
        StringBuilder output = new StringBuilder();

        for (ASTNode child : node.getChildren()) {
            if (child.getType().equals("NO ADVANCING")) {
                noAdvancing = true;
            } else {
                output.append(evaluateAtomic(child));
            }
        }

        if (noAdvancing) {
            System.out.print(output.toString());
        } else {
            System.out.println(output.toString());
        }
    }

    private void executeAccept(ASTNode node) {
        Scanner scanner = new Scanner(System.in);
        for (ASTNode child : node.getChildren()) {
            if (child.getType().equals("ID")) {
                String varName = child.getText().toLowerCase();
                if (memory.containsKey(varName)) {
                    String input = scanner.nextLine();
                    Object existingValue = memory.get(varName);
                    
                    // attempt to maintain numeric type if initialized as such
                    if (existingValue instanceof Double) {
                        try {
                            memory.put(varName, Double.parseDouble(input));
                        } catch (NumberFormatException e) {
                            memory.put(varName, 0.0);
                        }
                    } else {
                        memory.put(varName, input);
                    }
                } else {
                    throw new RuntimeException("Variable not initialized: " + varName);
                }
            }
        }
    }

    private void executeMove(ASTNode node) {
        Object value = evaluateAtomic(node.getChildren().get(0));
        
        // looping through all target IDs
        for (int i = 1; i < node.getChildren().size(); i++) {
            ASTNode target = node.getChildren().get(i);
            if (target.getType().equals("ToID")) {
                memory.put(target.getText().toLowerCase(), value);
            }
        }
    }

    private void executeIf(ASTNode node) {
        // child 0 is AnyExpression
        boolean condition = evaluateCondition(node.getChildren().get(0));
        
        if (condition) {
            // find "Then"
            for (ASTNode child : node.getChildren()) {
                if (child.getType().equals("Then")) {
                    for (ASTNode stmt : child.getChildren()) {
                        executeStatement(stmt);
                    }
                }
            }
        } else {
            // find "ElseStmt"
            for (ASTNode child : node.getChildren()) {
                if (child.getType().equals("ElseStmt")) {
                    for (ASTNode stmt : child.getChildren()) {
                        executeStatement(stmt);
                    }
                }
            }
        }
    }

    private void executeMath(ASTNode node, String op) {
        double result = op.equals("*") || op.equals("/") ? 1.0 : 0.0;
        String targetId = null;

        List<ASTNode> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            ASTNode child = children.get(i);
            if (child.getType().startsWith("Atomic")) {
                double val = Double.parseDouble(evaluateAtomic(child).toString());
                
                if (i == 0 && (op.equals("+") || op.equals("-"))) result = val;
                else if (i == 0 && (op.equals("*") || op.equals("/"))) result = val;
                else {
                    switch (op) {
                        case "+": result += val; break;
                        case "-": result -= val; break;
                        case "*": result *= val; break;
                        case "/": result /= val; break;
                    }
                }
            } else if (child.getType().equals("GivingClause")) {
                targetId = child.getChildren().get(0).getText().toLowerCase(); // first ID in giving
            }
        }

        // if no giving clause assign to the last operand
        if (targetId != null) {
            memory.put(targetId, result);
        } else {
             // fallback: assign to the last atomic ID found
             // (we are assuming it is in the same statement because we have filtered out for semantic/logical issues
             // when building ast)
             ASTNode lastAtomic = children.get(children.size() - 1);
             if (lastAtomic.getType().equals("AtomicID")) {
                 memory.put(lastAtomic.getText().toLowerCase(), result);
             }
        }
    }

    private void executeLoop(ASTNode node) {
        ASTNode whileCondition = null;
        ASTNode untilCondition = null;
        java.util.List<ASTNode> statements = new java.util.ArrayList<>();

        // vars holding VARYING config
        String varyingVar = null;
        Double toValue = null;
        Double byValue = 1.0; // default step is 1 if BY is omitted
        boolean isVarying = false;

        for (ASTNode child : node.getChildren()) {
            if (child.getType().equals("WhileClause")) {
                whileCondition = child.getChildren().get(0); 
            } else if (child.getType().equals("UntilClause")) {
                untilCondition = child.getChildren().get(0); 
            } else if (child.getType().equals("VaryingClause")) {
                isVarying = true;
                varyingVar = child.getChildren().get(0).getText().toLowerCase();
                
                // parse FROM, TO, BY
                for (int i = 1; i < child.getChildren().size(); i++) {
                    ASTNode varyingPart = child.getChildren().get(i);
                    double val = Double.parseDouble(evaluateAtomic(varyingPart.getChildren().get(0)).toString());
                    
                    if (varyingPart.getType().equals("From")) {
                        memory.put(varyingVar, val); // init loop variable
                    } else if (varyingPart.getType().equals("To")) {
                        toValue = val;
                    } else if (varyingPart.getType().equals("By")) {
                        byValue = val;
                    }
                }
            } else {
                statements.add(child);
            }
        }

        while (true) {
            // 1) check VARYING 'TO' boundary condition before execution
            if (isVarying && toValue != null) {
                double currentVal = (Double) memory.get(varyingVar);
                // breaks if we exceed the limit (handles both positive and negative steps)
                if ((byValue > 0 && currentVal > toValue) || (byValue < 0 && currentVal < toValue)) {
                    break;
                }
            }

            // 2) check WHILE and UNTIL conds
            if (whileCondition != null && !evaluateCondition(whileCondition)) {
                break;
            }
            if (untilCondition != null && evaluateCondition(untilCondition)) {
                break;
            }

            // 3) execute loop stmts
            for (ASTNode stmt : statements) {
                executeStatement(stmt);
            }

            // 4) increment the VARYING var
            if (isVarying) {
                double currentVal = (Double) memory.get(varyingVar);
                memory.put(varyingVar, currentVal + byValue);
            }

            // 5) prevent infinite loops if no conditions are defined
            if (whileCondition == null && untilCondition == null && !isVarying) {
                break;
            }
        }
    }

    private void executeEvaluate(ASTNode node) {
        // TODO: complete the logic here
        // node.getChildren().get(0) is the Evaluate subject (AnyExpression). 
        // In a full implementation, you evaluate the subject and compare it 
        // to the When subjects. Here, we resolve conditions directly.
        /*
        so basically right now it evaluates this
        EVALUATE TRUE
            WHEN age = 18
                DISPLAY "You are an adult".
        
        and not yet:
        EVALUATE age
            WHEN 18 
                DISPLAY "You are an adult".
            WHEN 21 THROUGH 65
                DISPLAY "You are working age".

        and:
        EVALUATE age ALSO income
            WHEN 18 ALSO 0
                DISPLAY "Broke student".

        */
        
        for (int i = 1; i < node.getChildren().size(); i++) {
            ASTNode child = node.getChildren().get(i);
            
            if (child.getType().equals("WhenClauseStatement")) {
                ASTNode whenClause = child.getChildren().get(0);
                ASTNode whenSubject = whenClause.getChildren().get(0);
                
                boolean match = false;
                if (whenSubject.getType().equals("OTHER")) {
                    match = true;
                } else {
                    // fallback to basic condition evaluation for the when block
                    match = evaluateCondition(whenSubject);
                }

                if (match) {
                    for (int j = 1; j < child.getChildren().size(); j++) {
                        executeStatement(child.getChildren().get(j));
                    }
                    break; // EVALUATE exits after the first true WHEN
                }
            }
        }
    }


    private void executePerform(ASTNode node) {
        // TODO: Perform statement isnt implemented fully in the AST (see comments in for loop below)
        // TODO: First check if the paragraph being called in the perform even exists
        String targetId = node.getChildren().get(0).getText().toLowerCase();
        int times = 1;

        for (ASTNode child : node.getChildren()) {
            if (child.getType().equals("TimesClause")) {
                ASTNode atomicNode = child.getChildren().get(0);
                times = (int) Double.parseDouble(evaluateAtomic(atomicNode).toString());
            }
        }

        for (int i = 0; i < times; i++) {
            // Note: The BuildASTVisitor does not visit Paragraphs inside 
            // ProcedureContext (ie visitProcedure does not have any logic for paragraph from syntax)
            // so this is a placeholder print until Paragraph ast nodes are added to the ast.
            System.err.println("[Interpreter Warning] PERFORM requested for paragraph: " 
                + targetId + " (Requires Paragraph traversal in BuildASTVisitor)");
        }
    }

    // --- Expression and Value Evaluation ---

    private Object evaluateAtomic(ASTNode node) {
        // recall that BuildASTVisitor creates AST nodes like "AtomicID", "AtomicInt", "AtomicString"
        switch (node.getType()) {
            case "AtomicID":
                String varName = node.getText().toLowerCase();
                if (!memory.containsKey(varName)) {
                    throw new RuntimeException("Variable not initialized: " + varName);
                }
                return memory.get(varName);
            case "AtomicInt":
                return Double.parseDouble(node.getText());
            case "AtomicString":
                // remove enclosing quotes
                String text = node.getText();
                return text.substring(1, text.length() - 1); 
            default:
                // handle unwrapped atomic nodes if any
                if (node.getChildren().size() > 0) {
                    return evaluateAtomic(node.getChildren().get(0));
                }
                return node.getText();
        }
    }

    private boolean evaluateCondition(ASTNode conditionNode) {
        // TODO: Not complete. this is a simplified condition evaluator. 
        // for a full implementation we must recursively traverse:
        // AnyExpression -> LogicalExpression -> RelationalExpression -> AdditiveExpression
        
        // eg for a simple RelationalExpression evaluation:
        if (conditionNode.getType().equals("RelationalExpression")) {
            double left = evaluateMathExpression(conditionNode.getChildren().get(0));
            String op = conditionNode.getChildren().get(1).getText(); // RelationalOp
            double right = evaluateMathExpression(conditionNode.getChildren().get(2));

            switch (op) {
                case "=": return left == right;
                case "<": return left < right;
                case ">": return left > right;
                case "<=": return left <= right;
                case ">=": return left >= right;
                default: throw new RuntimeException("Unknown relational operator: " + op);
            }
        }

        // Drill down to the actual relational or boolean node
        if (!conditionNode.getChildren().isEmpty()) {
            return evaluateCondition(conditionNode.getChildren().get(0));
        }

        return false; 
    }

    private double evaluateMathExpression(ASTNode exprNode) {
        // TODO: not complete
        if (exprNode.getType().startsWith("Atomic")) {
            return Double.parseDouble(evaluateAtomic(exprNode).toString());
        }
        
        // Recursively unpack AdditiveExpression, MultiplicativeExpression, etc.
        if (exprNode.getChildren().size() == 1) {
            return evaluateMathExpression(exprNode.getChildren().get(0));
        }
        
        return 0.0; // Placeholder for deeper mathematical expression recursion
    }
}