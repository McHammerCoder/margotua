package margotua.pipeline;

import margotua.mining.UnparseTree;
import margotua.examples.*;
import margotua.tracing.Snapshot;
import margotua.tracking.CallTree;
import margotua.util.Formatter;

import java.util.List;

public class Runner {
    public static void main(String args[]) {
        try {
            //System.out.println("\n\n ===== Classpath: ===== \n\n");

            String classpath = Tracer.getClasspathForLibraries();
            //System.out.println(classpath);

            //Tracer tracer = new Tracer(HelloWorld.class);
            //Tracer tracer = new Tracer(SimpleJSON.class, classpath);
            //Tracer tracer = new Tracer(MinimalJSON.class, classpath);
            //Tracer tracer = new Tracer(PrimitiveTaints.class, classpath);
            //Tracer tracer = new Tracer(StringBufferTaints.class, classpath);
            //Tracer tracer = new Tracer(EoYaml.class, classpath);
            Tracer tracer = new Tracer(Csv.class, classpath);
            //Tracer tracer = new Tracer(GalimatiasUrl.class, classpath);

            System.out.println("\n\n ===== Trace: ===== \n\n");
            List<Snapshot> snapshots = tracer.runTrace("run");
            System.out.println("\n\n ===== Debuggee out: ===== \n\n");
            Tracer.copy(tracer.getTraceSubjectInputStream(), System.out);
            System.out.println("\n\n ===== Debuggee err: ===== \n\n");
            Tracer.copy(tracer.getTraceSubjectErrorStream(), System.out);

            System.out.println("\n\n ====== Snapshots:  ====== \n\n");
            Tracer.dumpSnapshots(snapshots);

            CallTree callTree = Tracker.track(snapshots);
            Tracker.injectStringBufferPseudoReturns(callTree);
            System.out.println("\n\n ====== Call tree:  ====== \n\n");
            System.out.println(Formatter.removeEmptyBlocks(callTree.toString()));

            System.out.println("\n\n ====== Mined tree: ====== \n\n");
            UnparseTree unparseTree = Miner.mine(callTree);
            System.out.println(Formatter.removeEmptyBlocks(unparseTree.toString()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
