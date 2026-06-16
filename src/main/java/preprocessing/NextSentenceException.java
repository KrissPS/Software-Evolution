package preprocessing;

/**
 * exception thrown to signal a NEXT SENTENCE control flow
 * used to exit the current sentence and proceed to the next one
 * xaught at the sentence execution level in BabyCobolInterpreter
 */
public class NextSentenceException extends RuntimeException {
}
