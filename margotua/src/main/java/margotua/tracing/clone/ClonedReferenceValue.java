package margotua.tracing.clone;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public abstract class ClonedReferenceValue extends ClonedValue {
    public final long id;

    public ClonedReferenceValue(ClonedValueType type, String typeName, long id) {
        super(type, typeName);
        this.id = id;
    }

    public abstract Taint[] getTaints();
}
