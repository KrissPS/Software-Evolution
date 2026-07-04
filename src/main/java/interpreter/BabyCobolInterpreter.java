package interpreter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Scanner;

import preprocessing.BabyCobolParserUtils;
import preprocessing.NextSentenceException;
import preprocessing.GoToException;
import ast.*;
import preprocessing.StopProgramException;

public class BabyCobolInterpreter {
    private SymbolTable symbolTable;
    private Map<String, Object> memory;

    // to store and order paragraphs for PERFORM/THROUGH
    private Map<String, ASTNode> paragraphs;
    private java.util.List<String> paragraphNames;

    private Map<String, String> alteredTargets = new HashMap<>();

    private String currentParagraph;

    private String signalHandler = null;

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
                executeProcedure(child);
            }
        }
    }

    private void executeProcedure(ASTNode procedureNode) {
        // index paragraphs
        for (ASTNode child : procedureNode.getChildren()) {
            if (child.getType().equals("Paragraph")) {
                String paraName = child.getText().toLowerCase();
                paragraphs.put(paraName, child);
                paragraphNames.add(paraName);
            }
        }

        try {
            for (ASTNode child : procedureNode.getChildren()) {
                if (child.getType().equals("Sentence")) {
                    try {
                        for (ASTNode statement : child.getChildren()) {
                            executeStatement(statement);
                        }
                    } catch (NextSentenceException e) {
                    }
                } else if (child.getType().equals("Paragraph")) {
                    executeParagraph(child);
                }
            }

        } catch (GoToException e) {
            executeParagraphByName(e.getTarget());

        } catch (RuntimeException e) {

            if (signalHandler != null) {
                String handler = signalHandler;
                signalHandler = null;        // avoid recursive SIGNALs
                executeParagraphByName(handler);
                return;
            }

            throw e;
        }
    }

    private void executeParagraph(ASTNode paragraphNode) {
        currentParagraph = paragraphNode.getText().toLowerCase();
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

    private void executeParagraphByName(String name) {

        int index = paragraphNames.indexOf(name.toLowerCase());

        if (index == -1) {
            throw new RuntimeException("Paragraph not found: " + name);
        }

        while (index < paragraphNames.size()) {
            ASTNode paragraph = paragraphs.get(paragraphNames.get(index));

            try {
                executeParagraph(paragraph);
                index++;
            }
            catch (GoToException e) {
                index = paragraphNames.indexOf(e.getTarget().toLowerCase());

                if (index == -1) {
                    throw new RuntimeException("Paragraph not found: " + e.getTarget());
                }
            }
            catch (RuntimeException e) {

                if (signalHandler != null) {
                    String handler = signalHandler;
                    signalHandler = null;     // Disable SIGNAL while executing it

                    index = paragraphNames.indexOf(handler);

                    if (index == -1) {
                        throw new RuntimeException(
                                "Signal paragraph not found: " + handler);
                    }
                } else {
                    throw e;
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
                executeEvaluate(statement);
                break;
            case "PerformStmt":
                executePerform(statement);
                break;
            case "NextSentenceStmt":
                throw new NextSentenceException();
            case "StopStmt":
                throw new StopProgramException();
            case "GoToStmt":
                executeGoTo(statement);
                break;
            case "CallStmt":
                executeCall(statement);
                break;
            case "AlterStmt":
                executeAlter(statement);
                break;
            case "SignalStmt":
                executeSignal(statement);
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
        ASTNode sourceNode = node.getChildren().get(0);

        if (sourceNode.getType().equals("AtomicID")) {

            Symbol source = symbolTable.getSymbol(sourceNode.getText());

            if (source != null && source.isRecord()) {

                moveRecord(sourceNode.getText(), node);
                return;
            }
        }

        Object value = evaluateAtomic(sourceNode);

        for (int i = 1; i < node.getChildren().size(); i++) {

            ASTNode target = node.getChildren().get(i);

            if (!target.getType().equals("ToID")) {
                continue;
            }

            String varName = target.getText().toLowerCase();

            Object actualValue = value;

            if ("__SPACES__".equals(value)) {
                Symbol symbol = symbolTable.getSymbol(varName);

                if (symbol != null && symbol.getPicture() != null) {

                    String picture = symbol.getPicture();
                    int length = 0;

                    for (int k = 0; k < picture.length(); k++) {
                        char c = picture.charAt(k);

                        if (c == 'X' || c == 'A' || c == '9' || c == 'Z') {

                            if (k + 1 < picture.length() && picture.charAt(k + 1) == '(') {

                                int close = picture.indexOf(')', k + 1);

                                int count = Integer.parseInt(
                                        picture.substring(k + 2, close));

                                length += count;
                                k = close;

                            } else {
                                length++;
                            }
                        }
                    }
                    actualValue = " ".repeat(length);
                } else {
                    actualValue = "";
                }
            } else if ("__HIGH_VALUES__".equals(value)) {
                actualValue = Character.toString(Character.MAX_VALUE);
            } else if ("__LOW_VALUES__".equals(value)) {
                actualValue = Character.toString(Character.MIN_VALUE);
            }

            if (memory.containsKey(varName) && memory.get(varName) instanceof Object[]) {

                Object[] array = (Object[]) memory.get(varName);

                if (actualValue instanceof Object[]) {

                    Object[] srcArray = (Object[]) actualValue;

                    for (int j = 0; j < Math.min(array.length, srcArray.length); j++) {
                        array[j] = srcArray[j];
                    }
                } else {

                    for (int j = 0; j < array.length; j++) {
                        array[j] = coerceValue(actualValue, array[j]);
                    }
                }

            } else {
                if (memory.containsKey(varName)) {
                    actualValue = coerceValue(actualValue, memory.get(varName));
                }
                memory.put(varName, actualValue);
            }
        }
    }

    private void moveRecord(String sourceRecord, ASTNode moveNode) {

        Symbol source = symbolTable.getSymbol(sourceRecord);

        for (int i = 1; i < moveNode.getChildren().size(); i++) {

            ASTNode targetNode = moveNode.getChildren().get(i);

            if (!targetNode.getType().equals("ToID"))
                continue;

            Symbol target = symbolTable.getSymbol(targetNode.getText());

            if (target == null || !target.isRecord())
                continue;

            for (Symbol srcField : symbolTable.getAllSymbols()) {

                if (!source.getName().equalsIgnoreCase(srcField.getParentName()))
                    continue;

                for (Symbol dstField : symbolTable.getAllSymbols()) {

                    if (!target.getName().equalsIgnoreCase(dstField.getParentName()))
                        continue;

                    if (srcField.getName().equalsIgnoreCase(dstField.getName())) {

                        Object value = memory.get(srcField.getName().toLowerCase());

                        if (value != null) {
                            memory.put(dstField.getName().toLowerCase(), value);
                        }
                    }
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
                        
                        // special handling if the subject is "TRUE" (so the condition mode)
                        if (subjectValue instanceof Boolean && (Boolean) subjectValue) {
                            subMatch = evaluateCondition(subWhenNode);
                        } else {
                            // SubWhenSubject contains WhenValue children separated by OR
                            // matches if ANY WhenValue matches
                            for (ASTNode whenValueNode : subWhenNode.getChildren()) {
                                if (matchWhenValue(subjectValue, whenValueNode)) {
                                    subMatch = true;
                                    break;
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

    /**
     * checks whether a single WhenValue matches the given subject value,
     * A WhenValue has 1 WhenValueExpression child (exact match) or 2 (the range for THROUGH)
     * each of WhenValueExpression wraps the actual value/expression
     */
    private boolean matchWhenValue(Object subjectValue, ASTNode whenValueNode) {
        java.util.List<ASTNode> children = whenValueNode.getChildren();
        
        if (children.size() == 1) {
            // exact value match, so extract single value from WhenValueExpression
            ASTNode wve = children.get(0);
            Object whenVal = resolveAtomicValue(wve.getChildren().get(0));
            return compareValues(subjectValue, whenVal) == 0;
        } else if (children.size() == 2) {
            // THROUGH range, so extract values from both WhenValueExpressions
            ASTNode wveStart = children.get(0);
            ASTNode wveEnd = children.get(1);
            Object rangeStart = resolveAtomicValue(wveStart.getChildren().get(0));
            Object rangeEnd = resolveAtomicValue(wveEnd.getChildren().get(0));
            return compareValues(subjectValue, rangeStart) >= 0 && compareValues(subjectValue, rangeEnd) <= 0;
        }
        
        return false;
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
        switch (node.getType()) {

            case "AtomicID":
                String varName = node.getText().toLowerCase();

                if (!memory.containsKey(varName)) {
                    throw new RuntimeException("Variable not initialized: " + varName);
                }

                Object val = memory.get(varName);

                if (val instanceof Object[]) {
                    Object[] arr = (Object[]) val;
                    return arr.length > 0 ? arr[0] : 0.0;
                }

                return val;

            case "AtomicInt":
            case "AtomicDecimal":
                return Double.parseDouble(node.getText());

            case "AtomicString":
                String text = node.getText();
                return text.substring(1, text.length() - 1);

            case "AtomicSpaces":
                return "__SPACES__";

            case "AtomicHighValues":
                return "__HIGH_VALUES__";

            case "AtomicLowValues":
                return "__LOW_VALUES__";

            default:
                if (!node.getChildren().isEmpty()) {
                    return evaluateAtomic(node.getChildren().get(0));
                }

                return node.getText();
        }
    }

    private boolean evaluateCondition(ASTNode conditionNode) {
        // this is to handle NOT unary expression
        if (conditionNode.getType().equals("UnaryExpression")) {
            java.util.List<ASTNode> children = conditionNode.getChildren();
            // check for NOT operator
            if (children.size() >= 2 && 
                children.get(0).getType().equals("UnaryOp") && 
                "NOT".equalsIgnoreCase(children.get(0).getText())
            ) {
                return !evaluateCondition(children.get(1));
            }
        }

        // this is to handle ContractedRelationalExpression
        if (conditionNode.getType().equals("ContractedRelationalExpression")) {
            java.util.List<ASTNode> children = conditionNode.getChildren();
            if (children.size() == 1) {
                return evaluateCondition(children.get(0));
            }
            if (children.size() == 2 
                && children.get(0).getType().equals("RelationalOperator")) {
                // the contracted form "> value" is not meaningful without a left operand
                return false;
            }
        }

        // handle SubWhenSubject when TRUE
        // SubWhenSubject has WhenValue children separated by OR
        if (conditionNode.getType().equals("SubWhenSubject")) {
            for (ASTNode whenValueNode : conditionNode.getChildren()) {
                if (evaluateWhenValue(whenValueNode)) {
                    return true; // it is OR, so the first match wins
                }
            }
            return false;
        }

        // handle when WhenValue delegates to its WhenValueExpression
        if (conditionNode.getType().equals("WhenValue")) {
            return evaluateWhenValue(conditionNode);
        }

        // handle WhenValueExpression, AND between children
        if (conditionNode.getType().equals("WhenValueExpression")) {
            java.util.List<ASTNode> children = conditionNode.getChildren();

            boolean result = evaluateCondition(children.get(0));
            for (int i = 1; i + 1 < children.size(); i += 2) {
                // children.get(i) is LogicalOp("AND")
                result = result && evaluateCondition(children.get(i + 1));
            }
            return result;
        }

        // handle LogicalExpression with AND/OR
        if (conditionNode.getType().equals("LogicalExpression")) {
            java.util.List<ASTNode> children = conditionNode.getChildren();
            boolean result = evaluateCondition(children.get(0));
            for (int i = 1; i + 1 < children.size(); i += 2) {
                String op = children.get(i).getText();
                boolean next = evaluateCondition(children.get(i + 1));
                if ("AND".equalsIgnoreCase(op)) {
                    result = result && next;
                } else if ("OR".equalsIgnoreCase(op)) {
                    result = result || next;
                }
            }
            return result;
        }

        // for a simple RelationalExpression evaluation:
        if (conditionNode.getType().equals("RelationalExpression")) {
            java.util.List<ASTNode> children = conditionNode.getChildren();
            if (children.size() >= 3) {
                double left = evaluateMathExpression(children.get(0));
                String op = children.get(1).getText();
                double right = evaluateMathExpression(children.get(2));
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
        }

        // drill down to the actual relational or boolean node
        if (!conditionNode.getChildren().isEmpty()) {
            return evaluateCondition(conditionNode.getChildren().get(0));
        }

        return false; 
    }

    /**
     * evaluate a WhenValue node when TRUE
     * each WhenValue has a WhenValueExpression wrapping the condition/s
     */
    private boolean evaluateWhenValue(ASTNode whenValueNode) {
        for (ASTNode wve : whenValueNode.getChildren()) {
            if (evaluateCondition(wve)) {
                return true;
            }
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

    private void executeGoTo(ASTNode node) {

        String target = node.getText().toLowerCase();

        if (currentParagraph != null &&
                alteredTargets.containsKey(currentParagraph)) {

            target = alteredTargets.get(currentParagraph);
        }

        if (paragraphs.containsKey(target)) {
            throw new GoToException(target);
        }

        if (memory.containsKey(target)) {

            Object value = memory.get(target);

            if (!(value instanceof String)) {
                throw new RuntimeException(
                        "GO TO field '" + target + "' does not contain a paragraph name");
            }

            String runtimeTarget = ((String) value).toLowerCase();

            if (!paragraphs.containsKey(runtimeTarget)) {
                throw new RuntimeException(
                        "GO TO runtime target does not exist: " + runtimeTarget);
            }

            throw new GoToException(runtimeTarget);
        }

        throw new RuntimeException(
                "Unknown GO TO target or field: " + target);
    }

    private void executeCall(ASTNode node) {
        String programName = node.getText();

        try {
            String source = BabyCobolParserUtils.readResource(
                    "/examples/" + programName + ".babycob"
            );

            String processed = BabyCobolParserUtils.preprocess(source);

            ASTUtils.ASTResult result =
                    ASTUtils.buildASTAndSymbolTable(processed);

            BabyCobolInterpreter child =
                    new BabyCobolInterpreter(result.symbolTable);

            try {
                child.execute(result.root);
            } catch (StopProgramException e) {
                // Normal termination of the called program.
                // Return to the caller.
            }

        } catch (Exception e) {
            throw new RuntimeException("CALL failed for program: " + programName, e);
        }
    }

    private void executeAlter(ASTNode node) {

        String source = node.getChildren().get(0).getText().toLowerCase();
        String target = node.getChildren().get(1).getText().toLowerCase();

        ASTNode paragraph = paragraphs.get(source);

        if (paragraph == null) {
            throw new RuntimeException("Unknown paragraph: " + source);
        }

        if (paragraph.getChildren().size() != 1) {
            throw new RuntimeException(
                    "ALTER requires exactly one sentence.");
        }

        ASTNode sentence = paragraph.getChildren().get(0);

        if (sentence.getChildren().size() != 1) {
            throw new RuntimeException(
                    "ALTER requires exactly one statement.");
        }

        ASTNode stmt = sentence.getChildren().get(0);

        if (!stmt.getType().equals("GoToStmt")) {
            throw new RuntimeException(
                    "ALTER only works on paragraphs containing one GO TO.");
        }

        if (!paragraphs.containsKey(target)) {
            throw new RuntimeException(
                    "Unknown ALTER target: " + target);
        }

        alteredTargets.put(source, target);
    }

    private void executeSignal(ASTNode node) {

        ASTNode child = node.getChildren().get(0);

        if (child.getType().equals("Off")) {
            signalHandler = null;
        } else {
            if (!paragraphs.containsKey(child.getText().toLowerCase())) {
                throw new RuntimeException(
                        "Unknown SIGNAL paragraph: " + child.getText());
            }

            signalHandler = child.getText().toLowerCase();
        }
    }
}