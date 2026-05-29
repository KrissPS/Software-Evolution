package runtime;

public class PictureParser {

    public static PictureSpec parse(String picture) {
        validateSingleSAndV(picture);

        String expanded = expand(picture);

        boolean signed = expanded.contains("S");
        int decimalIndex = expanded.indexOf('V');
        int decimals = decimalIndex >= 0
                ? expanded.length() - decimalIndex - 1
                : 0;

        String cleaned = expanded
                .replace("S", "")
                .replace("V", "");

        if (cleaned.matches("[9Z]+")) {
            return new PictureSpec(picture, PictureSpec.Kind.NUMERIC, cleaned.length(), signed, decimals);
        }

        if (cleaned.matches("A+")) {
            return new PictureSpec(picture, PictureSpec.Kind.ALPHABETIC, cleaned.length(), false, 0);
        }

        if (cleaned.matches("X+")) {
            return new PictureSpec(picture, PictureSpec.Kind.ANY, cleaned.length(), false, 0);
        }

        throw new IllegalArgumentException("Invalid PICTURE: " + picture);
    }

    private static void validateSingleSAndV(String picture) {
        long countS = picture.chars().filter(c -> c == 'S').count();
        long countV = picture.chars().filter(c -> c == 'V').count();

        if (countS > 1) {
            throw new IllegalArgumentException("PICTURE cannot contain more than one S: " + picture);
        }

        if (countV > 1) {
            throw new IllegalArgumentException("PICTURE cannot contain more than one V: " + picture);
        }
    }

    private static String expand(String picture) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < picture.length(); i++) {
            char c = picture.charAt(i);

            if (i + 1 < picture.length() && picture.charAt(i + 1) == '(') {
                int close = picture.indexOf(')', i + 1);
                int count = Integer.parseInt(picture.substring(i + 2, close));

                result.append(String.valueOf(c).repeat(count));
                i = close;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }
}