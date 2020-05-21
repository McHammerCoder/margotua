package margotua.examples.csvlib;

import static java.util.Arrays.*;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

public class Test {

    public static void main(String[] args) throws Exception {
        StringWriter out = new StringWriter();
        CsvWriter writer = new CsvWriter(out);
        writer.write(asList("1", "2\"3", "4,5", "6\r7", "8\n9", "0\r\n1", "2"));
        writer.write(asList("1", "2\"3", "4,5", "6\r7", "8\n9", "0\r\n1", "2"));
        writer.write(asList("1", "2\"3", "4,5", "6\r7", "8\n9", "0\r\n1", "2"));
        System.out.println(out);
        System.out.println();
        
        CsvParser parser = new CsvParser();
        Iterator<List<String>> iter = parser.parseAsLists(new StringReader(out.toString()));
        while (iter.hasNext()) {
            System.out.println(iter.next());
        }
    }

}
