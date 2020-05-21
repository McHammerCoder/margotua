package margotua.pipeline;

import margotua.mining.UnparseTree;
import margotua.tracing.*;
import margotua.tracing.clone.ClonedObjectValue;
import margotua.tracking.CallTree;
import margotua.util.TaintedValue;

import java.util.*;
import java.util.stream.Collectors;

public class Tracker {

    public static CallTree track(List<Snapshot> snapshots) {
        CallTree callTree = new CallTree();
        CallTree.CallNode currentNode = callTree.root;

        for(Snapshot snapshot:snapshots) {
            if(snapshot instanceof CallSnapshot) {
                CallSnapshot callSnapshot = (CallSnapshot) snapshot;
                if(callSnapshot.isConstructor) {
                    // skip constructors for now. should not need to trace them unless intermediate objects that have to
                    // be tracked are generated
                    continue;
                }
                CallTree.CallNode nextNode = new CallTree.CallNode(currentNode, callSnapshot);
                currentNode.descendants.add(nextNode);
                currentNode = nextNode;
                continue;
            } else if(snapshot instanceof StepSnapshot) {
                StepSnapshot stepSnapshot = (StepSnapshot) snapshot;
                currentNode.descendants.add(new CallTree.StepNode(currentNode, stepSnapshot));
            } else if(snapshot instanceof ReturnSnapshot) {
                ReturnSnapshot returnSnapshot = (ReturnSnapshot) snapshot;
                currentNode.returned(returnSnapshot);
                currentNode = currentNode.parent;
                if(currentNode == null) {
                    //reached the top
                    break;
                }
            } else {
                System.err.println("!!!!!!!!!! Should never happen!");
                System.exit(255);
            }
        }

        return callTree;
    }

    public static void injectStringBufferPseudoReturns(CallTree tree) {
        injectStringBufferPseudoReturns(tree.root);
    }


    private static void injectStringBufferPseudoReturns(CallTree.CallNode node) {
        for(CallTree.Node descendant: node.descendants) {
            if(descendant instanceof CallTree.CallNode) {
                injectStringBufferPseudoReturns((CallTree.CallNode) descendant);
            }
        }

        //if(node.getReturnValue() == null && node.callSnapshot != null) {
        if(node.callSnapshot != null) {
            //System.err.println("Attempting to reconstruct void return value in node: "+node.callSnapshot.getLocationString()+"...");
            //System.err.println("Matching ["+node.callSnapshot.getRawParameters().values().stream().map(AssignedValue::toString).collect(Collectors.joining(", "))+"] against "+StringBuffer.class.getName());
            List<Map.Entry<String, AssignedValue>> matching =  node.callSnapshot.getRawParameters().entrySet().stream().filter(param -> param.getValue().getType().equals(StringBuffer.class.getName())).collect(Collectors.toList());
            if(matching.size() == 1){
                try {
                    Map.Entry<String, AssignedValue> param = matching.get(0);
                    //System.err.println("[+] Found StringBuffer parameter: "+ param.getKey());
                    String bufferName = param.getKey();
                    AssignedValue beforeValue = param.getValue();
                    StringBuffer before = ValueConcretizer.deepClone(beforeValue.getValue());
                    //System.err.println("[+] Before value was: "+before.toString());

                    CallTree.Node lastStep = node.getLastStepInCall();
                    if(lastStep == null) {
                        //System.err.println("[-] No Steps inside this call! Aborting...");
                        // no steps inside call -> ignore call
                        return;
                    }
                    StepSnapshot lastStepSnapshot = ((CallTree.StepNode) lastStep).snapshot;
                    //System.err.println("[ ] Last step at " + lastStepSnapshot.getLocationString());
                    AssignedValue lastBufferState = lastStepSnapshot.getLocalVariables().get(bufferName);
                    if(lastBufferState == null) {
                        //should never happen!
                        //System.err.println("Could not find StringBuffer return param " + bufferName + " in call step " + lastStepSnapshot.getLocationString());
                        return;
                    }
                    StringBuffer after = ValueConcretizer.deepClone(lastBufferState.getValue());
                    //System.err.println("[+] After value was: "+after.toString());

                    String suffix = after.substring(before.length());
                    //System.err.println("[+] Suffix difference: " + suffix);
                    node.returned(new TaintedValue<String>(suffix, Arrays.copyOfRange(((ClonedObjectValue) lastBufferState.getValue()).getTaints(),before.length(), after.length())));
                } catch (ConcretizationException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
