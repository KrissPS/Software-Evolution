package runtime;

public class PictureSpec {
    public enum Kind {
        NUMERIC,
        ALPHABETIC,
        ANY
    }

    private final String raw;
    private final Kind kind;
    private final int maxLength;
    private final boolean signed;
    private final int decimals;

    public PictureSpec(String raw, Kind kind, int maxLength, boolean signed, int decimals) {
        this.raw = raw;
        this.kind = kind;
        this.maxLength = maxLength;
        this.signed = signed;
        this.decimals = decimals;
    }

    public String raw() {
        return raw;
    }

    public Kind kind() {
        return kind;
    }

    public int maxLength() {
        return maxLength;
    }

    public boolean signed() {
        return signed;
    }

    public int decimals() {
        return decimals;
    }
}