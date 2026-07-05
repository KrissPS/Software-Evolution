package preprocessing;

public class GoToException extends RuntimeException {
    private final String target;

    public GoToException(String target) {
        super("GO TO " + target);
        this.target = target;
    }

    public String getTarget() {
        return target;
    }
}
