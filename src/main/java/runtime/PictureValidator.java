package runtime;

public class PictureValidator {

    public static boolean fits(String value, PictureSpec spec) {
        if (value == null) {
            return true;
        }

        return switch (spec.kind()) {
            case NUMERIC -> fitsNumeric(value, spec);
            case ALPHABETIC -> value.matches("[A-Za-z ]{0," + spec.maxLength() + "}");
            case ANY -> value.length() <= spec.maxLength();
        };
    }

    private static boolean fitsNumeric(String value, PictureSpec spec) {
        String regex;

        if (spec.signed()) {
            regex = "[+-]?";
        } else {
            regex = "";
        }

        if (spec.decimals() > 0) {
            int integerDigits = spec.maxLength() - spec.decimals();
            regex += "\\d{1," + integerDigits + "}(\\.\\d{1," + spec.decimals() + "})?";
        } else {
            regex += "\\d{1," + spec.maxLength() + "}";
        }

        return value.matches(regex);
    }
}