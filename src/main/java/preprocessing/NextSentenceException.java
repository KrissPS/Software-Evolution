package preprocessing;

/**
 * Exception thrown to signal a NEXT SENTENCE control flow.
 * Used to exit the current sentence and proceed to the next one.
 * Caught at the sentence execution level in BabyCobolInterpreter.
 */
public class NextSentenceException extends RuntimeException {
}
