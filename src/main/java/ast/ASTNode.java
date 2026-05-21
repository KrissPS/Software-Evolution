package ast;

import java.util.ArrayList;
import java.util.List;

public class ASTNode {
    private String type;
    private String text;
    private List<ASTNode> children = new ArrayList<>();

    public ASTNode(String type) {
        this.type = type;
    }

    public ASTNode(String type, String text) {
        this.type = type;
        this.text = text;
    }

    public void addChild(ASTNode child) {
        if (child != null) {
            this.children.add(child);
        }
    }

    public List<ASTNode> getChildren() {
        return children;
    }

    public String getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public String printTree() {
        return printTree(0);
    }

    private String printTree(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }
        sb.append(type);
        if (text != null && !text.isEmpty()) {
            sb.append(": ").append(text);
        }
        sb.append("\n");
        for (ASTNode child : children) {
            if (child != null) {
                sb.append(child.printTree(indent + 1));
            }
        }
        return sb.toString();
    }
}
