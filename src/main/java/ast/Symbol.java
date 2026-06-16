package ast;

public class Symbol {
    private String name;
    private int level;
    private String picture;
    private String like;
    private int occurs;
    private String parentName; // name of the parent record, null if top level

    public Symbol(String name, int level, String picture, String like, int occurs) {
        this(name, level, picture, like, occurs, null);
    }

    public Symbol(String name, int level, String picture, String like, int occurs, String parentName) {
        this.name = name;
        this.level = level;
        this.picture = picture;
        this.like = like;
        this.occurs = occurs;
        this.parentName = parentName;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public String getPicture() {
        return picture;
    }

    public String getLike() {
        return like;
    }

    public int getOccurs() {
        return occurs;
    }

    public String getParentName() {
        return parentName;
    }

    public boolean isRecord() {
        return (picture == null || picture.isEmpty()) && (like == null || like.isEmpty());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-20s | Level: %-2d", name, level));
        if (picture != null && !picture.isEmpty()) {
            sb.append(" | Picture: ").append(picture);
        }
        if (like != null && !like.isEmpty()) {
            sb.append(" | Like: ").append(like);
        }
        if (occurs > 0) {
            sb.append(" | Occurs: ").append(occurs);
        }
        if (parentName != null && !parentName.isEmpty()) {
            sb.append(" | Parent: ").append(parentName);
        }
        return sb.toString();
    }
}