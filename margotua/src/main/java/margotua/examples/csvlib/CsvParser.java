package margotua.examples.csvlib;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * CSV format parser.
 * 
 * <ul>
 * <li> CSV format is specified by <a href="http://tools.ietf.org/html/rfc4180">RFC 4180</a>.
 * <li> Each line should end with CRLF (but CR or LF will do too).
 * <li> A line consists of fields delimited with "field separator" (usually a comma).
 * <li> Complex field values, which contain field separators or which span through 
 * multiple lines, could be surrounded with double quotes (or single quotes, or any 
 * other char, depending on configuration).
 * </ul>
 * 
 * @author Vitaliy Garnashevich
 */
public class CsvParser {

    public static final char FIELD_SEP_SEMICOLON = ';';
    public static final char FIELD_SEP_COMMA = ',';
    public static final char FIELD_SEP_TAB = '\t';
    public static final char FIELD_SEP_BAR = '|';
    
    public static final char QUOTE_SINGLE = '\'';
    public static final char QUOTE_DOUBLE = '"';
    
    
    private final char fieldSep;
    private final char quoteChar;

    public CsvParser() {
        this(FIELD_SEP_COMMA, QUOTE_DOUBLE);
    }
    
    public CsvParser(char fieldSeparator) {
        this(fieldSeparator, QUOTE_DOUBLE);
    }
    
    public CsvParser(char fieldSeparator, char quoteChar) {
        this.fieldSep = fieldSeparator;
        this.quoteChar = quoteChar;
    }

    public char getFieldSeparator() {
        return fieldSep;
    }

    public char getQuoteChar() {
        return quoteChar;
    }

    /**
     * Parse CSV file as a set of {@link Map}s. Note that the first line is required 
     * to be the header. Header is parsed automatically, in order to know map keys.
     * <p> Don't forget to close the reader when it is no longer needed.
     */
    public Iterator<Map<String, String>> parseAsMaps(Reader reader) {
        Iterator<List<String>> iter = parseAsLists(reader);
        if (!iter.hasNext()) {
            throw new CsvParserException("CSV header expected");
        }
        List<String> header = iter.next();
        return new MapsIterator(iter, header);
    }
    
    /**
     * Parse CSV file as a set of {@link List}s. Note that the first line could be a header.
     * <p> Don't forget to close the reader when it is no longer needed.
     */
    public Iterator<List<String>> parseAsLists(Reader reader) {
        return new ListsIterator(reader);
    }
    
    private abstract class AbstractIterator<T> implements Iterator<T> {
        private T next;
        private boolean done;

        @Override
        public boolean hasNext() {
            if (done) {
                return false;
            }
            if (next == null) {
                next = getNext();
                if (next == null) {
                    done = true;
                    return false;
                }
            }
            return true;
        }

        protected abstract T getNext();

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            T result = next;
            next = null;
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
    }
    
    private class MapsIterator extends AbstractIterator<Map<String, String>> {

        private final Iterator<List<String>> iter;
        private final List<String> header;

        public MapsIterator(Iterator<List<String>> iter, List<String> header) {
            this.iter = iter;
            this.header = header;
        }

        @Override
        protected Map<String, String> getNext() {
            if (!iter.hasNext()) {
                return null;
            }
            Iterator<String> values = iter.next().iterator();
            Iterator<String> heads = header.iterator();
            
            Map<String, String> result = new LinkedHashMap<String, String>();
            while (heads.hasNext()) {
                String head = heads.next();
                String value = values.hasNext() ? values.next() : "";
                result.put(head, value);
            }
            return result;
        }

    }
    
    private class ListsIterator extends AbstractIterator<List<String>> {
        private final Scanner scanner;

        private ListsIterator(Reader reader) {
            this.scanner = new Scanner(reader);
        }

        @Override
        protected List<String> getNext() {
            return parseNextLine(scanner);
        }
        
    }

    private List<String> parseNextLine(Scanner scanner) {
        try {
            List<String> result = new LinkedList<String>();
            if (scanner.isEof()) {
                return null;
            }
            while (parseValue(scanner, result)) {
                // empty
            }
            return result;
        } catch (IOException e) {
            throw new CsvParserException("Failed to parse CSV file", e);
        }
    }

    /**
     * Returns <code>false</code> when end of line reached.
     */
    private boolean parseValue(Scanner scanner, List<String> result) throws IOException {
        StringBuilder builder = new StringBuilder();
        int ch;
        while ((ch = scanner.read()) != -1) {
            if (ch == fieldSep) {
                result.add(builder.toString());
                return true;
            } else if (ch == quoteChar) {
                parseQuotedValue(scanner, builder);
            } else if (ch == 13) {
                ch = scanner.read();
                if (ch != 10) {
                    scanner.unread(ch);
                }
                break;
            } else if (ch == 10) {
                break;
            } else {
                builder.append((char) ch);
            }
        }
        // end of line (or file) reached
        result.add(builder.toString());
        return false;
    }

    private void parseQuotedValue(Scanner scanner, StringBuilder builder) throws IOException {
        int ch;
        while ((ch = scanner.read()) != -1) {
            if (ch == quoteChar) {
                ch = scanner.read();
                if (ch != quoteChar) {
                    scanner.unread(ch);
                    break;
                }
            }
            builder.append((char) ch);
        }
    }

    /**
     * Allows reading by one character. 
     * Allows "unreading" at most one character.
     */
    private static class Scanner {
        
        private final Reader reader;
        private int ch = -1;

        public Scanner(Reader reader) {
            this.reader = reader;
        }
        
        public int read() throws IOException {
            if (ch == -1) {
                return reader.read();
            }
            
            int result = ch;
            ch = -1;
            return result;
        }
        
        public void unread(int c) {
            if (ch != -1) {
                throw new IllegalStateException();
            }
            ch = c;
        }
        
        public boolean isEof() throws IOException {
            ch = this.read();
            return ch == -1;
        }
    }
    
}
