package margotua.pipeline;

import margotua.mining.UnparseTree;
import margotua.subject.TestSubject;
import margotua.tracing.Snapshot;
import margotua.tracking.CallTree;
import margotua.util.Formatter;

import java.util.*;

public class TerminalFuzzer {
    public static final int NUM_ATTEMPTS = 1000, INJECTION_MAX_SIZE = 5, NUM_RUNS = 1;

    public static void main(String[] args) {
        try {

            System.out.println("\n\n ===== Classpath: ===== \n\n");

            String classpath = Tracer.getClasspathForLibraries();
            System.out.println(classpath);

            Tracer tracer = new Tracer(TestSubject.class, classpath);

            System.out.println("\n\n ======== Trace: ========= \n\n");
            List<Snapshot> snapshots = tracer.runTrace("run");
            System.out.println("\n\n ===== Debuggee out: ===== \n\n");
            String output = Tracer.convertStreamToString(tracer.getTraceSubjectInputStream());
            System.out.println(output);
            System.out.println("\n\n ===== Debuggee err: ===== \n\n");
            Tracer.copy(tracer.getTraceSubjectErrorStream(), System.out);

            System.out.println("\n\n ====== Snapshots: ======= \n\n");
            Tracer.dumpSnapshots(snapshots);

            CallTree callTree = Tracker.track(snapshots);
            Tracker.injectStringBufferPseudoReturns(callTree);
            System.out.println("\n\n ====== Call tree: ======= \n\n");
            System.out.println(Formatter.removeEmptyBlocks(callTree.toString()));

            System.out.println("\n\n ====== Mined tree: ====== \n\n");
            UnparseTree unparseTree = Miner.mine(callTree);
            System.out.println(Formatter.removeEmptyBlocks(unparseTree.toString()));

            System.out.println("\n\n ====== Terminals: ======= \n\n");
            Set<String> terminals = Miner.extractTerminals(unparseTree);
            System.out.println("All terminals: " + terminals.toString());
            Set<String> enclosingTerminals = Miner.findEnclosingTerminals(unparseTree);

            System.out.println("Enclosing terminals: " + enclosingTerminals.toString());


            Object plain = TestSubject.parse(output);

            System.out.println("\n\n ====== Injecting: ======= \n\n");


            Object injected = null;
            int[] tries = new int[NUM_RUNS];
            Arrays.fill(tries, -1);
            for(int run = 0; run < NUM_RUNS; ++run) {
                for (int i = 1; i <= NUM_ATTEMPTS; i++) {
                    String prefix = getRandomSetElement(enclosingTerminals);
                    String terminalCombination = getRandomInjection(terminals, INJECTION_MAX_SIZE);
                    String injection = prefix + terminalCombination;
                    Object injectedObject = TestSubject.createObjectWithInjection(injection);
                    String injectedOutput = TestSubject.run(injectedObject);

                    //System.out.println("INJ: " + injection + "  --> " + injectedOutput);

                    injected = TestSubject.parse(injectedOutput);
                    if (injected != null && !TestSubject.encodeStructure(plain).equals(TestSubject.encodeStructure(injected))) {
                        System.out.println("success! took " + i + " tries, found injection: " + injection);
                        System.out.println(TestSubject.encodeStructure(plain) + "   VS   " + TestSubject.encodeStructure(injected));
                        tries[run] = i;
                        break;
                    } else {
                        injected = null;
                    }
                }
            }

            IntSummaryStatistics successes = Arrays.stream(tries).filter(attempts -> attempts > 0).summaryStatistics();
            System.out.println("successes: " + successes + " out of " + NUM_RUNS + " runs");

            if(injected == null) {
                System.err.println("Unable to find valid injection!");
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getRandomInjection(Set<String> terminals, int maxLength) {
        int length = 1 + new Random().nextInt(maxLength-1);
        StringBuffer buf = new StringBuffer(length);
        for(int i = 0; i < length; i++) {
            buf.append(getRandomSetElement(terminals));
        }
        return buf.toString();
    }

    private static <E> E getRandomSetElement(Set<E> set) {
        return set.stream().skip(new Random().nextInt(set.size())).findFirst().orElse(null);
    }

    private static String constructInjection(Set<String> terminals) {
        return String.join("", terminals);
    }
}
