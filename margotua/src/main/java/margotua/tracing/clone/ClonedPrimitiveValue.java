package margotua.tracing.clone;

public class ClonedPrimitiveValue extends ClonedValue {
    public final Object value;
    public final Class clazz;

    public ClonedPrimitiveValue(String typeName, Class clazz, Object value) {
        super(ClonedValueType.PRIMITIVE, typeName);
        this.clazz = clazz;
        this.value = value;
    }

    @Override
    public String toString() {
        return "ClonedPrimitiveValue<NOID, "+typeName+", "+value.toString().replace("\n", "\\n")+">";
    }


}
