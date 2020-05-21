package margotua.pipeline;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import jdk.nashorn.internal.codegen.CompilerConstants;
import margotua.mining.UnparseTree;
import margotua.tracing.*;
import margotua.tracing.clone.ClonedObjectValue;
import margotua.tracing.clone.ClonedValue;
import margotua.tracking.CallTree;
import margotua.util.TaintHelper;
import margotua.util.TaintedValue;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Miner {
    public static final String TERMINAL = "__TERMINAL";
    public static final String ATTR = "__ATTRIBUTE";

    public static UnparseTree mine(CallTree callTree) {
        UnparseTree unparseTree = new UnparseTree();
        unparseTree.root.descendants.add(processCall((CallTree.CallNode) callTree.root.descendants.get(0), unparseTree.root));
        return unparseTree;
    }

    private static UnparseTree.Node processCall(CallTree.CallNode callNode, UnparseTree.Node parent) {
        UnparseTree.Node thisRule = new UnparseTree.Node(parent, (callNode.callSnapshot != null) ? callNode.callSnapshot.getMethodName() : "NULL", callNode.getReturnValue());
        List<CallTree.CallNode> subcalls = callNode.descendants.stream().filter(CallTree.CallNode.class::isInstance).map(CallTree.CallNode.class::cast).collect(Collectors.toList());

        //System.err.println("== " + thisRule.toString());

        TaintedValue<String> taintedReturnValue = callNode.getReturnValue();
        String returnValueString = taintedReturnValue.value;
        Taint[] taints = taintedReturnValue.taints;
        List<String> taintStrings = TaintHelper.taintArrayToStringList(taints);

        boolean[] claimedChars = new boolean[returnValueString.length()]; // allocated as all false by default
        int[] skips = new int[returnValueString.length()];
        UnparseTree.Node[] subrules = new UnparseTree.Node[returnValueString.length()];
        String[] attributes = new String[returnValueString.length()];
        String[] attributeValues = new String[returnValueString.length()];
        String[] parameters = new String[returnValueString.length()];
        String[] parameterValues = new String[returnValueString.length()];

        if (subcalls.size() != 0) {
            // Match up subcalls
            for (CallTree.Node node : callNode.descendants) {
                if (node instanceof CallTree.StepNode) {
                    continue;
                }

                CallTree.CallNode descendant = (CallTree.CallNode) node;
                if (descendant.callSnapshot.isConstructor || descendant.callSnapshot.isStaticInitializer) {
                    continue;
                }

                UnparseTree.Node subrule = processCall(descendant, thisRule);

                //System.err.println("      " + subrule.toString());

                String descendantReturn = subrule.value.value;
                int substringIndex = returnValueString.indexOf(descendantReturn);
                if (substringIndex >= 0) {
                    List<String> descendantTaintStrings = TaintHelper.taintArrayToStringList(subrule.value.taints);
                    if (taintStrings == null || descendantTaintStrings == null) {
                        //System.err.println("      ERROR: taintStrins or descendantTaintStrings is NULL");
                        subrules[substringIndex] = subrule;
                        skips[substringIndex] = descendantReturn.length();
                        for (int i = 0; i < descendantReturn.length(); i++) {
                            claimedChars[substringIndex + i] = true;
                        }
                    } else {
                        int taintSublistIndex = Collections.indexOfSubList(taintStrings, descendantTaintStrings);
                        if (taintSublistIndex == substringIndex) {
                            if (!Arrays.asList(Arrays.copyOfRange(claimedChars, substringIndex, substringIndex + descendantReturn.length())).contains(true)) {
                                //System.err.println("      Success: adding " + subrule.toString() + " at position " + substringIndex);
                                subrules[substringIndex] = subrule;
                                skips[substringIndex] = descendantReturn.length();
                                for (int i = 0; i < descendantReturn.length(); i++) {
                                    claimedChars[substringIndex + i] = true;
                                }
                            } else {
                                //System.err.println("      ERROR: substring \"" + descendantReturn + "\" overlaps claimed region!");
                            }
                        } else {
                            //System.err.println("      ERROR: mismatch in parent/child taint idxs: " + substringIndex + "/" + taintSublistIndex + "[" + String.join(", ", taintStrings) + "] vs [" + String.join(", ", descendantTaintStrings) + "]");
                        }
                    }
                } else {
                    //System.err.println("      Could not find descendant return in parent value: \"" + descendantReturn + "\" in \"" + returnValueString + "\".");
                }
            }
        }

        //Attempt to find attributes
        //CallTree.StepNode lastStep = callNode.getLastStepInCall();

        AssignedValue thisObject = callNode.callSnapshot.getParameters().get("this");
        ClonedObjectValue thisObjectClone = thisObject != null ? (ClonedObjectValue) thisObject.getValue() : null;
        if (thisObjectClone != null) {
            for (Map.Entry<String, ClonedValue> entry : ((ClonedObjectValue) thisObject.getValue()).entrySet()) {
                try {
                    //System.err.println("        Checking " + entry.getKey() + ": " + entry.getValue());
                    // locate class attribute in return value
                    if (entry.getValue() instanceof ClonedObjectValue) {
                        ClonedObjectValue objectValue = (ClonedObjectValue) entry.getValue();
                        String attributeValue = objectValue.getConcretizedObject().toString();
                        int substringIndex = returnValueString.indexOf(attributeValue);
                        if (substringIndex >= 0) {
                            List<String> attributeTaintStrings = TaintHelper.taintArrayToStringList(objectValue.getTaints());
                            if (taintStrings == null || attributeTaintStrings == null) {
                                //System.err.println("      ERROR: taintStrins or descendantTaintStrings is NULL");
                                ;
                            } else {
                                int taintSublistIndex = Collections.indexOfSubList(taintStrings, attributeTaintStrings);
                                if (taintSublistIndex == substringIndex) {
                                    if (!Arrays.asList(Arrays.copyOfRange(claimedChars, substringIndex, substringIndex + attributeValue.length())).contains(true)) {
                                        //System.err.println("      Success: adding " + entry.getKey() + " at position " + substringIndex);
                                        attributes[substringIndex] = entry.getKey();
                                        attributeValues[substringIndex] = attributeValue;
                                        if(skips[substringIndex] == 0) skips[substringIndex] = attributeValue.length();
                                        for (int i = 0; i < attributeValue.length(); i++) {
                                            claimedChars[substringIndex + i] = true;
                                        }
                                    } else {
                                        //System.err.println("      ERROR: substring \"" + attributeValue + "\" overlaps claimed region!");
                                    }
                                } else {
                                    //System.err.println("      ERROR: mismatch in parent/child taint idxs: " + substringIndex + "/" + taintSublistIndex + "[" + String.join(", ", taintStrings) + "] vs [" + String.join(", ", attributeTaintStrings) + "]");
                                }
                            }
                        }
                    }
                } catch (ConcretizationException e) {
                    continue;
                }
            }
        }

        // Attempt to find parameters
        for (Map.Entry<String, AssignedValue> entry : callNode.callSnapshot.getParameters().entrySet()) {
            try {
                //System.err.println("        Checking " + entry.getKey() + ": " + entry.getValue());
                AssignedValue parameter = entry.getValue();
                // locate parameter in return value
                if (parameter.getValue() instanceof ClonedObjectValue) {
                    ClonedObjectValue objectValue = (ClonedObjectValue) parameter.getValue();
                    String attributeValue = objectValue.getConcretizedObject().toString();
                    int substringIndex = returnValueString.indexOf(attributeValue);
                    if (substringIndex >= 0) {
                        List<String> attributeTaintStrings = TaintHelper.taintArrayToStringList(objectValue.getTaints());
                        if (taintStrings == null || attributeTaintStrings == null) {
                            //System.err.println("      ERROR: taintStrins or descendantTaintStrings is NULL");
                            ;
                        } else {
                            int taintSublistIndex = Collections.indexOfSubList(taintStrings, attributeTaintStrings);
                            if (taintSublistIndex == substringIndex) {
                                if (!Arrays.asList(Arrays.copyOfRange(claimedChars, substringIndex, substringIndex + attributeValue.length())).contains(true)) {
                                    //System.err.println("      Success: adding " + entry.getKey() + " at position " + substringIndex);
                                    attributes[substringIndex] = entry.getKey();
                                    attributeValues[substringIndex] = attributeValue;
                                    if(skips[substringIndex] == 0) skips[substringIndex] = attributeValue.length();
                                    for (int i = 0; i < attributeValue.length(); i++) {
                                        claimedChars[substringIndex + i] = true;
                                    }
                                } else {
                                    //System.err.println("      ERROR: substring \"" + attributeValue + "\" overlaps claimed region!");
                                }
                            } else {
                                //System.err.println("      ERROR: mismatch in parent/child taint idxs: " + substringIndex + "/" + taintSublistIndex + "[" + String.join(", ", taintStrings) + "] vs [" + String.join(", ", attributeTaintStrings) + "]");
                            }
                        }
                    }
                }
            } catch (ConcretizationException e) {
                continue;
            } catch (NullPointerException e) {
                continue;
            }
        }

        //System.err.println("      Claims: " + Arrays.toString(claimedChars) + ", skips: " + Arrays.toString(skips));

        for (int i = 0; i < claimedChars.length; i++) {
            //System.out.println(i);
            UnparseTree.Node sr = subrules[i];
            String attr = attributes[i];
            String attrVal = attributeValues[i];
            String param = parameters[i];
            String paramVal = parameterValues[i];
            if (sr != null) {
                thisRule.descendants.add(sr);
                i+=skips[i]-1;
            } else if (attr != null && attrVal != null) {
                thisRule.descendants.add(new UnparseTree.Node(thisRule, attr, new TaintedValue<>(attrVal, null)));
                i+=skips[i]-1;
            } else if (param != null && paramVal != null) {
                thisRule.descendants.add(new UnparseTree.Node(thisRule, param, new TaintedValue<>(paramVal, null)));
                i+=skips[i]-1;
            } else if (!claimedChars[i]) {
                thisRule.descendants.add(new UnparseTree.Node(thisRule, TERMINAL, new TaintedValue<>(returnValueString.substring(i, i + 1), null)));
            }

        }

        return thisRule;
    }

    private static List<String> allSubstrings(String string) {
        List<String> substrings = new ArrayList<>();
        for (int i = 0; i < string.length(); i++) {
            for (int j = i + 1; j <= string.length(); j++) {
                substrings.add(string.substring(i, j));
            }
        }
        return substrings;
    }

    public static Set<String> extractTerminals(UnparseTree tree) {
        return extractTerminals(tree.root);
    }

    private static Set<String> extractTerminals(UnparseTree.Node node) {
        Set<String> terminals = new HashSet<>();

        String terminalFragment = "";
        for (UnparseTree.Node descendant : node.descendants) {
            if (TERMINAL.equals(descendant.name)) {
                terminalFragment = terminalFragment + descendant.value.value;
            } else {
                if (terminalFragment.length() > 0) {
                    terminals.addAll(allSubstrings(terminalFragment));
                    terminalFragment = "";
                }
                terminals.addAll(extractTerminals(descendant));
            }
        }

        if (terminalFragment.length() > 0) {
            terminals.addAll(allSubstrings(terminalFragment));
        }

        return terminals;
    }

    public static Set<String> findEnclosingTerminals(UnparseTree tree) {
        return findEnclosingTerminals(tree.root);
    }

    public static Set<String> findEnclosingTerminals(UnparseTree.Node node) {
        Set<String> terminals = new HashSet<>();
        boolean seenControlledValue = false;
        String concatenation = "";
        for (UnparseTree.Node descendant : node.descendants) {
            if (TERMINAL.equals(descendant.name)) {
                if (seenControlledValue) {
                    concatenation = concatenation + descendant.value.value;
                    terminals.add(concatenation);
                } else {
                    continue;
                }
            } else {
                terminals.addAll(findEnclosingTerminals(descendant));
                seenControlledValue = descendant.value.taints != null && Arrays.stream(descendant.value.taints).noneMatch(Objects::isNull);
                concatenation = "";
            }
        }
        return terminals;
    }
}
