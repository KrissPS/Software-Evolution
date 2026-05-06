package preprocessing;

public class CodeCleaner {

    public static String cleanCode(String codeFile) {
        String[] lines = codeFile.split("\\R", -1);
        StringBuilder cleanedCode = new StringBuilder();

        boolean firstCodeLine = true;

        for (String line : lines) {
            char indicator = line.length() >= 7 ? line.charAt(6) : ' ';

            String code = "";
            if (line.length() > 7) {
                code = line.substring(7, Math.min(line.length(), 72));
            }

            if (indicator == '*') {
                continue;
            }

            if (indicator == ' ') {
                if (!firstCodeLine) {
                    cleanedCode.append('\n');
                }

                cleanedCode.append(code);
                firstCodeLine = false;
            } else if (indicator == '-') {
                if (firstCodeLine) {
                    throw new IllegalArgumentException("Continuation line cannot be the first code line.");
                }

                cleanedCode.append(code);
            } else {
                throw new IllegalArgumentException("Invalid line status indicator: " + indicator);
            }
        }

        return cleanedCode.toString();
    }
}