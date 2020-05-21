package margotua.examples.csvlib;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * CSV format writer.
 * 
 * @author Vitaliy Garnashevich
 */
public class CsvWriter implements Closeable {

    public static final char FIELD_SEP_SEMICOLON = ';';
    public static final char FIELD_SEP_COMMA = ',';
    public static final char FIELD_SEP_TAB = '\t';
    public static final char FIELD_SEP_BAR = '|';
    
    public static final char QUOTE_SINGLE = '\'';
    public static final char QUOTE_DOUBLE = '"';
    
    public static final String EOL_CR = "\r";
    public static final String EOL_LF = "\n";
    public static final String EOL_CRLF = "\r\n";
    
    
    private final Writer writer;
    private final char fieldSep;
    private final char quoteChar;
    private final String lineSep;
    
    private final String fieldSepStr;
    private final String quoteCharStr;
    private final String doubleQuoteCharStr;
    private final String quoteCharPattern;

    public CsvWriter(Writer writer) {
        this(writer, FIELD_SEP_COMMA, QUOTE_DOUBLE, EOL_CRLF);
    }
    
    public CsvWriter(Writer writer, char fieldSeparator) {
        this(writer, fieldSeparator, QUOTE_DOUBLE, EOL_CRLF);
    }
    
    public CsvWriter(Writer writer, char fieldSeparator, char quoteChar, String lineSeparator) {
        this.writer = writer;
        this.fieldSep = fieldSeparator;
        this.quoteChar = quoteChar;
        this.lineSep = lineSeparator;
        
        this.fieldSepStr = String.valueOf(fieldSep);
        this.quoteCharStr = String.valueOf(quoteChar);
        this.doubleQuoteCharStr = quoteChar + "" + quoteChar;
        this.quoteCharPattern = "\\" + quoteChar;
    }

    public char getFieldSeparator() {
        return fieldSep;
    }

    public char getQuoteChar() {
        return quoteChar;
    }

    public String getLineSeparator() {
        return lineSep;
    }

    /**
     * Close underlying writer.
     */
    @Override
    public void close() throws IOException {
        writer.close();
    }
    
    /**
     * Write row of values as CSV.
     */
    public String write(List<String> list) throws IOException {
        boolean isFirst = true;
        for (String value : list) {
            if (!isFirst) {
                writer.write(fieldSepStr);
            }
            isFirst = false;
            
            if (value != null) {
                if (value.contains(fieldSepStr) 
                        || value.contains(quoteCharStr)
                        || value.contains(EOL_CR)
                        || value.contains(EOL_LF)) {
                    writer.write(quoteCharStr);
                    writer.write(value.replaceAll(quoteCharPattern, doubleQuoteCharStr));
                    writer.write(quoteCharStr);
                } else {
                    writer.write(value);
                }
            }
        }
        writer.write(lineSep);
        return writer.toString();
    }

}



