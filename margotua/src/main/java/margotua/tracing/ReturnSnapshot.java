package margotua.tracing;

import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import com.sun.jdi.VoidValue;
import margotua.tracing.clone.ClonedValue;

public class ReturnSnapshot extends Snapshot{
    private AssignedValue returnValue = null;

    public ReturnSnapshot(StackFrame frame, Value returnValue) {
        super(frame.location());

        if(!(returnValue instanceof VoidValue) && returnValue != null) {
            this.returnValue = makeAssignedValue(this, "__RETURN_VALUE", returnValue);
            //System.err.println("Got " + returnValue.toString() + " at " + getLocationString());
        }
    }

    public AssignedValue getReturnValue() {
        return returnValue;
    }
}
