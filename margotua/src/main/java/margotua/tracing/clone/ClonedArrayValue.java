package margotua.tracing.clone;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import margotua.tracing.ValueConcretizer;

import java.lang.reflect.Array;

public class ClonedArrayValue extends ClonedReferenceValue {
    public final ClonedValue[] array;

    public ClonedArrayValue(String typeName, Long id, ClonedValue[] array) {
        super(ClonedValueType.ARRAY, typeName, id);
        this.array = array;
    }

    @Override
    public String toString() {
        String objectValue;
        try {
            objectValue = ValueConcretizer.deepClone(this).toString().replace("\n", "\\n");
        } catch (Exception e) {
            e.printStackTrace();
            objectValue = "[]";
        }
        return "ClonedArrayValue<"+id+", "+typeName+", \""+ objectValue +"\">";
    }

    @Override
    public Taint[] getTaints() {
        return null;
    }
}
