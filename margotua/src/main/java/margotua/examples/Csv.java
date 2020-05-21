package margotua.examples;

import margotua.examples.csvlib.CsvWriter;
import margotua.util.StringListTainter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Csv {
    private static class CsvRunner {
        private StringWriter writer;
        private CsvWriter csvWriter;
        List<String> input;
        public CsvRunner(List<String> input) {
            writer = new StringWriter();
            csvWriter = new CsvWriter(writer);
            this.input = input;
        }

        public String run() throws IOException {
            csvWriter.write(input);
            return writer.toString();
        }
    }

    public static List<String> createSimpleObj() {
        List<String> list = new ArrayList<>();
        list.add("first");
        list.add("2nd");
        list.add(",;rd");
        return list;
    }

    public static String run(CsvRunner runner) throws Exception {
        String out =  runner.run();
        System.out.println(out);
        return out;
    }

    public static void main(String[] args) throws Exception {
        System.err.println("start = " + java.time.LocalTime.now());
        System.err.println(Arrays.toString(args));
        //Object list = createObj();
        List<String> list = createSimpleObj();
        System.err.println("object created = " + java.time.LocalTime.now());
        StringListTainter tainter;
        if(args.length == 1 ) {
            tainter = new StringListTainter(args[0]);
        } else {
            tainter = new StringListTainter();
        }
        List<String> tainted = tainter.taint(list);
        System.err.println("tainted = " + java.time.LocalTime.now());
        CsvRunner runner = new CsvRunner(tainted);
        run(runner);
        System.err.println("done = " + java.time.LocalTime.now());
    }

}
