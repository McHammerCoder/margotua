package margotua.examples;


import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import io.mola.galimatias.GalimatiasParseException;
import io.mola.galimatias.URL;

import java.util.Arrays;

public class GalimatiasUrl {
    public static URL createSimpleObj(String pathInj) throws GalimatiasParseException {
        String scheme = new String(MultiTainter.taintedCharArray("http".toCharArray(), 1));
        String user = new String(MultiTainter.taintedCharArray("user".toCharArray(), 2));
        String pass = new String(MultiTainter.taintedCharArray("pass".toCharArray(), 3));
        String host = new String(MultiTainter.taintedCharArray("example.com".toCharArray(), 4));
        String path = new String(MultiTainter.taintedCharArray(("path"+pathInj).toCharArray(), 5));
        String query = new String(MultiTainter.taintedCharArray("query".toCharArray(), 6));
        String fragment = new String(MultiTainter.taintedCharArray("fragment".toCharArray(), 7));
        return URL.parse(scheme+"://"+user+":"+pass+"@"+host+"/"+path+"?"+query+"#"+fragment);
    }

    public static String run(URL uri) {
        String out = uri.toString();
        System.out.println(out);
        return out;
    }

    public static void main(String[] args) throws Exception {
        System.err.println("start = " + java.time.LocalTime.now());
        System.err.println(Arrays.toString(args));
        //Object list = createObj();
        URL url;
        if(args.length == 1) {
            url = createSimpleObj(args[0]);
        } else {
            url = createSimpleObj("");
        }
        System.err.println("object created = " + java.time.LocalTime.now());
        run(url);
        System.err.println("done = " + java.time.LocalTime.now());
    }
}
