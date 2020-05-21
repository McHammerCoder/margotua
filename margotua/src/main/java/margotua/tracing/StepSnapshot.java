package margotua.tracing;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.InternalException;
import com.sun.jdi.StackFrame;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class StepSnapshot extends Snapshot {
    private Map<String, AssignedValue> localVariables;

    public StepSnapshot(StackFrame frame) {
        super(frame.location());

        localVariables = new HashMap<>();
        try {
            //System.err.println("before this");
            if (frame.thisObject() != null) {
                localVariables.put("this", new AssignedValue(this, "this", frame.thisObject()));
            }
            //System.err.println("after this");

            //List<LocalVariable> vis = frame.visibleVariables().stream().filter(
            //        e -> !e.typeName().startsWith("edu.columbia.cs.psl.phosphor")
            //).collect(Collectors.toList());
            //for(LocalVariable lvar : vis){
            //    System.out.println(lvar.name() + " :: " +lvar.typeName());
            //}

            localVariables = frame.getValues(frame.visibleVariables()).entrySet().stream().filter(
                    e -> Objects.nonNull(e.getValue())
            ).collect(toMapNullFriendly(
                    e -> e.getKey().name(),
                    e -> makeAssignedValue(this, e.getKey().name(), e.getValue())));
        } catch (AbsentInformationException e) {
            this.localVariables = new HashMap<>();
        } catch (InternalException e) {
            //if (e.toString().contains("Unexpected JDWP Error: 35")) // expect JDWP error 35
            if (e.errorCode() == 35) { // expect JDWP error 35 "INVALID_SLOT"
                //System.err.println("!! Encountered error 35 at " + getLocationString());
                //e.printStackTrace();
            } else {
                throw e;
            }
        }
    }

    public Map<String, AssignedValue> getRawLocalVariables() {
        return localVariables;
    }

    public Map<String, AssignedValue> getLocalVariables() {
        return localVariables.entrySet().stream().filter(
                e -> ! e.getKey().matches("Phopshor\\$\\$ImplicitTaintTrackingFromParent|phosphorReturnPreAlloc|phosphorJumpControlTag|phosphorTempStack")).collect(
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
