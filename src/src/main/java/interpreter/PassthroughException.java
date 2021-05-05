package interpreter;

public final class PassthroughException extends RuntimeException {
    public PassthroughException (Throwable cause) {
        super(cause);
    }
}
