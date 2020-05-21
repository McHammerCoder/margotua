package margotua.tracing;

public class ConcretizationException extends Exception{
    public ConcretizationException(String message) {
        super(message);
    }
    public ConcretizationException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
