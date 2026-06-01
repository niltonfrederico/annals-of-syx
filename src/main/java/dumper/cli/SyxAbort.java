package dumper.cli;

/**
 * Thrown to abort the syx run with a user-facing message. Caught at the
 * top of {@link Syx} and printed to stderr without a stack trace. Use for
 * expected failure paths (missing install, unsupported version, etc.);
 * unexpected exceptions still bubble up with a trace.
 */
public final class SyxAbort extends RuntimeException {
    public SyxAbort(String message) { super(message); }
    public SyxAbort(String message, Throwable cause) { super(message, cause); }
}
