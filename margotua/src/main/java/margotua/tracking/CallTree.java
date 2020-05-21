package margotua.tracking;

import margotua.mining.UnparseTree;
import margotua.tracing.*;
import margotua.tracing.clone.ClonedObjectValue;
import margotua.util.Formatter;
import margotua.util.TaintedValue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CallTree {
    public final CallTree.CallNode root;

    public CallTree() {
        this.root = new CallNode(null, null);
    }

    @Override
    public String toString() {
        return root.toString();
    }

    public static abstract class Node {
        public final List<Node> descendants;

        public Node() {
            this.descendants = new ArrayList<>();
        }

        /*
        @Override
        public String toString() {
            return "Node<" + name + ((value != null) ? ", " + value : "") + ">:{ " + descendants.stream()
                    .map(node -> node.toString())
                    .collect(Collectors.joining(", ")) + "}";
        }*/
    }

    public static class CallNode extends Node{
        public final CallNode parent;
        public final CallSnapshot callSnapshot;
        public ReturnSnapshot returnSnapshot;
        private TaintedValue<String> pseudoReturn;
        public CallNode(CallNode parent, CallSnapshot callSnapshot) {
            super();

            this.parent = parent;
            this.callSnapshot = callSnapshot;
        }

        public void returned(ReturnSnapshot returnSnapshot) {
            this.returnSnapshot = returnSnapshot;
            if(returnSnapshot.getReturnValue() != null) {
                String returnString;
                AssignedValue returnValue = returnSnapshot.getReturnValue();
                if(returnValue.getValue() instanceof ClonedObjectValue) {
                    try {
                        returnString = ((ClonedObjectValue) returnSnapshot.getReturnValue().getValue()).getConcretizedObject().toString();
                    } catch (ConcretizationException e) {
                        //System.err.println("Error unwrapping ClonedObjectValue String: " + e.getMessage());
                        returnString = null;
                    }
                } else {
                    returnString = "";
                }
                pseudoReturn = new TaintedValue<>(returnString, returnSnapshot.getReturnValue().getTaints());
                //System.err.println("  ++  Got a return value: " + pseudoReturn.toString() + " at " + returnSnapshot.getLocationString() + "/"+callSnapshot.getLocationString());
                //System.err.println(pseudoReturn);
            } else {
                //System.err.println("  !!  Void return value: " + returnSnapshot.toString() + " at " + returnSnapshot.getLocationString() + "/"+((callSnapshot!=null)?callSnapshot.getLocationString():"NOCALL"));
            }
        }

        public void returned(TaintedValue<String> returnValue) {
            this.pseudoReturn = returnValue;
        }

        public TaintedValue<String> getReturnValue() {
            return pseudoReturn;
        }

        public CallTree.StepNode getLastStepInCall() {
            return (StepNode)descendants.stream()
                    .filter(descendant -> descendant instanceof CallTree.StepNode)
                    .reduce((first, second) -> second)
                    .orElse(null);
        }

        @Override
        public String toString() {
            return "CallNode<call="+((callSnapshot!=null)?callSnapshot.getLocationString():"")+", return="+((getReturnValue() != null)? getReturnValue().toString():((returnSnapshot!=null)?returnSnapshot.toString():"N/A"))+">: {\n"+
                    Formatter.prefixLines("    ", descendants.stream()
                    .map(node->node.toString())
                    .filter(s->s.length()>0)
                    .collect(Collectors.joining(",\n")))+"\n}";
        }
    }

    public static class StepNode extends Node{
        public final CallNode parent;
        public final StepSnapshot snapshot;
        public StepNode(CallNode parent, StepSnapshot snapshot) {
            super();

            this.parent = parent;
            this.snapshot = snapshot;
        }

        @Override
        public String toString() {
            return "";
        }
    }

}
