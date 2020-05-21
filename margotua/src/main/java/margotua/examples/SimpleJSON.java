package margotua.examples;

import org.json.simple.JSONValue;
import margotua.util.DeepSimpleJSONTainter;

import java.util.*;
import java.util.stream.Collectors;

public class SimpleJSON{
    public static Object createObj() {
        ArrayList list = new ArrayList();
        String test = "test";
        list.add("test");
        Map obj = new HashMap();
        obj.put("a", 23.42);
        obj.put("b", "aA0./,()");
        ArrayList nested = new ArrayList();
        nested.add(new Long(4000));
        nested.add(new Boolean(true));
        nested.add('x');
        nested.add(new HashMap<>());
        obj.put("nested", nested);
        list.add(obj);
        list.add(new ArrayList<>());

        return list;
    }

    public static Object createSimpleInjectedObj(String inj) {
        Map obj = new HashMap();
        Map nested = new HashMap();
        ArrayList arr = new ArrayList();
        arr.add("nested" + inj);
        arr.add(nested);
        obj.put("array", arr);
        return obj;
    }

    public static Object createSimpleObj() {
        return createSimpleInjectedObj("");
    }

    public static String run(Object input) {
        String out = JSONValue.toJSONString(input);
        System.out.println(out);
        return out;
    }

    public static void main(String[] args) {
        System.err.println("start = " + java.time.LocalTime.now());
        System.err.println(Arrays.toString(args));
        Object list = createSimpleObj();
        System.err.println("object created = " + java.time.LocalTime.now());
        DeepSimpleJSONTainter tainter;
        if(args.length == 1 ) {
            tainter = new DeepSimpleJSONTainter(args[0]);
        } else {
            tainter = new DeepSimpleJSONTainter();
        }
        Object tainted = tainter.taint(list);
        System.err.println("tainted = " + java.time.LocalTime.now());
        run(tainted);
        System.err.println("done = " + java.time.LocalTime.now());
    }

    public static String encodeStructure(Object parsedJson) {
        StringBuilder builder = new StringBuilder();
        if(parsedJson instanceof Map) {
            builder.append("M{");
            List<Map.Entry<String, Object>> descendants = ((Map<String, Object>) parsedJson).entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).collect(Collectors.toList());
            for(Map.Entry<String, Object> descendant: descendants) {
                builder.append(encodeStructure(descendant.getValue()));
                builder.append(",");
            }
            builder.append("}");
        } else if (parsedJson instanceof Collection) {
            builder.append("C[");
            for(Object member: (Collection)parsedJson) {
                builder.append(encodeStructure(member));
                builder.append(",");
            }
            builder.append("]");
        } else if (parsedJson instanceof String) {
            builder.append("S");
        } else {
            System.err.println(parsedJson.getClass().toString());
        }

        return builder.toString();
    }
}
