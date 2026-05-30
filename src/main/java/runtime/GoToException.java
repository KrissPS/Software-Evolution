package runtime;

public class GoToException extends RuntimeException {
    private final String target;

    public GoToException(String target) {
        this.target = target;
    }

    public String target() {
        return target;
    }
}
