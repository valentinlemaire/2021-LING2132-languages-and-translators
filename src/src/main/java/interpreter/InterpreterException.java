package interpreter;

public final class InterpreterException extends RuntimeException {
    public InterpreterException (String message, Throwable cause) {
        super(message, cause);
    }
}
