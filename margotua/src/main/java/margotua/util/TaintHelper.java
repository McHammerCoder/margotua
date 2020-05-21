package margotua.util;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import margotua.tracing.AssignedValue;
import margotua.tracing.CallSnapshot;
import margotua.tracing.StepSnapshot;
import margotua.tracing.clone.ClonedArrayValue;
import margotua.tracing.clone.ClonedObjectValue;
import margotua.tracing.clone.ClonedValue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TaintHelper {
    public static Taint[] getTaints(AssignedValue assigment) {
        ClonedValue value = assigment.getValue();
        if(value instanceof ClonedObjectValue) {
            ClonedObjectValue objectValue = (ClonedObjectValue) value;
            return objectValue.getTaints();
        } else if (value instanceof ClonedArrayValue) {
            ClonedArrayValue arrayValue = (ClonedArrayValue) value;
            //System.out.println(" !! Array taint not implemented");
            return null;
        } else {
            String name = assigment.name;
            String taintVariableName = getTaintShadowVariableName(name);
            Map<String, AssignedValue> availableAssignments;
            if(assigment.snapshot instanceof CallSnapshot) {
                availableAssignments = ((CallSnapshot)assigment.snapshot).getParameters();
            } else if( assigment.snapshot instanceof StepSnapshot) {
                availableAssignments = ((StepSnapshot)assigment.snapshot).getRawLocalVariables();
            } else {
                availableAssignments = new HashMap<>();
            }
            AssignedValue taintValue = availableAssignments.get(taintVariableName);
            if(taintValue == null) {
                //System.out.println(" !! NULL taint " + taintVariableName);
                return null;
            }
            if(! taintValue.type.equals(Taint.class.getName())) {
                System.err.println(" !! Unexpected taint type: " + taintValue.type);
                return null;
            }
            return ((ClonedObjectValue)taintValue.getValue()).getTaints();
        }
    }

    public static List<String> taintArrayToStringList(Taint[] taints) {
        if(taints == null) {
            return null;
        }
        return Arrays.stream(taints).map(TaintHelper::unnestTaintLabels).map(label -> label!=null?label:"null").map(Object::toString).collect(Collectors.toList());
    }

    public static String getTaintString(Taint[] taints) {
        if(taints == null || taints.length == 0) {
            return "";
        } else {
            return String.join(", ", taintArrayToStringList(taints));
        }
    }

    private static String getTaintShadowVariableName(String variableName) {
        return variableName + "$$PHOSPHORTAGGED";
    }

    public static Object unnestTaintLabels(Taint taint) {
        Object nested = taint;
        while(nested instanceof Taint) {
            nested = ((Taint)nested).getLabel();
        }
        return nested;
    }
}
