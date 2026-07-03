package preprocessing;

public class StopProgramException extends RuntimeException {

    public StopProgramException() {
        super();
    }

    public StopProgramException(String message) {
        super(message);
    }
}