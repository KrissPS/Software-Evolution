package preprocessing;

import java.util.*;
import java.util.regex.*;

/**
 * Resolves context-sensitive keyword disambiguation in BabyCobol.
 * 
 * Problem: Keywords can be declared as field names.
 * Solution: Case indicates intent - UPPERCASE = keyword, lowercase = field.
 * 
 * Algorithm:
 * 1. Find all lowercase words matching keyword names
 * 2. Try all 2^N combinations of uppercasing them
 * 3. Parse each combination
 * 4. If 1 valid parse → auto-fix; if 0 or >1 → error
 */
public class KeywordResolver {

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "IDENTIFICATION", "PROGRAM-ID", "DATA", "PROCEDURE", "DIVISION",
            "ACCEPT", "ADD", "MULTIPLY", "BY", "TO", "GIVING", "DIVIDE", "INTO",
            "REMAINDER", "EVALUATE", "ALSO", "END", "WHEN", "OTHER", "THROUGH",
            "IF", "THEN", "ELSE", "AND", "OR", "NOT", "SUBTRACT", "FROM",
            "MOVE", "PERFORM", "STOP", "PICTURE", "IS", "LIKE", "OF", "OCCURS", "TIMES",
            "DISPLAY", "WITH", "NO", "ADVANCING", "ALTER", "PROCEED",
            "SIGNAL", "OFF", "ON", "ERROR"));

    public static String resolve(String code) throws AmbiguityException {
        List<LowercaseKeywordOccurrence> lowercaseKeywords = findLowercaseKeywords(code);

        if (lowercaseKeywords.isEmpty()) {
            return code;
        }

        // Try all 2^N combinations of uppercasing
        List<String> validParses = new ArrayList<>();
        int totalCombinations = 1 << lowercaseKeywords.size();

        for (int mask = 0; mask < totalCombinations; mask++) {
            String candidate = code;

            for (int i = 0; i < lowercaseKeywords.size(); i++) {
                if ((mask & (1 << i)) != 0) {
                    LowercaseKeywordOccurrence kw = lowercaseKeywords.get(i);
                    candidate = upgradeKeywordAt(candidate, kw.word, kw.startIndex);
                }
            }

            if (isValidParse(candidate)) {
                validParses.add(candidate);
            }
        }

        if (validParses.size() == 1) {
            return validParses.get(0);
        } else if (validParses.isEmpty()) {
            throw new AmbiguityException("No valid parse exists for:\n" + code);
        } else {
            StringBuilder error = new StringBuilder("Ambiguous - multiple valid parses:\n");
            for (int i = 0; i < validParses.size(); i++) {
                error.append("  ").append(i + 1).append(": ").append(validParses.get(i)).append("\n");
            }
            throw new AmbiguityException(error.toString());
        }
    }

    private static List<LowercaseKeywordOccurrence> findLowercaseKeywords(String code) {
        List<LowercaseKeywordOccurrence> results = new ArrayList<>();
        Pattern wordPattern = Pattern.compile("[a-zA-Z][a-zA-Z0-9-]*");
        Matcher matcher = wordPattern.matcher(code);

        while (matcher.find()) {
            String word = matcher.group();
            String upper = word.toUpperCase();

            if (KEYWORDS.contains(upper) && !word.equals(upper)) {
                results.add(new LowercaseKeywordOccurrence(word, matcher.start()));
            }
        }

        return results;
    }

    private static String upgradeKeywordAt(String code, String word, int startIndex) {
        return code.substring(0, startIndex) + word.toUpperCase() + code.substring(startIndex + word.length());
    }

    private static boolean isValidParse(String code) {
        try {
            BabyCobolParserUtils.parseTree(code);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static class LowercaseKeywordOccurrence {
        String word;
        int startIndex;

        LowercaseKeywordOccurrence(String word, int startIndex) {
            this.word = word;
            this.startIndex = startIndex;
        }
    }

    public static class AmbiguityException extends Exception {
        public AmbiguityException(String message) {
            super(message);
        }
    }
}
