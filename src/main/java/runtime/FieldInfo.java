package runtime;

public class FieldInfo {
    private final String name;
    private final PictureSpec picture;
    private String value;

    public FieldInfo(String name, PictureSpec picture) {
        this.name = name;
        this.picture = picture;
        this.value = defaultValue(picture);
    }

    public String name() {
        return name;
    }

    public PictureSpec picture() {
        return picture;
    }

    public String value() {
        return value;
    }

    public void setValue(String value) {
        if (!PictureValidator.fits(value, picture)) {
            throw new IllegalArgumentException(
                    "Value '" + value + "' does not fit PICTURE " + picture.raw() + " for field " + name
            );
        }

        this.value = value;
    }

    private static String defaultValue(PictureSpec picture) {
        return switch (picture.kind()) {
            case NUMERIC -> "0";
            case ALPHABETIC, ANY -> "";
        };
    }
}