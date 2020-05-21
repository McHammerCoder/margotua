package margotua.tracing;

import com.sun.jdi.InternalException;
import com.sun.jdi.Value;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import margotua.tracing.clone.*;
import margotua.util.TaintHelper;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AssignedValue implements Serializable {
    private static final boolean CREATE_OBJ_DEEP_COPIES = false;
    private static final String indent = "  ";

    public final String type;
    public final String name;
    public final Snapshot snapshot;
    private Object value;
    private ClonedValue clonedValue;


    public AssignedValue(Snapshot snapshot, String name, Value value){
        this.snapshot = snapshot;
        this.name = name;

        if (value == null) {
            throw new IllegalArgumentException("Attempting to clone NULL value!!");
        } else if (value.type() == null) {
            throw new IllegalArgumentException("Attempting to clone value with NULL type!!");
        }
        type = value.type().name();
        //System.out.println(">>>>> Copying: " + type);
        try {
            clonedValue = FieldCloner.cloneFields(value);
        } catch (InternalException e) {
            if (e.errorCode() == 35) { // expect JDWP error 35 "INVALID_SLOT"
                System.err.println("!! Encountered error 35 in Value: " + name + "::" + type);
                //e.printStackTrace();
            } else {
                throw e;
            }
        }
    }

    public String getType() {
        return type;
    }

    public ClonedValue getValue() {
        return clonedValue;
    }

    public static boolean isWrapperType(Class<?> clazz) {
        return clazz.equals(Boolean.class) ||
                clazz.equals(Integer.class) ||
                clazz.equals(Character.class) ||
                clazz.equals(Byte.class) ||
                clazz.equals(Short.class) ||
                clazz.equals(Double.class) ||
                clazz.equals(Long.class) ||
                clazz.equals(Float.class);
    }

    @Override
    public String toString() {
        String objectValue = clonedValue.toString();

        return objectValue;
    }

    public Taint[] getTaints() {
        return TaintHelper.getTaints(this);
    }

    public String getTaintString() {
        Taint[] taints = getTaints();
        if(taints == null || taints.length == 0) {
            return "";
        }
        StringBuilder str = new StringBuilder("Taint<");
        boolean first = true;
        for(Taint taint: taints) {
            if(!first) {
                str.append(", ");
            }
            if(taint == null) {
                str.append("NULL");
            } else {
                //str.append(taint.toString());
                Object label = TaintHelper.unnestTaintLabels(taint);
                str.append(label.toString());
            }
            first=false;
        }
        str.append(">");
        return str.toString();
    }

    public static String rawDump(ClonedValue value) {
        Set<Long> seenIds = new HashSet<>();
        StringBuilder output = new StringBuilder("  ");
        return output.append(rawDump(value, seenIds, "", false)).toString();
    }

    public static StringBuilder rawDump(ClonedValue value, Set<Long> seenIDs, String prefix, boolean onlyTaints){
        StringBuilder builder = new StringBuilder();
        if(value == null) {
            if(onlyTaints) {
                return null;
            } else {
                builder.append("NULL");
                return builder;
            }
        }
        if (value instanceof ClonedPrimitiveValue) {
            if(onlyTaints) {
                return null;
            } else {
                ClonedPrimitiveValue primitiveValue = (ClonedPrimitiveValue) value;
                builder.append(primitiveValue.value.toString());
                return builder;
            }
        }

        ClonedReferenceValue referenceValue = (ClonedReferenceValue) value;
        if(seenIDs.contains(referenceValue.id)) {
            if(onlyTaints) {
                return null;
            } else {
                builder.append("<<Already seen: " + referenceValue.id + ">>");
                return builder;
            }
        }

        seenIDs.add(referenceValue.id);
        if(referenceValue instanceof ClonedArrayValue){
            ClonedArrayValue arrayValue = (ClonedArrayValue) referenceValue;
            builder.append('[');
            boolean encounteredTaint = false;
            for(int i = 0; i< arrayValue.array.length; i++) {
                ClonedValue m = arrayValue.array[i];
                StringBuilder element = rawDump(m, seenIDs, prefix, onlyTaints);
                if(element != null) {
                    encounteredTaint = true;
                    builder.append(element);
                    builder.append(", ");
                }
            }
            if(onlyTaints && !encounteredTaint) {
                return null;
            }
            builder.append(']');
        } else {
            builder.append("{\n");
            if(referenceValue.typeName.contains("edu.columbia.cs.psl.phosphor")) {
                // attempt to reconstruct taint objects for easier handling
                try{
                    Object obj = ValueConcretizer.deepClone(referenceValue);
                    Taint t = MultiTainter.getMergedTaint(obj);
                    if(t != null) {
                        builder.append(prefix).append(indent).append("!! TAINT: ").append(t.toString()).append("\n");
                        builder.append(prefix).append(indent).append("!! TAINT DEPS:").append(t.getLabels());
                    } else {
                        builder.append(prefix).append(indent).append("!! TAINT: NULL").append("\n");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                ClonedObjectValue objectValue = (ClonedObjectValue) referenceValue;
                builder.append(prefix).append(indent).append("!! Object Type: ").append(objectValue.typeName).append("\n");
                builder.append(prefix).append(indent).append("!! Object ID: ").append(objectValue.id).append("\n");
                boolean encounteredTaint = false;
                if(!objectValue.typeName.equals("java.lang.Class")) {
                    for (Map.Entry<String, ClonedValue> entry : objectValue.entrySet()) {
                        StringBuilder serialized = rawDump(entry.getValue(), seenIDs, prefix + indent, onlyTaints);
                        if(serialized != null) {
                            encounteredTaint = true;
                            builder.append(prefix).append(indent);
                            builder.append(entry.getKey()).append(": ");
                            builder.append(serialized);
                            builder.append(", \n");
                        }
                    }
                }
                if(onlyTaints && !encounteredTaint) {
                    return null;
                }
            }
            builder.append(prefix).append('}');
        }

        return builder;
    }
    /*
    public static Taint asTaint(ClonedValue clonedValue) {
        if(clonedValue.typeName.contains("edu.columbia.cs.psl.phosphor")) {
            // attempt to reconstruct taint objects for easier handling
            try{
                Object obj = ValueConcretizer.deepClone(clonedValue);
                return  MultiTainter.getMergedTaint(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public Taint getTaint() {
        if(clonedValue.typeName.contains("edu.columbia.cs.psl.phosphor")) {
            return asTaint(clonedValue);
        } else {
            for(Map.Entry attribute : clonedValue)
            // attempt to reconstruct taint objects for easier handling
            try{
                Object obj = ValueConcretizer.deepClone(referenceValue);
                Taint t = MultiTainter.getMergedTaint(obj);
                if(t != null) {
                    builder.append(prefix).append(indent).append("!! TAINT: ").append(t.toString()).append("\n");
                } else {
                    builder.append(prefix).append(indent).append("!! TAINT: NULL").append("\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    } */
}
