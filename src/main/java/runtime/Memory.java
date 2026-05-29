package runtime;

import java.util.HashMap;
import java.util.Map;

public class Memory {

    private final Map<String, FieldInfo> fields = new HashMap<>();

    public void declare(String name, String pictureRaw) {
        String normalizedName = name.toUpperCase();
        PictureSpec picture = PictureParser.parse(pictureRaw);

        fields.put(normalizedName, new FieldInfo(normalizedName, picture));
    }

    public void declareLike(String name, String existingName) {
        FieldInfo existing = get(existingName);
        fields.put(name.toUpperCase(), new FieldInfo(name.toUpperCase(), existing.picture()));
    }

    public FieldInfo get(String name) {
        FieldInfo field = fields.get(name.toUpperCase());

        if (field == null) {
            throw new IllegalArgumentException("Unknown field: " + name);
        }

        return field;
    }

    public void set(String name, String value) {
        get(name).setValue(value);
    }

    public String getValue(String name) {
        return get(name).value();
    }

    public boolean exists(String name) {
        return fields.containsKey(name.toUpperCase());
    }
}