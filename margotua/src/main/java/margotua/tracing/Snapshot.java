package margotua.tracing;

import com.sun.jdi.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Snapshot implements Serializable {
    String declaringType;
    String methodName;
    String sourcePath = null;
    int lineNumber;

    public Snapshot(Location loc) {
        declaringType = loc.declaringType().name();
        methodName = loc.method().name();
        lineNumber = loc.lineNumber();
        //System.out.println(methodName+":L"+lineNumber+"    "+loc.toString());
        try {
            sourcePath = loc.sourcePath();
        } catch (AbsentInformationException e) {

        }
    }

    // https://stackoverflow.com/a/32649053
    public static <T, K, U> Collector<T, ?, Map<K, U>> toMapNullFriendly(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper) {
        @SuppressWarnings("unchecked")
        U none = (U) new Object();
        return Collectors.collectingAndThen(
                Collectors.<T, K, U> toMap(keyMapper,
                        valueMapper.andThen(v -> v == null ? none : v)), map -> {
                    map.replaceAll((k, v) -> v == none ? null : v);
                    return map;
                });
    }

    static AssignedValue makeAssignedValue(Snapshot snapshot, String name, Value value) {
        //System.err.println("Cloning AssignedValue "+name+" at "+snapshot.getLocationString());
        return new AssignedValue(snapshot, name, value);

    }

    public String getMethodName() {
        return declaringType+"::"+methodName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getLocationString() {
        return getMethodName() + ":L" + getLineNumber();
    }


}
