package interpreter;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import java.util.Scanner;

import preprocessing.BabyCobolParserUtils;
import preprocessing.NextSentenceException;
import preprocessing.StopProgramException;
import ast.*;

public class BabyCobolInterpreter {
    private SymbolTable symbolTable;
    private Map<String, Object> memory;

    // to store and order paragraphs for PERFORM/THROUGH
    private Map<String, ASTNode> paragraphs;
    private java.util.List<String> paragraphNames;

    public Map<String, Object> getMemory() {
        return memory;
    }

    public BabyCobolInterpreter(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.memory = new HashMap<>();

        this.paragraphs = new java.util.LinkedHashMap<>(); // maintains insertion order
        this.paragraphNames = new java.util.ArrayList<>();

        initializeMemory();
    }

    /**
     * initializes the runtime memory from the symbol table
     * numeric pictures (containing 9) default to 0.0
     * alphabetic pictures default to "" (spaces)
     * records (no picture) are not initialized themselves but their children are
     * OCCURS arrays are initialized as Object[] of the appropriate size
     */
    private void initializeMemory() {
        for (Symbol symbol : symbolTable.getAllSymbols()) {
            String pic = symbol.getPicture();

            if (pic != null && !pic.isEmpty()) {
                // this is an elementary field
                if (symbol.getOccurs() > 0) {
                    // OCCURS array-- creates an array of initialized values
                    Object[] array = new Object[symbol.getOccurs()];
                    for (int i = 0; i < symbol.getOccurs(); i++) {
                        array[i] = defaultValueForPicture(pic);
                    }
                    memory.put(symbol.getName().toLowerCase(), array);
                } else {
                    memory.put(symbol.getName().toLowerCase(), defaultValueForPicture(pic));
                }
            } else if (symbol.getOccurs() > 0) {
                // group record with OCCURS, initialize as placeholder
                Object[] array = new Object[symbol.getOccurs()];
                memory.put(symbol.getName().toLowerCase(), array);
            }
            // records without OCCURS are not stored directly, their children are initialized
        }
    }

    /**
     * determines the default value for a given PICTURE string
     * numeric pictures (containing '9') default to 0.0
     * all others (X, A, Z, S, V combinations) default to empty string (space-filled)
     */
    private Object defaultValueForPicture(String pic) {
        if (pic != null && (pic.contains("9") || pic.contains("9"))) {
            return 0.0;
        }
        return "";
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
                try {
                    executeProcedure(child);
                } catch (StopProgramException e) {
                    return;
                }
            }
        }
    }

    private void executeProcedure(ASTNode procedureNode) {
        // 1) pre-pass: indexing all paragraphs so they can be found by PERFORM
        for (ASTNode child : procedureNode.getChildren()) {
            if (child.getType().equals("Paragraph")) {
                String paraName = child.getText().toLowerCase();
                paragraphs.put(paraName, child);
                paragraphNames.add(paraName);
            }
        }

        // 2) execution: running top level sentences and fall through paragraphs
        for (ASTNode child : procedureNode.getChildren()) {
            if (child.getType().equals("Sentence")) {
                try {
                    for (ASTNode statement : child.getChildren()) {
                        executeStatement(statement);
                    }
                } catch (NextSentenceException e) {
                    // NEXT SENTENCE skips to the next sentence
                }
            } else if (child.getType().equals("Paragraph")) {
                // apparently in COBOL execution falls through into paragraphs 
                // unless stopped by a GO TO or STOP RUN
                executeParagraph(child);
            }
        }
    }

    private void executeParagraph(ASTNode paragraphNode) {
        for (ASTNode sentence : paragraphNode.getChildren()) {
            if (sentence.getType().equals("Sentence")) {
                try {
                    for (ASTNode statement : sentence.getChildren()) {
                        executeStatement(statement);
                    }
                } catch (NextSentenceException e) {
                    // NEXT SENTENCE skips to the next sentence
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
                executeLoop(statement);
                break;
            case "EvaluateStmt":
                executeEvaluate(statement); // not complete
                break;
            case "PerformStmt":
                executePerform(statement); // maybe complete?
                break;
            case "NextSentenceStmt":
                throw new NextSentenceException();
            case "StopStmt":
                throw new StopProgramException();
            case "CallStmt":
                executeCall(statement);
                break;
            default:
                System.err.println("Unimplemented statement type: " + statement.getType());
        }
    }

    // --- statement implementations ---

    private void executeCall(ASTNode node) {
        String programName = node.getText();
        if (!node.getChildren().isEmpty()) {
            throw new RuntimeException("CALL USING is not implemented yet: " + programName);
        }

        String resourcePath = "/examples/" + programName.toLowerCase(Locale.ROOT) + ".babycob";
        try {
            String source = BabyCobolParserUtils.readResource(resourcePath);
            String processed = BabyCobolParserUtils.preprocess(source);
            ASTUtils.ASTResult ast = ASTUtils.buildASTAndSymbolTable(processed);
            new BabyCobolInterpreter(ast.symbolTable).execute(ast.root);
        } catch (RuntimeException e) {
            throw new RuntimeException("CALL failed for program " + programName + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("CALL failed for program " + programName + ": " + e.getMessage(), e);
        }
    }

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
                String varName = target.getText().toLowerCase();

                // if the target is an OCCURS array, assign value to all elements
                if (memory.containsKey(varName) && memory.get(varName) instanceof Object[]) {
                    Object[] array = (Object[]) memory.get(varName);
                    // if the source is also an array, copy element-wise. otherwise broadcast
                    if (value instanceof Object[]) {
                        Object[] srcArray = (Object[]) value;
                        for (int j = 0; j < Math.min(array.length, srcArray.length); j++) {
                            array[j] = srcArray[j];
                        }
                    } else {
                        for (int j = 0; j < array.length; j++) {
                            array[j] = coerceValue(value, array[j]);
                        }
                    }
                } else {
                    // coerce value to match the target's type if possible
                    if (memory.containsKey(varName)) {
                        value = coerceValue(value, memory.get(varName));
                    }
                    memory.put(varName, value);
                }
            }
        }
    }

    /**
     * coerce a source value to match the type/format of an existing value
     * if existing is Double, try to parse source as double
     * if existing is String, convert source to string
     */
    private Object coerceValue(Object source, Object existing) {
        if (existing instanceof Double) {
            if (source instanceof Double) return source;
            if (source instanceof String) {
                try {
                    return Double.parseDouble((String) source);
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            }
            return 0.0;
        }
        if (existing instanceof String) {
            if (source instanceof String) return source;
            return String.valueOf(source);
        }
        return source;
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
        /*
        TODO:
        Statements of with shortened form like so still cant run
        EVALUATE X
            WHEN 10 OR 20
                DISPLAY "EVALUATE CONTRACTED VALUES"

        */
        
        java.util.List<Object> evaluateSubjects = new java.util.ArrayList<>();
        
        // 1) gather the main EVALUATE subject
        evaluateSubjects.add(evaluateGenericExpression(node.getChildren().get(0)));
        
        // 2) gather any ALSO subjects
        for (int i = 1; i < node.getChildren().size(); i++) {
            ASTNode child = node.getChildren().get(i);
            if (child.getType().equals("AlsoClause")) {
                evaluateSubjects.add(evaluateGenericExpression(child.getChildren().get(0)));
            }
        }
        
        // 3) iterate through WHEN clauses
        for (int i = 1; i < node.getChildren().size(); i++) {
            ASTNode child = node.getChildren().get(i);
            
            if (child.getType().equals("WhenClauseStatement")) {
                ASTNode whenClause = child.getChildren().get(0);
                ASTNode whenSubjectNode = whenClause.getChildren().get(0); // either 'OTHER' or 'WhenSubject'
                
                boolean isMatch = false;
                
                if (whenSubjectNode.getType().equals("OTHER")) {
                    isMatch = true;
                } else {
                    // it is a WhenSubject that has SubWhenSubject nodes
                    isMatch = true; 
                    java.util.List<ASTNode> subSubjects = whenSubjectNode.getChildren();
                    
                    if (subSubjects.size() != evaluateSubjects.size()) {
                        throw new RuntimeException("EVALUATE subjects count does not match WHEN subjects count");
                    }
                    
                    // 4) compare each evaluate subject to its corresponding SubWhenSubject (ALSO matching)
                    for (int k = 0; k < evaluateSubjects.size(); k++) {
                        Object subjectValue = evaluateSubjects.get(k);
                        ASTNode subWhenNode = subSubjects.get(k);
                        
                        boolean subMatch = false;
                        
                        // special handling if the subject is "TRUE" (Condition mode)
                        if (subjectValue instanceof Boolean && (Boolean) subjectValue) {
                            subMatch = evaluateCondition(subWhenNode);
                        } else {
                            // value comparison (exact match or THROUGH range)
                            if (subWhenNode.getChildren().size() == 1) {
                                Object whenVal = evaluateGenericExpression(subWhenNode.getChildren().get(0));
                                if (compareValues(subjectValue, whenVal) == 0) {
                                    subMatch = true;
                                }
                            } else if (subWhenNode.getChildren().size() == 2) { // THROUGH range
                                Object rangeStart = evaluateGenericExpression(subWhenNode.getChildren().get(0));
                                Object rangeEnd = evaluateGenericExpression(subWhenNode.getChildren().get(1));
                                
                                if (compareValues(subjectValue, rangeStart) >= 0 && compareValues(subjectValue, rangeEnd) <= 0) {
                                    subMatch = true;
                                }
                            }
                        }
                        
                        // ALSO clauses act as an AND. if one fails then the whole WHEN clause fails
                        if (!subMatch) {
                            isMatch = false;
                            break; 
                        }
                    }
                }

                // 5) execute matching statements and break (first match wins)
                if (isMatch) {
                    for (int j = 1; j < child.getChildren().size(); j++) {
                        executeStatement(child.getChildren().get(j));
                    }
                    break; 
                }
            }
        }
    }

    private void executePerform(ASTNode node) {
        String targetId = node.getChildren().get(0).getText().toLowerCase();
        String throughId = null;
        int times = 1;

        // parse THROUGH and TIMES clauses
        for (ASTNode child : node.getChildren()) {
            if (child.getType().equals("TimesClause")) {
                ASTNode atomicNode = child.getChildren().get(0);
                times = (int) Double.parseDouble(evaluateAtomic(atomicNode).toString());
            } else if (child.getType().equals("ThroughClause")) {
                throughId = child.getText().toLowerCase();
            }
        }

        // verify the target paragraph exists
        if (!paragraphs.containsKey(targetId)) {
            throw new RuntimeException("PERFORM error: Paragraph '" + targetId + "' does not exist.");
        }
        if (throughId != null && !paragraphs.containsKey(throughId)) {
            throw new RuntimeException("PERFORM error: THROUGH paragraph '" + throughId + "' does not exist.");
        }

        // determine the execution range
        int startIndex = paragraphNames.indexOf(targetId);
        int endIndex = throughId != null ? paragraphNames.indexOf(throughId) : startIndex;

        if (startIndex > endIndex) {
            throw new RuntimeException("PERFORM error: THROUGH paragraph '" + throughId + "' is defined before '" + targetId + "'.");
        }

        // execute the paragraphs the requested number of times
        for (int i = 0; i < times; i++) {
            for (int p = startIndex; p <= endIndex; p++) {
                String paraToExecute = paragraphNames.get(p);
                executeParagraph(paragraphs.get(paraToExecute));
            }
        }
    }

    // --- Helper methods for Evaluate ---

    private Object evaluateGenericExpression(ASTNode exprNode) {
        // if the expression has relational operators evaluate it as a boolean condition
        if (hasRelationalNode(exprNode)) {
            return evaluateCondition(exprNode);
        }
        // otherwise go downwards and find atomic value
        return resolveAtomicValue(exprNode);
    }

    private boolean hasRelationalNode(ASTNode node) {
        if (node.getType().equals("RelationalOp") || node.getType().equals("LogicalOp")) {
            return true;
        }
        for (ASTNode child : node.getChildren()) {
            if (hasRelationalNode(child)) return true;
        }
        return false;
    }

    private Object resolveAtomicValue(ASTNode node) {
        // manually intercepting 'TRUE' so evaluateAtomic does not throw Variable not initialized error
        if (node.getType().equals("AtomicID") && node.getText().equalsIgnoreCase("true")) {
            return true;
        }
        if (node.getType().startsWith("Atomic")) {
            return evaluateAtomic(node);
        }
        if (!node.getChildren().isEmpty()) {
            return resolveAtomicValue(node.getChildren().get(0));
        }
        return null;
    }

    private int compareValues(Object a, Object b) {
        // if both are numbers compare mathematically
        if (a instanceof Double && b instanceof Double) {
            return Double.compare((Double) a, (Double) b);
        }
        // otherwise fallback is string comparison
        return String.valueOf(a).compareTo(String.valueOf(b));
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
                Object val = memory.get(varName);
                // if it's an array (OCCURS), return the first element as representative
                // for display/expression purposes
                if (val instanceof Object[]) {
                    Object[] arr = (Object[]) val;
                    if (arr.length > 0) {
                        return arr[0];
                    }
                    return 0.0;
                }
                return val;
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
        // for a simple RelationalExpression evaluation:
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
                case "<>": return left != right;
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
        if (exprNode.getType().startsWith("Atomic")) {
            return Double.parseDouble(evaluateAtomic(exprNode).toString());
        }
        
        // recursively unpack AdditiveExpression, MultiplicativeExpression, etc
        if (exprNode.getChildren().size() == 1) {
            return evaluateMathExpression(exprNode.getChildren().get(0));
        }
        
        return 0.0; // placeholder for deeper mathematical expression recursion
    }
}
