package flamegrapher.backend;

public class ProfilerError extends RuntimeException {
    private static final long serialVersionUID = 5761954298398117234L;

    public ProfilerError(String message) {
        super(message);
    }
}
