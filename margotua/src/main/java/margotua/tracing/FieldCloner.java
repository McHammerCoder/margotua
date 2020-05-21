package margotua.tracing;

import com.sun.jdi.*;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import margotua.tracing.clone.ClonedArrayValue;
import margotua.tracing.clone.ClonedObjectValue;
import margotua.tracing.clone.ClonedPrimitiveValue;
import margotua.tracing.clone.ClonedValue;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

public class FieldCloner {

    private static Class parseType(final String typeName) {
        switch (typeName) {
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "char":
                return char.class;
            case "void":
                return void.class;
            default:
                // All non-primitive Objects are represented as Maps
                return Map.class;
        }
    }

    private static ClonedValue cloneValue(Value value, Map<Long, ClonedValue> clones) {
        if (value instanceof PrimitiveValue) {
            return clonePrimitive((PrimitiveValue) value, clones);
        } else {
            return cloneGenericReference((ObjectReference) value, clones);
        }
    }

    private static ClonedValue clonePrimitive(PrimitiveValue value, Map<Long, ClonedValue> clones) {
        String typeName = value.type().name();
        switch (typeName) {
            case "boolean":
                return new ClonedPrimitiveValue(typeName, Boolean.class, value.booleanValue());
            case "byte":
                return new ClonedPrimitiveValue(typeName, Boolean.class, value.byteValue());
            case "char":
                return new ClonedPrimitiveValue(typeName, Boolean.class, value.charValue());
            case "double":
                return new ClonedPrimitiveValue(typeName, Boolean.class, value.doubleValue());
            case "float":
                return new ClonedPrimitiveValue(typeName, Boolean.class, value.floatValue());
            case "int":
                return new ClonedPrimitiveValue(typeName, Boolean.class, value.intValue());
            case "long":
                return new ClonedPrimitiveValue(typeName, Boolean.class, value.longValue());
            case "short":
                return new ClonedPrimitiveValue(typeName, Boolean.class, value.shortValue());
            case "void":
            default:
                throw new IllegalArgumentException("Can't clone this primitive value Type: "+value.type().name()+"!");
        }
    }

    private static ClonedValue cloneGenericReference(ObjectReference reference, Map<Long, ClonedValue> clones) {
        if (reference == null) return null;

        // Prevent cycles, expensive but necessary
        if (clones == null) {
            throw new AssertionError("No clones map passed!");
        }

        ClonedValue clone = clones.get(reference.uniqueID());
        if (clone != null) {
            return clone;
        }


        /*if(reference instanceof StringReference) {
            Map<String, Object> copy = new HashMap<>();
            copy.put(MARGOTUA_TYPE_NAME, "java.lang.String");
            copy.put(MARGOTUA_TYPE, FieldType.OBJECT);
            copy.put(MARGOTUA_ID, reference.uniqueID());
            copy.put(MARGOTUA_VALUE, ((StringReference)reference).value());

            return copy;
        } else */
        if (reference instanceof ArrayReference) {
            return cloneArrayReference((ArrayReference) reference, clones);
        } else {
            return cloneObjectReference(reference, clones);
        }
    }

    private static ClonedValue cloneArrayReference(ArrayReference reference, Map<Long, ClonedValue> clones) {
        int length = reference.length();
        String componentTypeName = ((ArrayType)reference.referenceType()).componentTypeName();
        ClonedValue[] arrayCopy = new ClonedValue[length];

        ClonedArrayValue copy = new ClonedArrayValue(componentTypeName, reference.uniqueID(), arrayCopy);

        clones.put(reference.uniqueID(), copy);
        for (int i = 0; i < length; i++) {
            Object member = cloneValue(reference.getValue(i), clones);
            Array.set(arrayCopy, i, member);
        }

        return copy;
    }

    private static ClonedValue cloneObjectReference(ObjectReference reference, Map<Long, ClonedValue> clones) {
        String typeName = reference.referenceType().name();

        ClonedObjectValue copy = new ClonedObjectValue(typeName, reference.uniqueID());

        if (clones != null) {
            clones.put(reference.uniqueID(), copy);

            for (com.sun.jdi.Field remoteField : reference.referenceType().allFields()) {
                Value remoteValue = reference.getValue(remoteField);
                ClonedValue remoteValueClone = cloneValue(remoteValue, clones);
                copy.put(remoteField.name(), remoteValueClone);
            }
        } else {
            throw new AssertionError("No clones map passed!");
        }
        return copy;

    }

    public static ClonedValue cloneFields(Value value){
        if (value == null) return null;
        Map<Long, ClonedValue> clones = new HashMap<>();
        return cloneValue(value, clones);
    }
}
