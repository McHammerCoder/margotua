package margotua.pipeline;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import com.sun.jdi.*;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;
import com.sun.jdi.event.*;
import com.sun.jdi.event.Event;
import com.sun.jdi.request.*;
import margotua.examples.SimpleJSON;
import margotua.tracing.*;

public class Tracer {
    private final static String PHOSPHOR_PARAMS = "-Xbootclasspath/a:lib/Phosphor-0.0.4-SNAPSHOT.jar -javaagent:lib/Phosphor-0.0.4-SNAPSHOT.jar=cacheDir=jre/classCache ";
    private final static String PHOSPHOR_METHOD_POSTFIX = "$$PHOSPHORTAGGED";

    private static final String[] classExclusionFilters = {"java.*", "sun.*", "jdk.*", "edu.columbia.cs.psl.phosphor.*"};
    public static final String ENTRY_REQUEST = "__ARMING";

    private Class targetClass = null;
    private String classpath;
    private String[] args;

    private VirtualMachine vm = null;
    private StepRequest stepRequest;
    private MethodExitRequest exitRequest;
    private boolean hasArmingBreakpoint = false;

    private List<BreakpointRequest> breakpoints;
    private boolean breakpointsState = false;

    private int recursionDepth;
    private String methodName = null;

    private LinkedList<Snapshot> stackFrameSnapshots;

    public Tracer(Class targetClass) {
        this(targetClass, null);
    }

    public Tracer(Class targetClass, String classpath) {
        this(targetClass, classpath, new String[]{});
    }

    public Tracer(Class targetClass, String classpath, String[] args) {
        this.targetClass = targetClass;
        this.classpath = classpath;
        this.args = args;
    }

    private void resetTracingState() {
        stackFrameSnapshots = new LinkedList<Snapshot>();
        breakpoints = new ArrayList<>();
        stepRequest = null;
        recursionDepth = -1;
    }

    private void launchVm() throws IOException, VMStartException, IllegalConnectorArgumentsException {
        resetTracingState();
        System.out.println("Working Directory = " +
                System.getProperty("user.dir"));
        LaunchingConnector launchingConnector = Bootstrap.virtualMachineManager().defaultConnector();
        Map<String, Connector.Argument> env = launchingConnector.defaultArguments();
        env.get("main").setValue(targetClass.getName()+ " " + String.join(" ", args));
        env.get("suspend").setValue("true");
        env.get("home").setValue("jre/jre-8-inst-p5");
        String options = PHOSPHOR_PARAMS+" -cp build/classes/java/main/";
        if(classpath != null) {
            options = options + classpath;
        }

        System.out.println(options);
        env.get("options").setValue(options);
        vm = launchingConnector.launch(env);
    }

    private void addPrepareHook() {
        ClassPrepareRequest classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest();
        classPrepareRequest.addClassFilter(targetClass.getName());
        classPrepareRequest.enable();
    }

    private void addPrepareHooks() {
        ClassPrepareRequest request = vm.eventRequestManager().createClassPrepareRequest();
        for(String filter : classExclusionFilters) {
            request.addClassExclusionFilter(filter);
        }
        request.enable();
    }

    private void addArmingBreakpoint(ClassPrepareEvent event) {
        if(!hasArmingBreakpoint && event.referenceType().name().equals(targetClass.getName())) {
            ReferenceType clazz = event.referenceType();
            List<Method> methods = clazz.methodsByName("run");
            assert (methods.size() == 1);
            Location methodStart = methods.get(0).location();

            BreakpointRequest request = vm.eventRequestManager().createBreakpointRequest(methodStart);
            request.putProperty(ENTRY_REQUEST, true);
            request.enable();
        }
    }

    private void addMethodCallHook() {
        MethodEntryRequest entryRequest = vm.eventRequestManager().createMethodEntryRequest();
        MethodExitRequest exitRequest = vm.eventRequestManager().createMethodExitRequest();

        for(String filter : classExclusionFilters) {
            entryRequest.addClassExclusionFilter(filter);
            exitRequest.addClassExclusionFilter(filter);
        }

        entryRequest.enable();
        exitRequest.enable();
    }

    private void addMethodCallBreakpoints(ReferenceType clazz) {
        for(Method method: clazz.methods()) {
            if(method.location() == null) {
                continue;
            }
            BreakpointRequest request = vm.eventRequestManager().createBreakpointRequest(method.location());
            request.putProperty(ENTRY_REQUEST, false);
            if(breakpointsState) {
                request.enable();;
            }
            breakpoints.add(request);
        }
    }

    private void enableMethodCallBreakpoints() {
        breakpointsState = true;
        for(BreakpointRequest request: breakpoints) {
            //System.err.println("enabling " + request.location().toString());
            request.enable();
        }
    }

    private void addMethodExitHook() {
        exitRequest = vm.eventRequestManager().createMethodExitRequest();

        for(String filter : classExclusionFilters) {
            exitRequest.addClassExclusionFilter(filter);
        }

        exitRequest.enable();
    }

    private void removeMethodExitHook() {
        if(exitRequest != null) {
            exitRequest.disable();
        }
    }

    private void disableMethodCallBreakpoints() {
        breakpointsState = false;
        for(BreakpointRequest request: breakpoints) {
            request.disable();
        }
    }

    private StackFrame getStackFrame(LocatableEvent event) throws IncompatibleThreadStateException{
        return event.thread().frame(0);
    }

    public static Map<LocalVariable, Value> getVisibleVariables(StackFrame stackFrame) throws AbsentInformationException {
        // Get values of all variables that are visible and print
        Map<LocalVariable, Value> visibleVariables = (Map<LocalVariable, Value>) stackFrame
                .getValues(stackFrame.visibleVariables());
        //System.out.println("Local Variables =");
        for (Map.Entry<LocalVariable, Value> entry : visibleVariables.entrySet()) {
            //System.out.println("	" + entry.getKey().name() + " = " + entry.getValue());
        }
        return visibleVariables;
    }

    private Snapshot handleEvent(Event event) throws IncompatibleThreadStateException{
        if (event instanceof VMStartEvent) {
            return handleEvent((VMStartEvent) event);
        } else if (event instanceof ClassPrepareEvent) {
            return handleEvent((ClassPrepareEvent) event);
        } else if (event instanceof BreakpointEvent) {
            return handleEvent((BreakpointEvent) event);
        } else if (event instanceof MethodEntryEvent) {
            return handleEvent((MethodEntryEvent) event);
        } else if (event instanceof StepEvent) {
            return handleEvent((StepEvent) event);
        } else if (event instanceof MethodExitEvent) {
            return handleEvent((MethodExitEvent) event);
        } else if (event instanceof VMDeathEvent) {
            return handleEvent((VMDeathEvent) event);
        } else {
            throw new IllegalArgumentException("Event type " + event.getClass() + " not supported!");
        }
    }

    private Snapshot handleEvent(VMStartEvent event) {
        return null;
    }

    /*private Snapshot handleEvent(ClassPrepareEvent event) {
        addMethodCallHook();
        return null;
    }*/

    private Snapshot handleEvent(ClassPrepareEvent event) {
        addArmingBreakpoint(event);
        addMethodCallBreakpoints(event.referenceType());
        return null;
    }

    private Snapshot handleEvent(BreakpointEvent event) throws IncompatibleThreadStateException {
        if(isTopLevelControlEvent(event)){
            //System.err.println("Got top lvl ctrl " + event.location().toString());
            if(recursionDepth < 0) {
                // first method call -> enable stepping
                if(stepRequest == null) {
                    stepRequest = event.virtualMachine().eventRequestManager().createStepRequest(
                            event.thread(), StepRequest.STEP_LINE, StepRequest.STEP_INTO);
                    stepRequest.addClassExclusionFilter("java.*");
                    stepRequest.addClassExclusionFilter("sun.*");
                    stepRequest.addClassExclusionFilter("jdk.*");
                    stepRequest.addClassExclusionFilter("edu.columbia.cs.psl.phosphor.*");
                }
                stepRequest.enable();

                // enable call breakpoints
                enableMethodCallBreakpoints();
                addMethodExitHook();
            }

            recursionDepth++;
        }

        if(stepRequest != null && stepRequest.isEnabled()) {
            return new CallSnapshot(getStackFrame(event));
        } else {
            return null;
        }
    }

    private boolean isTopLevelControlEvent(LocatableEvent event) {
        //if (event instanceof MethodEntryEvent || event instanceof MethodExitEvent){
        Object prop = event.request().getProperty(ENTRY_REQUEST);
        if ((event instanceof BreakpointEvent && prop != null && (Boolean)prop)
                || event instanceof MethodExitEvent){
            String methodName = event.location().method().name();
            String classNmae = event.location().declaringType().name();
            return (classNmae.equals(this.targetClass.getName() )) && (methodName.equals(this.methodName) || methodName.equals(this.methodName + PHOSPHOR_METHOD_POSTFIX));
        } else {
            //System.err.println("Not tracing: " + event.location().method().name());
            return false;
        }
    }

    private Snapshot handleEvent(MethodEntryEvent event) throws IncompatibleThreadStateException {
        //System.out.println(" ENTERING "+ event.location().declaringType().name()+"::"+ event.location().method().name()+":L"+ event.location().lineNumber() );

        if(isTopLevelControlEvent(event)) {
            if(recursionDepth < 0) {
                // first method call -> enable stepping
                if(stepRequest == null) {
                    stepRequest = event.virtualMachine().eventRequestManager().createStepRequest(
                            event.thread(), StepRequest.STEP_LINE, StepRequest.STEP_INTO);
                    stepRequest.addClassExclusionFilter("java.*");
                    stepRequest.addClassExclusionFilter("sun.*");
                    stepRequest.addClassExclusionFilter("jdk.*");
                    stepRequest.addClassExclusionFilter("edu.columbia.cs.psl.phosphor.*");
                }
                stepRequest.enable();
            }

            recursionDepth++;
        }

        if(stepRequest != null && stepRequest.isEnabled()) {
            return new CallSnapshot(getStackFrame(event));
        } else {
            return null;
        }
    }

    private Snapshot handleEvent(StepEvent event) throws IncompatibleThreadStateException {
        return new StepSnapshot(getStackFrame(event));
    }

    private Snapshot handleEvent(MethodExitEvent event) throws IncompatibleThreadStateException {
        Snapshot snap;
        //System.out.println(" EXITING "+ event.location().declaringType().name()+"::"+ event.location().method().name()+":L"+ event.location().lineNumber() +" Returning: " + ((event.returnValue() != null)?event.returnValue().toString():"<NULL>"));

        if(stepRequest != null && stepRequest.isEnabled()) {
            //System.out.println(" ===== "+ event.location().declaringType().name()+"::"+ event.location().method().name()+":L"+ event.location().lineNumber() +" Returning: " + ((event.returnValue() != null)?event.returnValue().toString():"<NULL>"));
            snap =  new ReturnSnapshot(getStackFrame(event), event.returnValue());
        } else {
            snap =  null;
        }

        if(isTopLevelControlEvent(event)) {
            recursionDepth--;

            if(recursionDepth < 0 && stepRequest != null) {
                //System.out.println(" DISABLING EXITS");
                // last method return -> disable stepping
                /*stepRequest.disable();*/
                disableMethodCallBreakpoints();
                removeMethodExitHook();
            }
        }

        return snap;
    }

    private Snapshot handleEvent(VMDeathEvent event) {
        return null;
    }

    public List<Snapshot> runTrace(String methodName){
        try {
            this.methodName = methodName;
            launchVm();
            //addPrepareHook();
            addPrepareHooks();
            vm.resume();

            EventSet eventSet = null;
            TRACE_LOOP: while((eventSet = vm.eventQueue().remove()) != null){
                for(Event event : eventSet) {
                    if(event instanceof VMDisconnectEvent) {
                        break TRACE_LOOP;
                    }

                    Snapshot frame = handleEvent(event);
                    if(frame != null) {
                        stackFrameSnapshots.add(frame);
                    }
                }

                vm.resume();
            }

            return stackFrameSnapshots;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream getTraceSubjectInputStream() {
        return vm.process().getInputStream();
    }

    public InputStream getTraceSubjectErrorStream() {
        return vm.process().getErrorStream();
    }

    private static String indent(int level) {
        int step_size = 4;
        return new String(new char[step_size*level]).replace("\0", " ");
    }

    static String getClasspathForLibraries() {
        ClassLoader cl = ClassLoader.getSystemClassLoader();

        URL[] urls = ((URLClassLoader)cl).getURLs();

        StringBuilder builder = new StringBuilder();
        for(URL url: urls){
            String lib = url.getFile();
            //System.err.println(lib);
            if(lib.contains(".gradle") || lib.contains("master.thesis") && lib.endsWith(".jar")){
                builder.append(':').append(lib);
            } else {
                //System.err.println("skipping...");
            }
        }
        return builder.toString();
    }

    public static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {

        byte[] buffer = new byte[1024];
        while (true) {
            int bytesRead = in.read(buffer);
            if (bytesRead == -1)
                break;
            out.write(buffer, 0, bytesRead);
        }
    }

    public static void dumpSnapshots(List<Snapshot> snapshots) {
        int indent = 0;

        for (Snapshot snapshot : snapshots) {
            if(snapshot instanceof CallSnapshot) {
                String prefix = String.join("", Collections.nCopies(indent, "    "));;
                CallSnapshot callSnapshot = (CallSnapshot) snapshot;
                System.out.print(prefix+">>> CALL ");
                System.out.println("Loc = " + snapshot.getMethodName() + ":" + snapshot.getLineNumber());
                for(Map.Entry<String, AssignedValue> entry : callSnapshot.getParameters().entrySet()) {
                    String suffix = entry.getValue().getTaintString();
                    if(suffix.length() > 0) {
                        suffix = ", " + suffix;
                    }
                    System.out.println(prefix+"    " + "  * " + entry.getKey() + "::" + entry.getValue().getType() + " = "+entry.getValue().toString() + suffix);
                }
                indent++;
            } else if( snapshot instanceof StepSnapshot) {
                    String prefix = String.join("", Collections.nCopies(indent, "    "));;
                    StepSnapshot stepSnapshot = (StepSnapshot) snapshot;
                    Map<String, AssignedValue> localVariables = stepSnapshot.getLocalVariables();
                    if(localVariables.size() == 0){
                        continue;
                    }

                    System.out.print(prefix+"+++ STEP ");
                    System.out.println("Loc = " + snapshot.getMethodName() + ":" + snapshot.getLineNumber());

                   for (Map.Entry<String, AssignedValue> entry : localVariables.entrySet()) {
                        String suffix = entry.getValue().getTaintString();
                        if(suffix.length() > 0) {
                            suffix = ", " + suffix;
                        }
                        System.out.println(prefix+"    " + "  * " + entry.getKey() + "::" + entry.getValue().getType() + " = "+entry.getValue().toString() + suffix);
                        //System.out.println(AssignedValue.rawDump(entry.getValue().getValue()));
                    }
            } else if(snapshot instanceof ReturnSnapshot) {
                indent--;
                String prefix = String.join("", Collections.nCopies(indent, "    "));;
                AssignedValue returnValue = ((ReturnSnapshot)snapshot).getReturnValue();
                System.out.print(prefix+"<<< RETN ");
                System.out.println("Loc = " + snapshot.getMethodName() + ":" + snapshot.getLineNumber());
                if (returnValue != null) {
                    String suffix = returnValue.getTaintString();
                    if (suffix.length() > 0) {
                        suffix = ", " + suffix;
                    }
                    System.out.println(prefix+"  = Return value::" + returnValue.getType() + " = " + returnValue.toString() + suffix);
                }
            }

        }
    }

    public static void main(String[] args) {
        try {
            //System.out.println("\n\n ===== Classpath: ===== \n\n");

            String classpath = getClasspathForLibraries();
            //System.out.println(classpath);

            //Tracer tracer = new Tracer(HelloWorld.class);
            Tracer tracer = new Tracer(SimpleJSON.class, classpath);
            //Tracer tracer = new Tracer(MinimalJSON.class, classpath);
            //Tracer tracer = new Tracer(PrimitiveTaints.class, classpath);

            System.out.println("\n\n ===== Trace: ===== \n\n");

            List<Snapshot> snapshots = tracer.runTrace("run");

            System.out.println("\n\n ===== Snapshots: ===== \n\n");
            dumpSnapshots(snapshots);

            System.out.println("\n\n ===== Debuggee out: ===== \n\n");
            copy(tracer.getTraceSubjectInputStream(), System.out);
            System.out.println("\n\n ===== Debuggee err: ===== \n\n");
            copy(tracer.getTraceSubjectErrorStream(), System.out);

            //System.out.println("\n\n ====== Mined tree: ====== \n\n");
            //System.out.println(Miner.mine(snapshots).toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
