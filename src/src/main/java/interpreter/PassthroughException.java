package interpreter;

final class PassthroughException extends RuntimeException {
    public PassthroughException (Throwable cause) {
        super(cause);
    }
}
