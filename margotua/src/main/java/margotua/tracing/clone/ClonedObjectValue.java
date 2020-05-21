package margotua.tracing.clone;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.LazyCharArrayObjTags;
import margotua.tracing.ConcretizationException;
import margotua.tracing.ValueConcretizer;

import java.lang.reflect.Array;
import java.util.*;

public class ClonedObjectValue extends ClonedReferenceValue implements Map<String, ClonedValue> {
    private final Map<String, ClonedValue> fields;
    Object concretizedObject = null;

    public ClonedObjectValue(String typeName, Long id) {
        super(ClonedValueType.OBJECT, typeName, id);
        this.fields = new HashMap<>();
    }

    public Object getConcretizedObject() throws ConcretizationException {
        if(concretizedObject != null) {
            return concretizedObject;
        } else {
            concretizedObject = ValueConcretizer.deepClone(this);
            //System.out.println(" !! Concretized: " + concretizedObject.toString());
            return concretizedObject;
        }
     }

    //<editor-fold desc="Pass-through Map implementation">
    @Override
    public int size() {
        return fields.size();
    }

    @Override
    public boolean isEmpty() {
        return fields.isEmpty();
    }

    @Override
    public boolean containsKey(Object o) {
        return fields.containsKey(o);
    }

    @Override
    public boolean containsValue(Object o) {
        return fields.containsValue(o);
    }

    @Override
    public ClonedValue get(Object o) {
        return fields.get(o);
    }

    @Override
    public ClonedValue put(String k, ClonedValue v) {
        return fields.put(k, v);
    }

    @Override
    public ClonedValue remove(Object o) {
        return fields.remove(o);
    }

    @Override
    public void putAll(Map<? extends String, ? extends ClonedValue> map) {
        fields.putAll(map);
    }

    @Override
    public void clear() {
        fields.clear();
    }

    @Override
    public Set<String> keySet() {
        return fields.keySet();
    }

    @Override
    public Collection<ClonedValue> values() {
        return fields.values();
    }

    @Override
    public Set<Entry<String, ClonedValue>> entrySet() {
        return fields.entrySet();
    }
    //</editor-fold>

    @Override
    public String toString() {
        String objectValue;
        try {
            objectValue = ValueConcretizer.deepClone(this).toString().replace("\n", "\\n").replace("\r", "\\r");
        } catch (NullPointerException e) {
            objectValue = "!! NULL";
        } catch (Exception e ) {
            e.printStackTrace();
            objectValue = this.keySet().toString().replace("\n", "\\n").replace("\r", "\\r");
        }
        return "ClonedObjectValue<"+id+", "+typeName+", \""+ objectValue +"\">";
    }

    public Taint[] getTaints() {
        try {
            if(typeName.equals(String.class.getName())) {
                try {
                    return ((LazyCharArrayObjTags) ValueConcretizer.deepClone(get("valuePHOSPHOR_TAG"))).taints;
                } catch (NullPointerException e) {
                    return null;
                }
            }

            if(typeName.equals(StringBuffer.class.getName())) {
                Taint[] taints = ((LazyCharArrayObjTags)ValueConcretizer.deepClone(get("valuePHOSPHOR_TAG"))).taints;
                if(taints != null) {
                    return Arrays.copyOfRange(taints, 0, Integer.parseInt(((ClonedPrimitiveValue) get("count")).value.toString()));
                } else {
                    return null;
                }
            }

            Object concretized = getConcretizedObject();
            if(concretized instanceof Boolean || concretized instanceof Character || concretized instanceof Short || concretized instanceof Byte) {
                System.out.println(" !! " + concretized.getClass().getName() + " not impl");
                return null;
            }

            Taint[] taint = {MultiTainter.getMergedTaint(getConcretizedObject())};
            //System.out.println(" !! Taint = " + ((taint!=null)?taint.toString():"null"));
            return taint;
        } catch (ConcretizationException e) {
            e.printStackTrace();
            return null;
        }
    }

}
