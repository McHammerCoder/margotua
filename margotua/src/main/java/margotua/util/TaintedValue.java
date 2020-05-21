package margotua.util;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.util.Arrays;
import java.util.stream.Collectors;

public class TaintedValue<T> {
    public final Taint[] taints;
    public final T value;

    public TaintedValue(T value, Taint[] taints) {
        this.value = value;
        this.taints = taints;
    }

    @Override
    public String toString() {
        String escapedValue = value.toString().replace("\n", "\\n").replace("\r", "\\r");
        return "TaintedValue<value="+escapedValue+", taint="+TaintHelper.getTaintString(taints)+">";
    }
}
