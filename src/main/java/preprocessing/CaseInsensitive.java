package preprocessing;

public class CaseInsensitive {

    public static String process(String text) {
        StringBuilder result = new StringBuilder();

        boolean insideString = false;
        boolean preservingProgramIdValue = false;
        boolean waitingProgramIdDot = false;

        boolean waitingPictureIs = false;
        boolean preservingPictureValue = false;
        boolean pictureValueStarted = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Preserve Strings
            if (c == '"') {
                insideString = !insideString;
                result.append(c);
                continue;
            }

            if (insideString) {
                result.append(c);
                continue;
            }

            // Preserve PROGRAM-ID value until final dot
            if (preservingProgramIdValue) {
                result.append(c);
                if (c == '.') {
                    preservingProgramIdValue = false;
                }
                continue;
            }

            if (waitingProgramIdDot) {
                result.append(Character.toUpperCase(c));
                if (c == '.') {
                    waitingProgramIdDot = false;
                    preservingProgramIdValue = true;
                }
                continue;
            }

            // Preserve PICTURE representation
            if (preservingPictureValue) {
                if (c == '.') {
                    result.append(c);
                    preservingPictureValue = false;
                    pictureValueStarted = false;
                    continue;
                }

                if (pictureValueStarted && Character.isWhitespace(c)) {
                    result.append(c);
                    preservingPictureValue = false;
                    pictureValueStarted = false;
                    continue;
                }

                pictureValueStarted = true;
                result.append(c);
                continue;
            }

            if (startsWithIgnoreCase(text, i, "PROGRAM-ID")) {
                result.append("PROGRAM-ID");
                i += "PROGRAM-ID".length() - 1;
                waitingProgramIdDot = true;
                continue;
            }

            if (startsWithIgnoreCase(text, i, "PICTURE")) {
                result.append("PICTURE");
                i += "PICTURE".length() - 1;
                waitingPictureIs = true;
                continue;
            }

            if (waitingPictureIs && startsWithIgnoreCase(text, i, "IS")) {
                result.append("IS");
                i += "IS".length() - 1;
                waitingPictureIs = false;
                preservingPictureValue = true;
                pictureValueStarted = false;
                continue;
            }

            result.append(Character.toUpperCase(c));
        }

        return result.toString();
    }

    private static boolean startsWithIgnoreCase(String text, int index, String target) {
        if (index + target.length() > text.length()) {
            return false;
        }

        return text.regionMatches(true, index, target, 0, target.length());
    }
}