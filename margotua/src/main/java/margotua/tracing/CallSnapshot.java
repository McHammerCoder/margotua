package margotua.tracing;

import com.sun.jdi.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CallSnapshot extends Snapshot {
    private Map<String, AssignedValue> parameters;
    public final boolean isConstructor;
    public final boolean isStaticInitializer;

    public CallSnapshot(StackFrame frame) {
        super(frame.location());
        isConstructor = frame.location().method().isConstructor();
        isStaticInitializer = frame.location().method().isStaticInitializer();

        parameters = new HashMap<>();
        try {
            //System.err.println("before this");
            if(frame.thisObject() != null) {
                parameters.put("this", new AssignedValue(this, "this", frame.thisObject()));
            }
            //System.err.println("after this");

            //List<LocalVariable> vis = frame.visibleVariables().stream().filter(
            //        e -> !e.typeName().startsWith("edu.columbia.cs.psl.phosphor")
            //).collect(Collectors.toList());
            //for(LocalVariable lvar : vis){
            //    System.out.println(lvar.name() + " :: " +lvar.typeName());
            //}



            //System.err.println("Cloning parameters for " + getLocationString());
            List<Location> locs = frame.location().method().allLineLocations();
            //if(locs.size() > 0) {
            //    System.err.println("At code idx " + frame.location().codeIndex() + ", first line is at: " + locs.get(0).codeIndex() + ", method at: " + frame.location().method().location().codeIndex());
            //}
            List<LocalVariable> methodParams = frame.location().method().arguments();
            for(LocalVariable var: methodParams) {
                //System.err.println("[ ] " + var.name());
                try {
                    parameters.put(var.name(), makeAssignedValue(this, var.name(), frame.getValue(var)));
                } catch (IllegalArgumentException e) {
                    //System.err.println("[-] Not able to access " + var.name() + ": " + e.getMessage());
                }
            }
            /*List<Value> argumentValues = frame.getArgumentValues();
            System.err.println("Cloning parameters for " + getLocationString()+": " + methodParams.stream().map(lv-> lv.name()+"::"+lv.typeName()).collect(Collectors.joining(", ")));
            parameters = IntStream
                    .range(0, Math.min(argumentValues.size(), methodParams.size()))
                    .mapToObj(i -> new AbstractMap.SimpleEntry<String, Value>(methodParams.get(i).name(), argumentValues.get(i)))
                    .collect(toMapNullFriendly(
                    e -> e.getKey(),
                    e -> makeAssignedValue(this, e.getKey(), e.getValue())));*/

            //parameters = cloneArguments(frame);
        } catch (AbsentInformationException e) {
            //System.err.println(getLocationString());
            //e.printStackTrace();
            this.parameters = new HashMap<>();
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

    private Map<String, AssignedValue> cloneArguments(StackFrame frame) {
        // Adapted from https://github.com/pgbovine/OnlinePythonTutor/blob/master/v4-cokapi/backends/java/java_jail/cp/traceprinter/JDI2JSON.java
        Map<String, AssignedValue> clonedArguments = new HashMap<>();

        /* KNOWN ISSUE:
           .arguments() gets the args which have names in LocalVariableTable,
           but if there are none, we get an IllegalArgExc, and can use .getArgumentValues()
           However, sometimes some args have names but not all. Such as within synthetic
           lambda methods like "lambda$inc$0". For an unknown reason, trying .arguments()
           causes a JDWP error in such frames. So sadly, those frames are incomplete. */
        boolean JDWPerror = false;
        try {
            frame.getArgumentValues();
        } catch (InternalException e) {
            if(e.errorCode() == 35) {
                JDWPerror = true;
            } else {
                throw e;
            }
        }

        List<LocalVariable> frame_args = null;
        boolean completed_args = false;
        try {
            // args make sense to show first
            frame_args = frame.location().method().arguments(); //throwing statement
            completed_args = !JDWPerror && frame_args.size() == frame.getArgumentValues().size();
            for (LocalVariable lv : frame_args) {
                if (lv.name().equals("args")) {
                    Value v = frame.getValue(lv);
                    if (v instanceof ArrayReference && ((ArrayReference)v).length()==0) continue;
                }
                try {
                    clonedArguments.put(lv.name(), makeAssignedValue(this, lv.name(), frame.getValue(lv)));
                }
                catch (IllegalArgumentException exc) {
                    System.err.println("That shouldn't happen!");
                }
            }
        }
        catch (AbsentInformationException e) {
        }
        // args did not have names, like a functional interface call...
        // although hopefully a future Java version will give them names!
        if (!completed_args && !JDWPerror) {
            try {
                List<Value> anon_args = frame.getArgumentValues();
                for (int i=0; i<anon_args.size(); i++) {
                    String name = "param#"+i;
                    clonedArguments.put(name, makeAssignedValue(this, name, anon_args.get(i)));
                }
            }
            catch (InvalidStackFrameException e) {
            }
        }

        return clonedArguments;
    }

    public Map<String, AssignedValue> getParameters() {
        return parameters.entrySet().stream().filter(
                e -> ! e.getKey().matches("Phopshor\\$\\$ImplicitTaintTrackingFromParent|phosphorReturnPreAlloc|phosphorJumpControlTag|phosphorTempStack")).collect(
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<String, AssignedValue> getRawParameters() {
        return parameters;
    }
}
