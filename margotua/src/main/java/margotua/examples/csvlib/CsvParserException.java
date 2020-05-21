package margotua.examples.csvlib;

public class CsvParserException extends RuntimeException {

    private static final long serialVersionUID = -5454738869227879452L;

    public CsvParserException(String msg) {
        super(msg);
    }

    public CsvParserException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
