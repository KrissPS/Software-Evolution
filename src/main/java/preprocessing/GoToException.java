package preprocessing;

public class GoToException extends RuntimeException {

    private final String target;

    public GoToException(String target) {
        this.target = target;
    }

    public String getTarget() {
        return target;
    }
}
