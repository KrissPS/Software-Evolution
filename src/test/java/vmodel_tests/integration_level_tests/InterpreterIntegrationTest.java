package vmodel_tests.integration_level_tests;

import ast.ASTUtils;
import interpreter.BabyCobolInterpreter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import preprocessing.BabyCobolParserUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class InterpreterIntegrationTest {

    private static final String RESOURCE_PATH = "src/test/java/vmodel_tests/integration_level_tests/resources/";

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private PrintStream originalOut;
    private InputStream originalIn;

    @BeforeEach
    public void setUp() {
        originalOut = System.out;
        System.setOut(new PrintStream(outContent));
        originalIn = System.in;
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    private BabyCobolInterpreter createInterpreter(String filename) throws Exception {
        String source = Files.readString(Paths.get(RESOURCE_PATH + filename));
        String processed = BabyCobolParserUtils.preprocess(source);
        ASTUtils.ASTResult ast = ASTUtils.buildASTAndSymbolTable(processed);
        return new BabyCobolInterpreter(ast.symbolTable);
    }

    private BabyCobolInterpreter runProgram(String filename) throws Exception {
        BabyCobolInterpreter interpreter = createInterpreter(filename);
        String source = Files.readString(Paths.get(RESOURCE_PATH + filename));
        ASTUtils.ASTResult ast = ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(source));
        interpreter.execute(ast.root);
        return interpreter;
    }

    private void setMockInput(String... lines) {
        System.setIn(new LineByLineInputStream(lines));
    }

    // Custom InputStream to prevent Scanner buffering issues with ACCEPT statements
    private static class LineByLineInputStream extends InputStream {
        private final String[] lines;
        private int lineIdx = 0, charIdx = 0;
        private boolean lineEnded = false;

        LineByLineInputStream(String... lines) { this.lines = lines; }

        @Override
        public int read() {
            if (lineIdx >= lines.length) return -1;
            String line = lines[lineIdx];
            if (charIdx < line.length()) return line.charAt(charIdx++);
            if (!lineEnded) { lineEnded = true; return '\n'; }
            lineIdx++; charIdx = 0; lineEnded = false;
            return -1;
        }
    }

    @Test
    public void testMemoryAndMoveInitialization() throws Exception {
        BabyCobolInterpreter interpreter = createInterpreter("memory_and_move.babycob");

        Map<String, Object> memoryBefore = interpreter.getMemory();
        assertEquals(0.0, memoryBefore.get("numvar"));
        assertEquals("", memoryBefore.get("strvar"));
        assertEquals(0.0, memoryBefore.get("target1"));
        assertEquals(0.0, memoryBefore.get("target2"));

        String source = Files.readString(Paths.get(RESOURCE_PATH + "memory_and_move.babycob"));
        ASTUtils.ASTResult ast = ASTUtils.buildASTAndSymbolTable(BabyCobolParserUtils.preprocess(source));
        interpreter.execute(ast.root);

        Map<String, Object> memoryAfter = interpreter.getMemory();
        assertEquals(123.0, (Double) memoryAfter.get("numvar"));
        assertEquals("HELLO", memoryAfter.get("strvar"));
        assertEquals(456.0, (Double) memoryAfter.get("target1"));
        assertEquals(456.0, (Double) memoryAfter.get("target2"));
    }

    @Test
    public void testRecordMoveSourceToSmallerTargetCopiesOnlyMatchingChildren() throws Exception {
        BabyCobolInterpreter interpreter = runProgram("record_move_source_to_smaller_target.babycob");

        Map<String, Object> memory = interpreter.getMemory();
        assertEquals(111.0, (Double) memory.get("d of c"));
        assertEquals(111.0, (Double) memory.get("d of e"));
        assertEquals(333.0, (Double) memory.get("f of e"));
    }

    @Test
    public void testRecordMoveSourceToLargerTargetLeavesNonMatchingChildrenUntouched() throws Exception {
        BabyCobolInterpreter interpreter = runProgram("record_move_source_to_larger_target.babycob");

        Map<String, Object> memory = interpreter.getMemory();
        assertEquals(444.0, (Double) memory.get("d of c"));
        assertEquals(444.0, (Double) memory.get("d of e"));
        assertEquals(333.0, (Double) memory.get("f of e"));
    }

    @Test
    public void testRecordMoveWithoutMatchingChildNamesDoesNoAssignment() throws Exception {
        BabyCobolInterpreter interpreter = runProgram("record_move_no_matching_children.babycob");

        Map<String, Object> memory = interpreter.getMemory();
        assertEquals(123.0, (Double) memory.get("d"));
        assertEquals(456.0, (Double) memory.get("f"));
    }

    @Test
    public void testNestedRecordMoveCopiesMatchingNestedChildren() throws Exception {
        BabyCobolInterpreter interpreter = runProgram("record_move_nested_matching_children.babycob");

        Map<String, Object> memory = interpreter.getMemory();
        assertEquals(777.0, (Double) memory.get("d of child of target-rec"));
        assertEquals(777.0, (Double) memory.get("d of child of source-rec"));
        assertEquals(888.0, (Double) memory.get("f of child of target-rec"));
    }

    @Test
    public void testRecordMoveWithDuplicateChildrenFailsClearly() {
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                runProgram("record_move_duplicate_children.babycob")
        );

        assertTrue(exception.getMessage().toLowerCase().contains("ambiguous"),
                "Duplicate child error should mention ambiguity, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("MOVE"),
                "Duplicate child error should mention MOVE, got: " + exception.getMessage());
    }

    @Test
    public void testDisplayAndAcceptStatements() throws Exception {
        setMockInput("456", "world");
        BabyCobolInterpreter interpreter = runProgram("display_and_accept.babycob");

        Map<String, Object> memory = interpreter.getMemory();
        assertEquals(456.0, (Double) memory.get("invar"));
        assertEquals("world", memory.get("strvar"));

        String stdout = outContent.toString();
        assertTrue(stdout.contains("ENTER NUM:"));
        assertTrue(stdout.contains("ENTER STR:"));
        assertTrue(stdout.contains("NUM IS 456.0 STR IS world"));
    }

    @Test
    public void testMathOperations() throws Exception {
        Map<String, Object> memory = runProgram("math_operations.babycob").getMemory();
        assertEquals(10.0, (Double) memory.get("a"));
        assertEquals(30.0, (Double) memory.get("b"));
        assertEquals(25.0, (Double) memory.get("d"));
        assertEquals(15.0, (Double) memory.get("c"), 0.0001);
    }

    @Test
    public void testSubtractAndDivideUseTargetOperandDirection() throws Exception {
        Map<String, Object> memory = runProgram("directional_arithmetic.babycob").getMemory();

        assertEquals(20.0, (Double) memory.get("c"));
        assertEquals(3.0, (Double) memory.get("d"), 0.0001);
        assertEquals(25.0, (Double) memory.get("e"));
        assertEquals(5.0, (Double) memory.get("f"), 0.0001);
    }

    @Test
    public void testConditionalAndLoop() throws Exception {
        Map<String, Object> memory = runProgram("conditional_and_loop.babycob").getMemory();
        assertEquals(12.0, (Double) memory.get("x"));
        assertEquals(22.0, (Double) memory.get("y"));
        assertEquals(5.0, (Double) memory.get("counter"));
    }
////////////
    @Test
    public void testEvaluateStatement() throws Exception {
        runProgram("evaluate_statement.babycob");
        String stdout = outContent.toString();
        assertTrue(stdout.contains("X IN 10-20 RANGE"));
        assertTrue(stdout.contains("BOTH MATCH"));
        assertTrue(stdout.contains("X IS GREATER THAN 10"));
    }

    @Test
    public void testPerformStatement() throws Exception {
        Map<String, Object> memory = runProgram("perform_statement.babycob").getMemory();
        // CNT1: PERFORM TASK1 3 TIMES => 3, PERFORM TASK1 THROUGH TASK2 2 TIMES => +2, fallthrough => +1 = 6
        // CNT2: PERFORM TASK1 THROUGH TASK2 2 TIMES => 2, fallthrough => +1 = 3
        assertEquals(6.0, (Double) memory.get("cnt1"));
        assertEquals(3.0, (Double) memory.get("cnt2"));
    }

    @Test
    public void testMoveSpacesUsesTargetPicture() throws Exception {
        Map<String, Object> memory = runProgram("figurative_constants_spaces.babycob").getMemory();

        assertEquals(0.0, (Double) memory.get("num-field"));
        assertEquals("   ", memory.get("text-field"));
        assertEquals("   ", memory.get("alpha-field"));
        assertEquals("   ", memory.get("zero-field"));
        assertEquals(0.0, (Double) memory.get("signed-field"));
        assertEquals(0.0, (Double) memory.get("decimal-field"));
    }

    @Test
    public void testMoveHighValuesUsesTargetPicture() throws Exception {
        Map<String, Object> memory = runProgram("figurative_constants_high_values.babycob").getMemory();

        assertEquals(999.0, (Double) memory.get("num-field"));
        assertEquals("\u00ff\u00ff\u00ff", memory.get("text-field"));
        assertEquals("zzz", memory.get("alpha-field"));
        assertEquals("999", memory.get("zero-field"));
        assertEquals(99.0, (Double) memory.get("signed-field"));
        assertEquals(9.99, (Double) memory.get("decimal-field"), 0.0001);
    }

    @Test
    public void testMoveLowValuesUsesTargetPicture() throws Exception {
        Map<String, Object> memory = runProgram("figurative_constants_low_values.babycob").getMemory();

        assertEquals(0.0, (Double) memory.get("num-field"));
        assertEquals("\u0000\u0000\u0000", memory.get("text-field"));
        assertEquals("   ", memory.get("alpha-field"));
        assertEquals("   ", memory.get("zero-field"));
        assertEquals(0.0, (Double) memory.get("signed-field"));
        assertEquals(0.0, (Double) memory.get("decimal-field"));
    }

    @Test
    public void testPerformThroughWithoutTimesExecutesRangeOnce() throws Exception {
        runProgram("perform_through_once.babycob");

        String stdout = outContent.toString();

        assertTrue(stdout.contains("MAIN BEFORE PERFORM"));
        assertTrue(stdout.contains("FIRST IN RANGE"));
        assertTrue(stdout.contains("SECOND IN RANGE"));
        assertTrue(stdout.contains("MAIN AFTER PERFORM"));
        assertTrue(stdout.indexOf("MAIN BEFORE PERFORM") < stdout.indexOf("FIRST IN RANGE"));
        assertTrue(stdout.indexOf("FIRST IN RANGE") < stdout.indexOf("SECOND IN RANGE"));
        assertTrue(stdout.indexOf("SECOND IN RANGE") < stdout.indexOf("MAIN AFTER PERFORM"));
    }

    @Test
    public void testPerformMissingParagraphThrowsRuntimeError() {
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                runProgram("perform_missing_target.babycob")
        );

        assertTrue(exception.getMessage().contains("PERFORM"),
                "Missing target error should mention PERFORM, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("missing-target"),
                "Missing target error should mention target paragraph, got: " + exception.getMessage());
    }

    @Test
    public void testPerformThroughMissingEndParagraphThrowsRuntimeError() {
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                runProgram("perform_through_missing_end.babycob")
        );

        assertTrue(exception.getMessage().contains("PERFORM"),
                "Missing THROUGH target error should mention PERFORM, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("missing-end"),
                "Missing THROUGH target error should mention end paragraph, got: " + exception.getMessage());
    }

    @Test
    public void testPerformThroughReversedRangeThrowsRuntimeError() {
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                runProgram("perform_through_reversed_range.babycob")
        );

        assertTrue(exception.getMessage().contains("PERFORM"),
                "Reversed THROUGH range error should mention PERFORM, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("first"),
                "Reversed THROUGH range error should mention start paragraph, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("second"),
                "Reversed THROUGH range error should mention end paragraph, got: " + exception.getMessage());
    }

    @Test
    public void testStopTerminatesProgramWithoutExitingJvm() throws Exception {
        assertDoesNotThrow(() -> runProgram("stop_statement.babycob"),
                "STOP should terminate BabyCOBOL execution without exiting the JVM");

        String stdout = outContent.toString();
        assertTrue(stdout.contains("BEFORE STOP"));
        assertFalse(stdout.contains("AFTER STOP"));
    }

    @Test
    public void testCallNoArgumentsExecutesCalledProgramAndReturns() throws Exception {
        runProgram("call_main_no_args.babycob");

        String stdout = outContent.toString();

        assertTrue(stdout.contains("MAIN BEFORE"));
        assertTrue(stdout.contains("HELLO FROM CALL"));
        assertTrue(stdout.contains("MAIN AFTER"));
        assertTrue(stdout.indexOf("MAIN BEFORE") < stdout.indexOf("HELLO FROM CALL"));
        assertTrue(stdout.indexOf("HELLO FROM CALL") < stdout.indexOf("MAIN AFTER"));
    }

    @Test
    public void testCallMissingProgramThrowsRuntimeError() {
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                runProgram("call_missing_program.babycob")
        );

        assertTrue(exception.getMessage().contains("CALL"),
                "Missing program error should mention CALL, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("DOES-NOT-EXIST"),
                "Missing program error should mention target program, got: " + exception.getMessage());
    }

    @Test
    public void testCallStopInCalledProgramReturnsToCaller() throws Exception {
        runProgram("call_main_stop_returns.babycob");

        String stdout = outContent.toString();

        assertTrue(stdout.contains("MAIN BEFORE STOP CALL"));
        assertTrue(stdout.contains("CALLEE BEFORE STOP"));
        assertFalse(stdout.contains("CALLEE AFTER STOP"));
        assertTrue(stdout.contains("MAIN AFTER STOP CALL"));
        assertTrue(stdout.indexOf("MAIN BEFORE STOP CALL") < stdout.indexOf("CALLEE BEFORE STOP"));
        assertTrue(stdout.indexOf("CALLEE BEFORE STOP") < stdout.indexOf("MAIN AFTER STOP CALL"));
    }

    @Test
    public void testCallUsingPassesSingleArgumentByPosition() throws Exception {
        runProgram("call_main_using_single.babycob");

        String stdout = outContent.toString();

        assertTrue(stdout.contains("MAIN BEFORE SINGLE"));
        assertTrue(stdout.contains("CALLEE GOT HELLO"));
        assertTrue(stdout.contains("MAIN AFTER SINGLE"));
        assertTrue(stdout.indexOf("MAIN BEFORE SINGLE") < stdout.indexOf("CALLEE GOT HELLO"));
        assertTrue(stdout.indexOf("CALLEE GOT HELLO") < stdout.indexOf("MAIN AFTER SINGLE"));
    }

    @Test
    public void testCallUsingPassesMultipleArgumentsByPosition() throws Exception {
        runProgram("call_main_using_multiple.babycob");

        String stdout = outContent.toString();

        assertTrue(stdout.contains("CALLEE FIRST ALPHA"));
        assertTrue(stdout.contains("CALLEE SECOND BETA"));
        assertTrue(stdout.indexOf("CALLEE FIRST ALPHA") < stdout.indexOf("CALLEE SECOND BETA"));
    }

    @Test
    public void testCallUsingCountMismatchThrowsRuntimeError() {
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                runProgram("call_main_using_count_mismatch.babycob")
        );

        assertTrue(exception.getMessage().contains("CALL"),
                "USING count mismatch should mention CALL, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("USING"),
                "USING count mismatch should mention USING, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("CALL-ECHO-TWO-ARGS"),
                "USING count mismatch should mention target program, got: " + exception.getMessage());
    }

    @Test
    public void testCallUsingArgumentToCalleeWithoutUsingThrowsRuntimeError() {
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                runProgram("call_main_using_to_no_using_callee.babycob")
        );

        assertTrue(exception.getMessage().contains("CALL"),
                "USING-to-no-USING mismatch should mention CALL, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("USING"),
                "USING-to-no-USING mismatch should mention USING, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("CALL-HELLO-NO-ARGS"),
                "USING-to-no-USING mismatch should mention target program, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("expects 0"),
                "USING-to-no-USING mismatch should mention expected count, got: " + exception.getMessage());
    }

    @Test
    public void testCallWithoutArgumentsToUsingCalleeThrowsRuntimeError() {
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                runProgram("call_main_no_args_to_using_callee.babycob")
        );

        assertTrue(exception.getMessage().contains("CALL"),
                "No-args-to-USING mismatch should mention CALL, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("USING"),
                "No-args-to-USING mismatch should mention USING, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("CALL-ECHO-ONE-ARG"),
                "No-args-to-USING mismatch should mention target program, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("caller provided 0"),
                "No-args-to-USING mismatch should mention provided count, got: " + exception.getMessage());
    }

    @Test
    public void testGoToJumpsToTargetParagraph() throws Exception {
        runProgram("goto_basic.babycob");

        String stdout = outContent.toString();

        assertTrue(stdout.contains("START"));
        assertFalse(stdout.contains("SHOULD NOT PRINT"));
        assertTrue(stdout.contains("TARGET REACHED"));
        assertTrue(stdout.indexOf("START") < stdout.indexOf("TARGET REACHED"));
    }

    @Test
    public void testGoToMissingParagraphThrowsRuntimeError() {
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                runProgram("goto_missing_target.babycob")
        );

        assertTrue(exception.getMessage().contains("GO TO"),
                "Missing target error should mention GO TO, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("MISSING-PARAGRAPH"),
                "Missing target error should mention target paragraph, got: " + exception.getMessage());
    }

    @Test
    public void testGoToInsideLoopTerminatesLoop() throws Exception {
        runProgram("goto_exits_loop.babycob");

        String stdout = outContent.toString();

        assertTrue(stdout.contains("LOOP BEFORE GOTO"));
        assertFalse(stdout.contains("LOOP AFTER GOTO"));
        assertTrue(stdout.contains("DONE AFTER LOOP"));
        assertTrue(stdout.indexOf("LOOP BEFORE GOTO") < stdout.indexOf("DONE AFTER LOOP"));
    }

    @Test
    public void testGoToOutsidePerformThroughTerminatesPerform() throws Exception {
        runProgram("goto_exits_perform_through.babycob");

        String stdout = outContent.toString();

        assertTrue(stdout.contains("MAIN BEFORE PERFORM"));
        assertTrue(stdout.contains("FIRST BEFORE GOTO"));
        assertFalse(stdout.contains("SECOND SHOULD NOT RUN"));
        assertFalse(stdout.contains("MAIN AFTER PERFORM"));
        assertTrue(stdout.contains("OUTSIDE TARGET"));
        assertTrue(stdout.indexOf("FIRST BEFORE GOTO") < stdout.indexOf("OUTSIDE TARGET"));
    }

    @Test
    public void testGoToInsidePerformThroughContinuesAtTargetInsideRange() throws Exception {
        runProgram("goto_inside_perform_through_range.babycob");

        String stdout = outContent.toString();

        assertTrue(stdout.contains("MAIN BEFORE PERFORM"));
        assertTrue(stdout.contains("FIRST BEFORE GOTO"));
        assertFalse(stdout.contains("SECOND SHOULD NOT RUN"));
        assertTrue(stdout.contains("THIRD TARGET"));
        assertTrue(stdout.contains("MAIN AFTER PERFORM"));
        assertTrue(stdout.indexOf("FIRST BEFORE GOTO") < stdout.indexOf("THIRD TARGET"));
        assertTrue(stdout.indexOf("THIRD TARGET") < stdout.indexOf("MAIN AFTER PERFORM"));
    }

    @Test
    public void testGoToCanJumpBackward() throws Exception {
        runProgram("goto_backward.babycob");

        String stdout = outContent.toString();

        assertTrue(stdout.contains("SECOND FIRST"));
        assertTrue(stdout.contains("FIRST AFTER BACKWARD"));
        assertTrue(stdout.indexOf("SECOND FIRST") < stdout.indexOf("FIRST AFTER BACKWARD"));
    }

    @Test
    public void testComputableGoToJumpsToRuntimeParagraphValue() throws Exception {
        runProgram("goto_computable_basic.babycob");

        String stdout = outContent.toString();

        assertTrue(stdout.contains("BEFORE COMPUTED GOTO"));
        assertFalse(stdout.contains("SHOULD NOT PRINT"));
        assertTrue(stdout.contains("COMPUTED TARGET REACHED"));
        assertTrue(stdout.indexOf("BEFORE COMPUTED GOTO") < stdout.indexOf("COMPUTED TARGET REACHED"));
    }

    @Test
    public void testComputableGoToBadRuntimeTargetThrowsRuntimeError() {
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                runProgram("goto_computable_bad_target.babycob")
        );

        assertTrue(exception.getMessage().contains("GO TO"),
                "Bad computed target error should mention GO TO, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("MISSING-PARAGRAPH"),
                "Bad computed target error should mention runtime target, got: " + exception.getMessage());
    }

    @Test
    public void testComputableGoToMissingFieldThrowsRuntimeError() {
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                runProgram("goto_computable_missing_field.babycob")
        );

        assertTrue(exception.getMessage().contains("GO TO"),
                "Missing field error should mention GO TO, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("TARGET-FIELD"),
                "Missing field error should mention unresolved identifier, got: " + exception.getMessage());
    }

    @Test
    public void testGoToParagraphNameWinsOverFieldName() throws Exception {
        runProgram("goto_computable_paragraph_wins.babycob");

        String stdout = outContent.toString();

        assertTrue(stdout.contains("STATIC TARGET WINS"));
        assertFalse(stdout.contains("COMPUTED TARGET SHOULD NOT WIN"));
    }

    @Test
    public void testComputableGoToInsideLoopTerminatesLoop() throws Exception {
        runProgram("goto_computable_exits_loop.babycob");

        String stdout = outContent.toString();

        assertTrue(stdout.contains("LOOP BEFORE COMPUTED GOTO"));
        assertFalse(stdout.contains("LOOP AFTER COMPUTED GOTO"));
        assertTrue(stdout.contains("DONE AFTER COMPUTED LOOP"));
        assertTrue(stdout.indexOf("LOOP BEFORE COMPUTED GOTO") < stdout.indexOf("DONE AFTER COMPUTED LOOP"));
    }

    @Test
    public void testComputableGoToOutsidePerformThroughTerminatesPerform() throws Exception {
        runProgram("goto_computable_exits_perform_through.babycob");

        String stdout = outContent.toString();

        assertTrue(stdout.contains("MAIN BEFORE PERFORM"));
        assertTrue(stdout.contains("FIRST BEFORE COMPUTED GOTO"));
        assertFalse(stdout.contains("SECOND SHOULD NOT RUN"));
        assertFalse(stdout.contains("MAIN AFTER PERFORM"));
        assertTrue(stdout.contains("OUTSIDE COMPUTED TARGET"));
        assertTrue(stdout.indexOf("FIRST BEFORE COMPUTED GOTO") < stdout.indexOf("OUTSIDE COMPUTED TARGET"));
    }

    @Test
    public void testComputableGoToInsidePerformThroughContinuesAtTargetInsideRange() throws Exception {
        runProgram("goto_computable_inside_perform_through_range.babycob");

        String stdout = outContent.toString();

        assertTrue(stdout.contains("MAIN BEFORE PERFORM"));
        assertTrue(stdout.contains("FIRST BEFORE COMPUTED GOTO"));
        assertFalse(stdout.contains("SECOND SHOULD NOT RUN"));
        assertTrue(stdout.contains("THIRD COMPUTED TARGET"));
        assertTrue(stdout.contains("MAIN AFTER PERFORM"));
        assertTrue(stdout.indexOf("FIRST BEFORE COMPUTED GOTO") < stdout.indexOf("THIRD COMPUTED TARGET"));
        assertTrue(stdout.indexOf("THIRD COMPUTED TARGET") < stdout.indexOf("MAIN AFTER PERFORM"));
    }

    @Test
    public void testAlterChangesGoToTarget() throws Exception {
        runProgram("alter_changes_target.babycob");

        String stdout = outContent.toString();

        assertTrue(stdout.contains("ALTERED TARGET"));
        assertFalse(stdout.contains("ORIGINAL TARGET"));
    }

    @Test
    public void testRepeatedAlterChangesTargetAgain() throws Exception {
        runProgram("alter_repeated_changes_target.babycob");

        String stdout = outContent.toString();

        assertTrue(stdout.contains("SECOND ALTERED TARGET"));
        assertFalse(stdout.contains("FIRST ALTERED TARGET"));
        assertFalse(stdout.contains("ORIGINAL TARGET"));
    }

    @Test
    public void testAlterInvalidParagraphShapeThrowsRuntimeError() {
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                runProgram("alter_invalid_paragraph_shape.babycob")
        );

        assertTrue(exception.getMessage().contains("ALTER"),
                "Invalid ALTER shape error should mention ALTER, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("ROUTE"),
                "Invalid ALTER shape error should mention paragraph, got: " + exception.getMessage());
    }

    @Test
    public void testAlterInvalidNewTargetThrowsRuntimeError() {
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                runProgram("alter_invalid_new_target.babycob")
        );

        assertTrue(exception.getMessage().contains("ALTER"),
                "Invalid ALTER target error should mention ALTER, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("MISSING-TARGET"),
                "Invalid ALTER target error should mention missing target, got: " + exception.getMessage());
    }

    @Test
    public void testAlteredComputableGoToBecomesNormalGoTo() throws Exception {
        runProgram("alter_computable_becomes_normal.babycob");

        String stdout = outContent.toString();

        assertTrue(stdout.contains("ALTERED NORMAL TARGET"));
        assertFalse(stdout.contains("COMPUTED FIELD TARGET"));
        assertFalse(stdout.contains("ORIGINAL COMPUTED TARGET"));
    }

    @Test
    public void testSignalDivisionByZeroRoutesToHandler() throws Exception {
        runProgram("signal_division_by_zero_handler.babycob");

        String stdout = outContent.toString();

        assertTrue(stdout.contains("SIGNAL HANDLER RAN"));
        assertFalse(stdout.contains("AFTER FAILURE"));
    }

    @Test
    public void testSignalBadComputableGoToRoutesToHandler() throws Exception {
        runProgram("signal_bad_computable_goto_handler.babycob");

        String stdout = outContent.toString();

        assertTrue(stdout.contains("GOTO SIGNAL HANDLER"));
        assertFalse(stdout.contains("AFTER BAD GOTO"));
    }

    @Test
    public void testSignalOffRestoresNormalFailure() {
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                runProgram("signal_off_restores_failure.babycob")
        );

        assertTrue(exception.getMessage().contains("GO TO"),
                "SIGNAL OFF should restore normal GO TO failure, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("MISSING-TARGET"),
                "SIGNAL OFF failure should mention bad runtime target, got: " + exception.getMessage());
    }

    @Test
    public void testSignalHandlerDoesNotReturnToFailurePoint() throws Exception {
        runProgram("signal_handler_does_not_return.babycob");

        String stdout = outContent.toString();

        assertTrue(stdout.contains("HANDLER START"));
        assertFalse(stdout.contains("AFTER FAILURE"));
    }

    @Test
    public void testSignalErrorInsideHandlerTerminatesNormally() {
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                runProgram("signal_handler_error_terminates.babycob")
        );

        assertTrue(exception.getMessage().contains("GO TO"),
                "Error inside SIGNAL handler should terminate normally, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("BAD-IN-HANDLER"),
                "Handler error should mention the failing target, got: " + exception.getMessage());
    }

    @Test
    public void testSignalExitsLoop() throws Exception {
        runProgram("signal_exits_loop.babycob");

        String stdout = outContent.toString();

        assertTrue(stdout.contains("LOOP BEFORE SIGNAL FAILURE"));
        assertTrue(stdout.contains("LOOP SIGNAL HANDLER"));
        assertFalse(stdout.contains("LOOP AFTER FAILURE"));
    }

    @Test
    public void testSignalExitsPerformThrough() throws Exception {
        runProgram("signal_exits_perform_through.babycob");

        String stdout = outContent.toString();

        assertTrue(stdout.contains("MAIN BEFORE PERFORM"));
        assertTrue(stdout.contains("FIRST BEFORE SIGNAL FAILURE"));
        assertTrue(stdout.contains("PERFORM SIGNAL HANDLER"));
        assertFalse(stdout.contains("SECOND SHOULD NOT RUN"));
        assertFalse(stdout.contains("MAIN AFTER PERFORM"));
    }
}
