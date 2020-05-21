package margotua.tracing;

import com.sun.jdi.*;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.objenesis.instantiator.ObjectInstantiator;
import margotua.tracing.clone.*;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ValueConcretizer {

    private static final Objenesis objenesis = new ObjenesisStd();
    private static final boolean nullTransient = false;
    private static final Field[] EMPTY_FIELD_ARRAY = new Field[0];

    private static final Map<String, ValueConcretizer> concretizers = Collections.synchronizedMap(new HashMap<>());

    //private static final Map<Long, Object> clones = Collections.synchronizedMap(new HashMap<>());
    private final ObjectInstantiator<?> instantiator;
    private final Field[] fields;
    private final boolean[] shouldClone;
    private final int numFields;

    private ValueConcretizer(ClonedValue value){
        Class<?> clazz = parseType(value.typeName);

        Class<?> sc = clazz;
        List<Field> l = new ArrayList<Field>();
        List<Boolean> shouldCloneList = new ArrayList<Boolean>();
        do {
            Field[] classFields = sc.getDeclaredFields();
            for (final Field f : classFields) {
                if (!f.isAccessible()) {
                    f.setAccessible(true);
                }
                int modifiers = f.getModifiers();
                if (!Modifier.isStatic(modifiers)) {
                    if (!(nullTransient && Modifier.isTransient(modifiers))) {
                        l.add(f);
                        boolean shouldClone = !f.isSynthetic();
                        shouldCloneList.add(shouldClone);
                    }
                }
            }
        } while ((sc = sc.getSuperclass()) != Object.class && sc != null);
        fields = l.toArray(EMPTY_FIELD_ARRAY);
        numFields = fields.length;
        shouldClone = new boolean[numFields];
        for (int i = 0; i < shouldCloneList.size(); i++) {
            shouldClone[i] = shouldCloneList.get(i);
        }

        instantiator = objenesis.getInstantiatorOf(clazz);
    }

    private <T> T deepClone(ClonedObjectValue objectValue, Map<Long, Object> clones) {
        try {
            @SuppressWarnings("unchecked") T newInstance = (T) instantiator.newInstance();
            if (clones != null) {
                clones.put(objectValue.id, newInstance);
                for (int i = 0; i < numFields; i++) {
                    Field field = fields[i];
                    Object remoteValueClone = shouldClone[i] ? cloneValue(objectValue.get(field.getName()), clones) : null;
                    field.set(newInstance, remoteValueClone);
                }
            } else {
                // Shallow clone
                for (int i = 0; i < numFields; i++) {
                    Field field = fields[i];
                    field.set(newInstance, objectValue.get(field.getName()));
                }
            }
            return newInstance;
        } catch (Exception e) {
            System.err.println("\n\n\n ==== ERROR: ");
            e.printStackTrace();
            System.err.println("\n ==== \n");
            throw new RuntimeException(e);
        }

    }

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
                try {
                    ClassLoader loader = ValueConcretizer.class.getClassLoader();
                    return loader.loadClass(typeName);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Type not found: " + typeName);
                }
        }
    }

    private static <T> T cloneValue(ClonedValue value, Map<Long, Object> clones) throws ClassNotLoadedException, ClassNotFoundException {
        if (value == null) {
            return null;
        } else if (value instanceof ClonedPrimitiveValue) {
            return clonePrimitive((ClonedPrimitiveValue) value, clones);
        } else {
            return cloneGenericReference((ClonedReferenceValue) value, clones);
        }
    }

    private static <T> T clonePrimitive(ClonedPrimitiveValue value, Map<Long, Object> clones) {
        switch (value.typeName) {
            case "boolean":
                return (T) (Boolean) value.value;
            case "byte":
                return (T) (Byte) value.value;
            case "char":
                return (T) (Character) value.value;
            case "double":
                return (T) (Double) value.value;
            case "float":
                return (T) (Float) value.value;
            case "int":
                return (T) (Integer) value.value;
            case "long":
                return (T) (Long) value.value;
            case "short":
                return (T) (Short) value.value;
            case "void":
            default:
                throw new IllegalArgumentException("Can't clone this primitive value Type: "+value.typeName+"!");
        }
    }

    private static <T> T cloneGenericReference(ClonedReferenceValue referenceValue, Map<Long, Object> clones) throws ClassNotFoundException, ClassNotLoadedException {
        if (referenceValue == null) return null;

        // Prevent cycles, expensive but necessary
        if (clones != null) {
            T clone = (T) clones.get(referenceValue.id);
            if (clone != null) {
                return clone;
            }
        }

        /*if(referenceValue instanceof StringReference) {
            return (T) ((StringReference)referenceValue).value();
        } else */
        if (referenceValue instanceof ClonedArrayValue) {
            return cloneArrayReference((ClonedArrayValue)referenceValue, clones);
        } else {
            return cloneObjectReference((ClonedObjectValue)referenceValue, clones);
        }
    }

    private static <T> T cloneArrayReference(ClonedArrayValue arrayValue, Map<Long, Object> clones)  throws ClassNotLoadedException, ClassNotFoundException{
        int length = arrayValue.array.length;
        String componentTypeName = arrayValue.typeName;
        Class<T> componentType = parseType(componentTypeName);
        T arrayCopy = (T) Array.newInstance(componentType, length);

        clones.put(arrayValue.id, arrayCopy);
        for (int i = 0; i < length; i++) {
            Array.set(arrayCopy, i, cloneValue(arrayValue.array[i], clones));
        }

        return arrayCopy;
    }

    private static <T> T cloneObjectReference(ClonedObjectValue reference, Map<Long, Object> clones) {
        String typeName = reference.typeName;
        ValueConcretizer concretizer = concretizers.get(typeName);

        if (concretizer == null) {
            concretizer = new ValueConcretizer(reference);
            concretizers.put(typeName, concretizer);
        }


        return concretizer.deepClone(reference, clones);
    }

    public static <T> T deepClone(ClonedValue value) throws ConcretizationException{
        if (value == null) return null;
        Map<Long, Object> clones = new HashMap<>();
        try {
            return cloneValue(value, clones);
        } catch (ClassNotFoundException | ClassNotLoadedException e) {
            throw new ConcretizationException("Could not concretize!", e);
        }
    }

    public static void main(String[] args) {

    }
}
