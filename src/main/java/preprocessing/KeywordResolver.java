package preprocessing;

import java.util.*;
import java.util.regex.*;

/**
 * KeywordResolver implements BabyCobol's context-sensitive keyword disambiguation feature.
 * 
 * Problem: Keywords (ADD, TO, FROM, etc.) can be used as field names in BabyCobol.
 * Solution: Case indicates intent - uppercase = keyword, lowercase = possibly a field.
 * 
 * Algorithm: Find all sequences of lowercase potential keywords and determine if they
 * create ambiguity. If unambiguous, auto-fix. If ambiguous, throw error.
 */
public class KeywordResolver {

    // Define all keywords in BabyCobol grammar
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "ADD", "TO", "FROM", "SUBTRACT", "DIVIDE", "MULTIPLY", "GIVING",
        "DISPLAY", "ACCEPT", "MOVE", "IF", "THEN", "ELSE", "STOP",
        "IDENTIFICATION", "DIVISION", "PROGRAM-ID", "DATA", "PROCEDURE",
        "PICTURE", "IS", "LIKE", "OCCURS", "TIMES", "WITH", "NO", "ADVANCING",
        "PERFORM", "THROUGH", "MOVESTAT"
        // ... add all keywords from BabyCobol.g4
    ));

    public static String resolve(String code) throws AmbiguityException {
        // STEP 1: Extract declared field names from DATA DIVISION
        Set<String> declaredFields = extractDeclaredFields(code);
        
        // STEP 2: Find all lowercase potential keywords in PROCEDURE DIVISION
        String procedureDivision = extractProcedureDivision(code);
        List<LowercaseKeyword> lowercaseKeywords = findLowercaseKeywords(procedureDivision);
        
        // STEP 3: If no ambiguous lowercase keywords, return as-is
        if (lowercaseKeywords.isEmpty()) {
            return code;
        }
        
        // STEP 4: Generate all possible "upgrade combinations"
        // Each combination represents upgrading different subsets of lowercase keywords to uppercase
        List<String> possibleUpgrades = generateAllUpgradeCombinations(code, lowercaseKeywords);
        
        // STEP 5: For each possible upgrade, count how many create valid parses
        List<String> validParses = new ArrayList<>();
        for (String upgrade : possibleUpgrades) {
            if (isValidParse(upgrade)) {
                validParses.add(upgrade);
            }
        }
        
        // STEP 6: Decision based on valid parse count
        if (validParses.size() == 0) {
            throw new AmbiguityException("No valid parse exists for given code");
        } else if (validParses.size() == 1) {
            // Unambiguous - auto-fix
            return validParses.get(0);
        } else {
            // AMBIGUOUS - multiple valid parses
            StringBuilder error = new StringBuilder();
            error.append("Ambiguous code: multiple valid interpretations exist:\n");
            for (int i = 0; i < validParses.size(); i++) {
                error.append("  Interpretation ").append(i + 1).append(": ")
                    .append(validParses.get(i)).append("\n");
            }
            throw new AmbiguityException(error.toString());
        }
    }

    // ======================== PSEUDOCODE FUNCTIONS ========================

    /**
     * PSEUDOCODE: Extract all field names declared in DATA DIVISION
     * 
     * Algorithm:
     *   1. Find "DATA DIVISION." marker
     *   2. Read until "PROCEDURE DIVISION."
     *   3. Parse all "01 FIELDNAME" declarations
     *   4. Return set of field names (uppercase for comparison)
     */
    private static Set<String> extractDeclaredFields(String code) {
        Set<String> fields = new HashSet<>();
        
        // PSEUDO: Find DATA DIVISION section
        // PSEUDO: Use regex to extract lines like "01 FIELDNAME PICTURE..."
        // PSEUDO: Add each FIELDNAME to fields set
        
        // PLACEHOLDER IMPLEMENTATION
        try {
            int dataStart = code.toUpperCase().indexOf("DATA DIVISION");
            int procedureStart = code.toUpperCase().indexOf("PROCEDURE DIVISION");
            
            if (dataStart == -1 || procedureStart == -1) {
                return fields;
            }
            
            String dataSection = code.substring(dataStart, procedureStart);
            
            // PSEUDO: Find pattern "01 FIELDNAME"
            Pattern pattern = Pattern.compile("01\\s+([A-Z0-9-]+)\\s+PICTURE");
            Matcher matcher = pattern.matcher(dataSection);
            
            while (matcher.find()) {
                fields.add(matcher.group(1).toUpperCase());
            }
        } catch (Exception e) {
            // PSEUDO: Log error and return empty set
        }
        
        return fields;
    }

    /**
     * PSEUDOCODE: Extract PROCEDURE DIVISION from code
     * 
     * Algorithm:
     *   1. Find "PROCEDURE DIVISION." marker
     *   2. Extract everything after it until EOF
     *   3. Return as string
     */
    private static String extractProcedureDivision(String code) {
        // PSEUDO: Find index of "PROCEDURE DIVISION."
        // PSEUDO: Return substring from that point to end
        
        int procedureStart = code.toUpperCase().indexOf("PROCEDURE DIVISION");
        if (procedureStart == -1) {
            return "";
        }
        return code.substring(procedureStart);
    }

    /**
     * PSEUDOCODE: Find all lowercase potential keywords
     * 
     * Algorithm:
     *   1. Tokenize the code into words (split by whitespace, punctuation)
     *   2. For each word:
     *        a. Check if it's inside a string (skip if yes)
     *        b. Get uppercase version
     *        c. Check if uppercase version is in KEYWORDS set
     *        d. If yes AND word is lowercase -> add to results with position info
     *   3. Return list of lowercase keywords with their positions
     */
    private static List<LowercaseKeyword> findLowercaseKeywords(String code) {
        List<LowercaseKeyword> results = new ArrayList<>();
        
        // PSEUDO: Split code into tokens preserving position
        // PSEUDO: For each token:
        //   if token.uppercase() is in KEYWORDS AND token is lowercase:
        //     results.add(new LowercaseKeyword(token, position))
        
        // PLACEHOLDER: Basic tokenization
        String[] tokens = code.split("[\\s.()\"]+");
        int position = 0;
        boolean inString = false;
        
        for (String token : tokens) {
            if (token.isEmpty()) continue;
            
            // PSEUDO: Track if inside string
            if (token.contains("\"")) {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                String uppercase = token.toUpperCase();
                if (KEYWORDS.contains(uppercase) && !token.equals(uppercase)) {
                    // Found lowercase keyword
                    results.add(new LowercaseKeyword(token, position));
                }
            }
            
            position += token.length() + 1;
        }
        
        return results;
    }

    /**
     * PSEUDOCODE: Generate all possible "upgrade combinations"
     * 
     * Algorithm:
     *   Given N lowercase keywords, generate 2^N combinations:
     *   - Combination 0: upgrade none
     *   - Combination 1: upgrade keyword[0] only
     *   - Combination 2: upgrade keyword[1] only
     *   - Combination 3: upgrade keywords[0] and keywords[1]
     *   - ... (all 2^N possibilities)
     *   
     *   For each combination:
     *     1. Copy original code
     *     2. For each keyword in the "upgrade set", replace lowercase with uppercase
     *     3. Add modified code to results
     *   
     *   Return all modified versions
     */
    private static List<String> generateAllUpgradeCombinations(String code, List<LowercaseKeyword> keywords) {
        List<String> combinations = new ArrayList<>();
        int totalCombos = 1 << keywords.size(); // 2^N
        
        // PSEUDO: For i from 0 to 2^keywords.size() - 1:
        //   currentCombination = ""
        //   For j from 0 to keywords.size() - 1:
        //     if bit j is set in i:
        //       include keywords[j] in this combination
        //   modified_code = applyUpgrades(code, currentCombination)
        //   combinations.add(modified_code)
        
        for (int i = 0; i < totalCombos; i++) {
            String modified = code;
            
            // PSEUDO: For each bit in the bitmask
            for (int j = 0; j < keywords.size(); j++) {
                if ((i & (1 << j)) != 0) {
                    // Bit j is set - upgrade this keyword
                    LowercaseKeyword kw = keywords.get(j);
                    String uppercase = kw.word.toUpperCase();
                    modified = modified.replaceAll("\\b" + Pattern.quote(kw.word) + "\\b", uppercase);
                }
            }
            
            combinations.add(modified);
        }
        
        return combinations;
    }

    /**
     * PSEUDOCODE: Check if a given code string is a valid BabyCobol parse
     * 
     * Algorithm:
     *   1. Run the code through ANTLR parser
     *   2. Try to build parse tree
     *   3. If no parse tree exception -> return true
     *   4. If ParseException thrown -> return false
     */
    private static boolean isValidParse(String code) {
        // PSEUDO: Try parsing code with BabyCobolParser
        // PSEUDO: Return true if successful, false if exception
        
        try {
            // PSEUDO: Parse with BabyCobolParserUtils or direct ANTLR call
            // BabyCobolParserUtils.parseTree(code);
            return true; // PLACEHOLDER
        } catch (Exception e) {
            return false;
        }
    }

    // ======================== HELPER CLASSES ========================

    /**
     * Helper class to track lowercase keywords and their positions
     */
    private static class LowercaseKeyword {
        String word;
        int position;
        
        LowercaseKeyword(String word, int position) {
            this.word = word;
            this.position = position;
        }
    }

    /**
     * Custom exception for ambiguous keyword resolution
     */
    public static class AmbiguityException extends Exception {
        public AmbiguityException(String message) {
            super(message);
        }
    }
}
