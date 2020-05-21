package margotua.util;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class DeepSimpleJSONTainter {
    private int ctr;
    private String replacement;

    public DeepSimpleJSONTainter(String replacement) {
        this.replacement = replacement;
        ctr = 1;
    }

    public DeepSimpleJSONTainter() {
        this(null);
    }


    private Taint nextTaint() {
        return new Taint(Integer.toString(ctr++));
    }

    public Object taint(Object object) {
        if(object == null){
            return null;
        }

        System.err.println(" >> Tainting: " +Integer.toString(ctr)+" -> "+ object.toString() + " :: " + object.getClass().getName());

        if(object instanceof String){
            String s;
            if(replacement != null) {
                s = new String(replacement);
            } else {
                s = (String) object;
            }
            return new String(MultiTainter.taintedCharArray(s.toCharArray(), nextTaint()));
        }

        if(object instanceof Double){
            return MultiTainter.taintedDouble((Double)object, nextTaint());
        }

        if(object instanceof Float){
            return MultiTainter.taintedFloat((Float)object, nextTaint());
        }

        if(object instanceof Integer){
            return MultiTainter.taintedInt((Integer) object, nextTaint());
        }

        if(object instanceof Long){
            return MultiTainter.taintedLong((Long) object, nextTaint());
        }

        if(object instanceof Short){
            return MultiTainter.taintedShort((Short) object, nextTaint());
        }

        if(object instanceof Character){
            return MultiTainter.taintedChar((Character) object, nextTaint());
        }

        if(object instanceof Boolean){
            return MultiTainter.taintedBoolean((Boolean)object, nextTaint());
        }
        if(object instanceof Map){
            MultiTainter.taintedObject(object, nextTaint());
            Set<Map.Entry> entries = ((Map)object).entrySet();
            for(Map.Entry entry : entries) {
                assert(entry.getKey() instanceof String);
                System.err.println(" >> Tainting: " +Integer.toString(ctr)+" -> "+ entry.getKey().toString() + " :: " + entry.getKey().getClass().getName());
                MultiTainter.taintedObject(entry.getKey(), nextTaint());
                entry.setValue(taint(entry.getValue()));
            }
            return object;
        }

        if(object instanceof Collection){
            MultiTainter.taintedObject(object, nextTaint());
            Collection collection = (Collection)object;
            ArrayList replacement = new ArrayList();
            for(Object item : collection) {
                replacement.add(taint(item));
            }
            collection.clear();
            collection.addAll(replacement);
            return collection;
        }

        if(object instanceof byte[]){
            return MultiTainter.taintedByteArray((byte[])object, nextTaint());
        }

        if(object instanceof short[]){
            return MultiTainter.taintedShortArray((short[])object, nextTaint());
        }

        if(object instanceof int[]){
            return MultiTainter.taintedIntArray((int[])object, nextTaint());
        }

        if(object instanceof long[]){
            return MultiTainter.taintedLongArray((long[])object, nextTaint());
        }

        if(object instanceof float[]){
            return MultiTainter.taintedFloatArray((float[])object, nextTaint());
        }

        if(object instanceof double[]){
            return MultiTainter.taintedDoubleArray((double[])object, nextTaint());
        }

        if(object instanceof boolean[]){
            return MultiTainter.taintedBooleanArray((boolean[])object, nextTaint());
        }

        if(object instanceof char[]){
            return MultiTainter.taintedCharArray((char[])object, nextTaint());
        }

        if(object instanceof Object[]){
            Object[] arr = (Object[]) object;
            for(int i = 0; i<arr.length; ++i) {
                arr[i] = taint(arr[i]);
            }
            return arr;
        }

        throw new AssertionError("Invalid object type: " + object.toString() + "::"+object.getClass().getName());
    }
}
