package interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import java.util.Scanner;

import preprocessing.BabyCobolParserUtils;
import preprocessing.GoToException;
import preprocessing.NextSentenceException;
import preprocessing.StopProgramException;
import ast.*;

public class BabyCobolInterpreter {
    private SymbolTable symbolTable;
    private Map<String, Object> memory;

    // to store and order paragraphs for PERFORM/THROUGH
    private Map<String, ASTNode> paragraphs;
    private java.util.List<String> paragraphNames;
    private Map<String, String> alteredGoToTargets;
    private String currentParagraphName;
    private String signalHandlerParagraph;

    public Map<String, Object> getMemory() {
        return memory;
    }

    public BabyCobolInterpreter(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.memory = new HashMap<>();

        this.paragraphs = new java.util.LinkedHashMap<>(); // maintains insertion order
        this.paragraphNames = new java.util.ArrayList<>();
        this.alteredGoToTargets = new HashMap<>();
        this.currentParagraphName = null;
        this.signalHandlerParagraph = null;

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
                String qualifiedKey = memoryKey(symbol);
                Object defaultValue = defaultValueForPicture(pic);
                if (symbol.getOccurs() > 0) {
                    // OCCURS array-- creates an array of initialized values
                    Object[] array = new Object[symbol.getOccurs()];
                    for (int i = 0; i < symbol.getOccurs(); i++) {
                        array[i] = defaultValueForPicture(pic);
                    }
                    memory.put(qualifiedKey, array);
                    if (hasUnambiguousSimpleName(symbol)) {
                        memory.put(symbol.getName().toLowerCase(Locale.ROOT), array);
                    }
                } else {
                    memory.put(qualifiedKey, defaultValue);
                    if (hasUnambiguousSimpleName(symbol)) {
                        memory.put(symbol.getName().toLowerCase(Locale.ROOT), defaultValue);
                    }
                }
            } else if (symbol.getOccurs() > 0) {
                // group record with OCCURS, initialize as placeholder
                Object[] array = new Object[symbol.getOccurs()];
                memory.put(memoryKey(symbol), array);
                if (hasUnambiguousSimpleName(symbol)) {
                    memory.put(symbol.getName().toLowerCase(Locale.ROOT), array);
                }
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

    private String memoryKey(Symbol symbol) {
        List<String> parts = new ArrayList<>();
        Symbol current = symbol;
        while (current != null) {
            parts.add(current.getName().toLowerCase(Locale.ROOT));
            current = current.getParent();
        }
        return String.join(" of ", parts);
    }

    private boolean hasUnambiguousSimpleName(Symbol symbol) {
        return symbolTable.getSymbolsByName(symbol.getName()).size() == 1;
    }

    private boolean hasMemoryValue(String name) {
        return memory.containsKey(normalizeRuntimeName(name));
    }

    private Object getMemoryValue(String name) {
        return memory.get(normalizeRuntimeName(name));
    }

    private void setMemoryValue(String name, Object value) {
        String key = normalizeRuntimeName(name);
        memory.put(key, value);

        Symbol symbol = symbolForRuntimeName(name);
        if (symbol != null) {
            String qualifiedKey = memoryKey(symbol);
            memory.put(qualifiedKey, value);
            if (hasUnambiguousSimpleName(symbol)) {
                memory.put(symbol.getName().toLowerCase(Locale.ROOT), value);
            }
        }
    }

    private String normalizeRuntimeName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private Symbol symbolForRuntimeName(String name) {
        String key = normalizeRuntimeName(name);
        for (Symbol symbol : symbolTable.getAllSymbols()) {
            if (memoryKey(symbol).equals(key)) {
                return symbol;
            }
        }

        List<Symbol> symbols = symbolTable.getSymbolsByName(name);
        if (symbols.size() == 1) {
            return symbols.get(0);
        }
        return null;
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
        paragraphs.clear();
        paragraphNames.clear();
        alteredGoToTargets.clear();
        currentParagraphName = null;
        signalHandlerParagraph = null;
        for (ASTNode child : procedureNode.getChildren()) {
            if (child.getType().equals("Paragraph")) {
                String paraName = child.getText().toLowerCase(Locale.ROOT);
                paragraphs.put(paraName, child);
                paragraphNames.add(paraName);
            }
        }

        // 2) execution: running top level sentences and fall through paragraphs
        for (int i = 0; i < procedureNode.getChildren().size(); i++) {
            ASTNode child = procedureNode.getChildren().get(i);
            if (child.getType().equals("Sentence")) {
                try {
                    for (ASTNode statement : child.getChildren()) {
                        executeStatementWithSignal(statement);
                    }
                } catch (NextSentenceException e) {
                    // NEXT SENTENCE skips to the next sentence
                } catch (GoToException e) {
                    i = indexOfParagraphChild(procedureNode, e.getTarget());
                }
            } else if (child.getType().equals("Paragraph")) {
                // apparently in COBOL execution falls through into paragraphs 
                // unless stopped by a GO TO or STOP RUN
                try {
                    executeParagraph(child);
                } catch (GoToException e) {
                    i = indexOfParagraphChild(procedureNode, e.getTarget());
                }
            }
        }
    }

    private void executeParagraph(ASTNode paragraphNode) {
        String previousParagraph = currentParagraphName;
        currentParagraphName = paragraphNode.getText().toLowerCase(Locale.ROOT);
        try {
            for (ASTNode sentence : paragraphNode.getChildren()) {
                if (sentence.getType().equals("Sentence")) {
                    try {
                        for (ASTNode statement : sentence.getChildren()) {
                            executeStatementWithSignal(statement);
                        }
                    } catch (NextSentenceException e) {
                        // NEXT SENTENCE skips to the next sentence
                    }
                }
            }
        } finally {
            currentParagraphName = previousParagraph;
        }
    }

    private void executeStatementWithSignal(ASTNode statement) {
        try {
            executeStatement(statement);
        } catch (RuntimeException e) {
            if (isControlFlowException(e)) {
                throw e;
            }
            handleFatalError(e);
        }
    }

    private boolean isControlFlowException(RuntimeException e) {
        return e instanceof GoToException
                || e instanceof NextSentenceException
                || e instanceof StopProgramException;
    }

    private void handleFatalError(RuntimeException e) {
        if (signalHandlerParagraph != null && !isExecutingSignalHandler()) {
            throw new GoToException(signalHandlerParagraph);
        }
        throw e;
    }

    private boolean isExecutingSignalHandler() {
        return currentParagraphName != null && currentParagraphName.equals(signalHandlerParagraph);
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
            case "GoToStmt":
                executeGoTo(statement);
                break;
            case "AlterStmt":
                executeAlter(statement);
                break;
            case "SignalStmt":
                executeSignal(statement);
                break;
            case "SignalOffStmt":
                signalHandlerParagraph = null;
                break;
            default:
                System.err.println("Unimplemented statement type: " + statement.getType());
        }
    }

    // --- statement implementations ---

    private void executeGoTo(ASTNode node) {
        if (currentParagraphName != null && alteredGoToTargets.containsKey(currentParagraphName)) {
            throw new GoToException(alteredGoToTargets.get(currentParagraphName));
        }
        throw new GoToException(resolveGoToTarget(node.getText()));
    }

    private void executeAlter(ASTNode node) {
        String alteredParagraphName = node.getText();
        String alteredKey = alteredParagraphName.toLowerCase(Locale.ROOT);
        String newTargetName = node.getChildren().get(0).getText();
        String newTargetKey = newTargetName.toLowerCase(Locale.ROOT);

        if (!paragraphs.containsKey(alteredKey)) {
            throw new RuntimeException("ALTER target paragraph does not exist: " + alteredParagraphName);
        }
        if (!paragraphs.containsKey(newTargetKey)) {
            throw new RuntimeException("ALTER new target paragraph does not exist: " + newTargetName);
        }
        if (!isAlterableGoToParagraph(paragraphs.get(alteredKey))) {
            throw new RuntimeException("ALTER paragraph must contain exactly one sentence with exactly one GO TO statement: "
                    + alteredParagraphName);
        }

        alteredGoToTargets.put(alteredKey, newTargetKey);
    }

    private boolean isAlterableGoToParagraph(ASTNode paragraphNode) {
        if (paragraphNode.getChildren().size() != 1) {
            return false;
        }
        ASTNode sentence = paragraphNode.getChildren().get(0);
        return sentence.getType().equals("Sentence")
                && sentence.getChildren().size() == 1
                && sentence.getChildren().get(0).getType().equals("GoToStmt");
    }

    private void executeSignal(ASTNode node) {
        String handlerName = node.getText();
        String handlerKey = handlerName.toLowerCase(Locale.ROOT);
        if (!paragraphs.containsKey(handlerKey)) {
            throw new RuntimeException("SIGNAL handler paragraph does not exist: " + handlerName);
        }
        signalHandlerParagraph = handlerKey;
    }

    private String resolveGoToTarget(String target) {
        String key = target.toLowerCase(Locale.ROOT);
        if (paragraphs.containsKey(key)) {
            return key;
        }

        if (!memory.containsKey(key)) {
            throw new RuntimeException("GO TO target is neither paragraph nor field: " + target);
        }

        Object runtimeValue = memory.get(key);
        if (runtimeValue instanceof Object[]) {
            throw new RuntimeException("GO TO computed target field cannot be an OCCURS array: " + target);
        }

        String computedTarget = String.valueOf(runtimeValue).trim();
        if (computedTarget.isEmpty()) {
            throw new RuntimeException("GO TO computed target is empty for field: " + target);
        }

        String computedKey = computedTarget.toLowerCase(Locale.ROOT);
        if (!paragraphs.containsKey(computedKey)) {
            throw new RuntimeException("GO TO computed target paragraph does not exist: " + computedTarget);
        }
        return computedKey;
    }

    private void executeCall(ASTNode node) {
        String programName = node.getText();

        String resourcePath = "/examples/" + programName.toLowerCase(Locale.ROOT) + ".babycob";
        try {
            String source = BabyCobolParserUtils.readResource(resourcePath);
            String processed = BabyCobolParserUtils.preprocess(source);
            ASTUtils.ASTResult ast = ASTUtils.buildASTAndSymbolTable(processed);
            BabyCobolInterpreter calledInterpreter = new BabyCobolInterpreter(ast.symbolTable);
            bindCallArguments(node, ast.root, calledInterpreter, programName);
            calledInterpreter.execute(ast.root);
        } catch (RuntimeException e) {
            throw new RuntimeException("CALL failed for program " + programName + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("CALL failed for program " + programName + ": " + e.getMessage(), e);
        }
    }

    private void bindCallArguments(ASTNode callNode, ASTNode calledProgramRoot,
                                   BabyCobolInterpreter calledInterpreter, String programName) {
        ASTNode callUsingClause = findChild(callNode, "UsingCallClause");
        ASTNode procedureUsingClause = findProcedureUsingClause(calledProgramRoot);

        if (callUsingClause == null) {
            if (procedureUsingClause != null && !procedureUsingClause.getChildren().isEmpty()) {
                throw new RuntimeException("CALL USING argument count mismatch for " + programName
                        + ": caller provided 0 but callee expects "
                        + procedureUsingClause.getChildren().size());
            }
            return;
        }

        if (procedureUsingClause == null) {
            throw new RuntimeException("CALL USING argument count mismatch for " + programName
                    + ": caller provided " + callUsingClause.getChildren().size()
                    + " but callee expects 0");
        }

        int callerCount = callUsingClause.getChildren().size();
        int calleeCount = procedureUsingClause.getChildren().size();
        if (callerCount != calleeCount) {
            throw new RuntimeException("CALL USING argument count mismatch for " + programName
                    + ": caller provided " + callerCount + " but callee expects " + calleeCount);
        }

        for (int i = 0; i < callerCount; i++) {
            String callerName = callUsingClause.getChildren().get(i).getText().toLowerCase(Locale.ROOT);
            String calleeName = procedureUsingClause.getChildren().get(i).getText().toLowerCase(Locale.ROOT);

            if (!memory.containsKey(callerName)) {
                throw new RuntimeException("CALL USING argument is not initialized: " + callerName);
            }
            if (!calledInterpreter.memory.containsKey(calleeName)) {
                throw new RuntimeException("CALL USING parameter is not initialized in " + programName
                        + ": " + calleeName);
            }

            calledInterpreter.memory.put(calleeName, memory.get(callerName));
        }
    }

    private ASTNode findProcedureUsingClause(ASTNode programRoot) {
        ASTNode procedure = findChild(programRoot, "Procedure");
        if (procedure == null) {
            return null;
        }
        return findChild(procedure, "UsingProcedureClause");
    }

    private ASTNode findChild(ASTNode node, String type) {
        for (ASTNode child : node.getChildren()) {
            if (child.getType().equals(type)) {
                return child;
            }
        }
        return null;
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
        ASTNode sourceNode = node.getChildren().get(0);
        boolean figurativeMove = sourceNode.getType().equals("AtomicFigurative");
        
        // looping through all target IDs
        for (int i = 1; i < node.getChildren().size(); i++) {
            ASTNode target = node.getChildren().get(i);
            if (target.getType().equals("ToID")) {
                String varName = target.getText().toLowerCase(Locale.ROOT);
                if (!figurativeMove && sourceNode.getType().equals("AtomicID")
                        && isRecordMove(sourceNode.getText(), target.getText())) {
                    executeRecordMove(sourceNode.getText(), target.getText());
                    continue;
                }

                Object value = figurativeMove
                        ? evaluateFigurativeConstantForTarget(sourceNode.getText(), varName)
                        : evaluateAtomic(sourceNode);

                // if the target is an OCCURS array, assign value to all elements
                if (hasMemoryValue(varName) && getMemoryValue(varName) instanceof Object[]) {
                    Object[] array = (Object[]) getMemoryValue(varName);
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
                    setMemoryValue(varName, array);
                } else {
                    // coerce value to match the target's type if possible
                    if (hasMemoryValue(varName)) {
                        value = coerceValue(value, getMemoryValue(varName));
                    }
                    setMemoryValue(varName, value);
                }
            }
        }
    }

    private boolean isRecordMove(String sourceName, String targetName) {
        Symbol source = symbolForRuntimeName(sourceName);
        Symbol target = symbolForRuntimeName(targetName);
        return source != null && target != null && source.isRecord() && target.isRecord();
    }

    private void executeRecordMove(String sourceName, String targetName) {
        Symbol source = symbolForRuntimeName(sourceName);
        Symbol target = symbolForRuntimeName(targetName);
        if (source == null || target == null || !source.isRecord() || !target.isRecord()) {
            throw new RuntimeException("MOVE requires record source and target for corresponding move: "
                    + sourceName + " TO " + targetName);
        }
        moveCorrespondingChildren(source, target);
    }

    private void moveCorrespondingChildren(Symbol sourceRecord, Symbol targetRecord) {
        Map<String, Symbol> sourceChildren = directChildrenByName(sourceRecord, "source");
        Map<String, Symbol> targetChildren = directChildrenByName(targetRecord, "target");

        for (Map.Entry<String, Symbol> entry : sourceChildren.entrySet()) {
            Symbol sourceChild = entry.getValue();
            Symbol targetChild = targetChildren.get(entry.getKey());
            if (targetChild == null) {
                continue;
            }
            if (sourceChild.isRecord() && targetChild.isRecord()) {
                moveCorrespondingChildren(sourceChild, targetChild);
            } else if (!sourceChild.isRecord() && !targetChild.isRecord()) {
                Object value = getMemoryValue(memoryKey(sourceChild));
                setMemoryValue(memoryKey(targetChild), coerceValue(value, getMemoryValue(memoryKey(targetChild))));
            }
        }
    }

    private Map<String, Symbol> directChildrenByName(Symbol parent, String side) {
        Map<String, Symbol> children = new LinkedHashMap<>();
        for (Symbol candidate : symbolTable.getAllSymbols()) {
            if (candidate.getParent() == parent) {
                String key = candidate.getName().toLowerCase(Locale.ROOT);
                if (children.containsKey(key)) {
                    throw new RuntimeException("Ambiguous MOVE corresponding " + side
                            + " child name '" + candidate.getName() + "' under record " + parent.getName());
                }
                children.put(key, candidate);
            }
        }
        return children;
    }

    private Object evaluateFigurativeConstantForTarget(String constant, String targetName) {
        Symbol targetSymbol = symbolForRuntimeName(targetName);
        if (targetSymbol == null || targetSymbol.getPicture() == null || targetSymbol.getPicture().isEmpty()) {
            throw new RuntimeException("Cannot MOVE " + constant + " to non-elementary target: " + targetName);
        }

        String picture = targetSymbol.getPicture();
        if (isRuntimeNumericPicture(picture)) {
            return numericFigurativeValue(constant, picture);
        }
        return stringFigurativeValue(constant, picture);
    }

    private boolean isRuntimeNumericPicture(String picture) {
        return picture != null && picture.toUpperCase(Locale.ROOT).contains("9");
    }

    private Object numericFigurativeValue(String constant, String picture) {
        switch (constant.toUpperCase(Locale.ROOT)) {
            case "HIGH-VALUES":
                return highestNumericValue(picture);
            case "SPACES":
            case "LOW-VALUES":
                return 0.0;
            default:
                throw new RuntimeException("Unknown figurative constant: " + constant);
        }
    }

    private double highestNumericValue(String picture) {
        StringBuilder numeric = new StringBuilder();
        for (char ch : expandPicture(picture).toCharArray()) {
            switch (Character.toUpperCase(ch)) {
                case '9':
                case 'Z':
                    numeric.append('9');
                    break;
                case 'V':
                    numeric.append('.');
                    break;
                case 'S':
                    break;
                default:
                    break;
            }
        }

        String number = numeric.toString();
        if (number.isEmpty() || number.equals(".")) {
            return 0.0;
        }
        return Double.parseDouble(number);
    }

    private String stringFigurativeValue(String constant, String picture) {
        StringBuilder value = new StringBuilder();
        for (char ch : expandPicture(picture).toCharArray()) {
            char normalized = Character.toUpperCase(ch);
            switch (constant.toUpperCase(Locale.ROOT)) {
                case "SPACES":
                    value.append(spaceValueForPictureSymbol(normalized));
                    break;
                case "HIGH-VALUES":
                    value.append(highValueForPictureSymbol(normalized));
                    break;
                case "LOW-VALUES":
                    value.append(lowValueForPictureSymbol(normalized));
                    break;
                default:
                    throw new RuntimeException("Unknown figurative constant: " + constant);
            }
        }
        return value.toString();
    }

    private char spaceValueForPictureSymbol(char symbol) {
        switch (symbol) {
            case '9':
                return '0';
            case 'V':
                return '.';
            default:
                return ' ';
        }
    }

    private char highValueForPictureSymbol(char symbol) {
        switch (symbol) {
            case '9':
            case 'Z':
                return '9';
            case 'A':
                return 'z';
            case 'X':
                return '\u00ff';
            case 'S':
                return '+';
            case 'V':
                return '.';
            default:
                return '\u00ff';
        }
    }

    private char lowValueForPictureSymbol(char symbol) {
        switch (symbol) {
            case '9':
                return '0';
            case 'X':
                return '\u0000';
            case 'S':
                return '-';
            case 'V':
                return '.';
            default:
                return ' ';
        }
    }

    private String expandPicture(String picture) {
        StringBuilder expanded = new StringBuilder();
        String normalized = picture.toUpperCase(Locale.ROOT);

        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (!isPictureSymbol(ch)) {
                continue;
            }

            int repeat = 1;
            if (i + 1 < normalized.length() && normalized.charAt(i + 1) == '(') {
                int close = normalized.indexOf(')', i + 2);
                if (close > i + 2) {
                    repeat = Integer.parseInt(normalized.substring(i + 2, close));
                    i = close;
                }
            }

            for (int j = 0; j < repeat; j++) {
                expanded.append(ch);
            }
        }

        return expanded.toString();
    }

    private boolean isPictureSymbol(char ch) {
        return ch == '9' || ch == 'A' || ch == 'X' || ch == 'Z' || ch == 'S' || ch == 'V';
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
                        executeStatementWithSignal(stmt);
                    }
                }
            }
        } else {
            // find "ElseStmt"
            for (ASTNode child : node.getChildren()) {
                if (child.getType().equals("ElseStmt")) {
                    for (ASTNode stmt : child.getChildren()) {
                        executeStatementWithSignal(stmt);
                    }
                }
            }
        }
    }

    private void executeMath(ASTNode node, String op) {
        String targetId = null;
        java.util.List<ASTNode> atomicNodes = new java.util.ArrayList<>();

        List<ASTNode> children = node.getChildren();
        for (ASTNode child : children) {
            if (child.getType().startsWith("Atomic")) {
                atomicNodes.add(child);
            } else if (child.getType().equals("GivingClause") || child.getType().equals("GivingRemainderClause")) {
                targetId = child.getChildren().get(0).getText().toLowerCase(); // first ID in giving
            }
        }

        double result = evaluateMathStatementResult(atomicNodes, op);

        // if no giving clause assign to the last operand
        if (targetId != null) {
            memory.put(targetId, result);
        } else {
             // fallback: assign to the last atomic ID found
             // (we are assuming it is in the same statement because we have filtered out for semantic/logical issues
             // when building ast)
             ASTNode lastAtomic = atomicNodes.get(atomicNodes.size() - 1);
             if (lastAtomic.getType().equals("AtomicID")) {
                 memory.put(lastAtomic.getText().toLowerCase(), result);
             }
        }
    }

    private double evaluateMathStatementResult(List<ASTNode> atomicNodes, String op) {
        if (atomicNodes.isEmpty()) {
            return 0.0;
        }

        switch (op) {
            case "+":
                double sum = 0.0;
                for (ASTNode atomic : atomicNodes) {
                    sum += Double.parseDouble(evaluateAtomic(atomic).toString());
                }
                return sum;
            case "*":
                double product = 1.0;
                for (ASTNode atomic : atomicNodes) {
                    product *= Double.parseDouble(evaluateAtomic(atomic).toString());
                }
                return product;
            case "-":
                double subtraction = Double.parseDouble(evaluateAtomic(atomicNodes.get(atomicNodes.size() - 1)).toString());
                for (int i = 0; i < atomicNodes.size() - 1; i++) {
                    subtraction -= Double.parseDouble(evaluateAtomic(atomicNodes.get(i)).toString());
                }
                return subtraction;
            case "/":
                if (atomicNodes.size() < 2) {
                    return Double.parseDouble(evaluateAtomic(atomicNodes.get(0)).toString());
                }
                double divisor = Double.parseDouble(evaluateAtomic(atomicNodes.get(0)).toString());
                if (divisor == 0.0) {
                    throw new RuntimeException("Division by zero");
                }
                double dividend = Double.parseDouble(evaluateAtomic(atomicNodes.get(atomicNodes.size() - 1)).toString());
                return dividend / divisor;
            default:
                throw new RuntimeException("Unknown arithmetic operator: " + op);
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
                executeStatementWithSignal(stmt);
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
                        executeStatementWithSignal(child.getChildren().get(j));
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
                try {
                    executeParagraph(paragraphs.get(paraToExecute));
                } catch (GoToException e) {
                    int targetIndex = paragraphNames.indexOf(e.getTarget());
                    if (targetIndex < 0) {
                        throw new RuntimeException("GO TO target paragraph does not exist: " + e.getTarget());
                    }
                    if (targetIndex < startIndex || targetIndex > endIndex) {
                        throw e;
                    }
                    p = targetIndex - 1;
                }
            }
        }
    }

    private int indexOfParagraphChild(ASTNode procedureNode, String target) {
        String key = target.toLowerCase();
        if (!paragraphs.containsKey(key)) {
            throw new RuntimeException("GO TO target paragraph does not exist: " + target);
        }

        for (int i = 0; i < procedureNode.getChildren().size(); i++) {
            ASTNode child = procedureNode.getChildren().get(i);
            if (child.getType().equals("Paragraph") && child.getText().equalsIgnoreCase(key)) {
                return i - 1;
            }
        }

        throw new RuntimeException("GO TO target paragraph does not exist: " + target);
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
