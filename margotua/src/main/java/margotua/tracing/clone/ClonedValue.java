package margotua.tracing.clone;


public abstract class ClonedValue {
    public static enum ClonedValueType  {
        PRIMITIVE,
        ARRAY,
        OBJECT
    };

    public final ClonedValueType type;
    public final String typeName;

    protected ClonedValue(ClonedValueType type, String typeName) {
        this.type = type;
        this.typeName = typeName;
    }
}
