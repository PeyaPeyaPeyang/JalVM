package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMStringObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class InjectorSignal implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("jdk/internal/misc/Signal");

    private int lastSignalHandle;
    private final Map<String, Integer> signalHandleMap;
    private final Map<Integer, Long> signalHandler;

    @Override
    public ClassReference suitableClass()
    {
        return CLAZZ;
    }

    public InjectorSignal()
    {
        this.signalHandleMap = new HashMap<>();

        // とりあえずPOSIX
        int lastSignalHandle = 0;
        this.signalHandleMap.put("HUP", ++lastSignalHandle);    // Hangup
        this.signalHandleMap.put("INT", ++lastSignalHandle);    // Interrupt
        this.signalHandleMap.put("QUIT", ++lastSignalHandle);   // Quit
        this.signalHandleMap.put("ILL", ++lastSignalHandle);    // Illegal instruction
        this.signalHandleMap.put("ABRT", ++lastSignalHandle);   // Abort
        this.signalHandleMap.put("FPE", ++lastSignalHandle);    // Floating point exception
        this.signalHandleMap.put("KILL", ++lastSignalHandle);   // Kill
        this.signalHandleMap.put("SEGV", ++lastSignalHandle);  // Segmentation fault
        this.signalHandleMap.put("PIPE", ++lastSignalHandle);  // Broken pipe
        this.signalHandleMap.put("ALRM", ++lastSignalHandle);  // Alarm clock
        this.signalHandleMap.put("TERM", ++lastSignalHandle);  // Termination request
        this.signalHandleMap.put("USR1", ++lastSignalHandle);  // User-defined signal ++lastSignalHandle
        this.signalHandleMap.put("USR2", ++lastSignalHandle);  // User-defined signal ++lastSignalHandle
        this.signalHandleMap.put("CHLD", ++lastSignalHandle);  // Child stopped or terminated
        this.signalHandleMap.put("CONT", ++lastSignalHandle);  // Continue if stopped
        this.signalHandleMap.put("STOP", ++lastSignalHandle);  // Stop process
        this.signalHandleMap.put("TSTP", ++lastSignalHandle);  // Terminal stop
        this.signalHandleMap.put("TTIN", ++lastSignalHandle);  // Background read from tty
        this.signalHandleMap.put("TTOU", ++lastSignalHandle);  // Background write to tty
        this.signalHandleMap.put("URG", ++lastSignalHandle);   // Urgent condition on socket
        this.signalHandleMap.put("XCPU", ++lastSignalHandle);  // CPU time
        this.signalHandleMap.put("XFSZ", ++lastSignalHandle);  // File
        this.signalHandleMap.put("VTALRM", ++lastSignalHandle); // Virtual timer expired
        this.signalHandleMap.put("PROF", ++lastSignalHandle);  // Profiling
        this.signalHandleMap.put("WINCH", ++lastSignalHandle); // Window size change
        this.signalHandleMap.put("GIO", ++lastSignalHandle);   // I/O possible
        this.signalHandleMap.put("PWR", ++lastSignalHandle);   // Power failure
        this.signalHandleMap.put("SYS", ++lastSignalHandle);   // Bad system call
        this.lastSignalHandle = lastSignalHandle;

        this.signalHandler = new HashMap<>();
    }

    public int getSignalOrCreateHandle(@NotNull String signalName)
    {
        Integer handle = this.signalHandleMap.get(signalName);
        if (handle == null)
        {
            handle = ++this.lastSignalHandle;
            this.signalHandleMap.put(signalName, handle);
        }
        return handle;
    }

    @Override
    public void inject(@NotNull VMSystemClassLoader cl, @NotNull VMClass clazz)
    {
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "findSignal0",
                        "(Ljava/lang/String;)I",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMStringObject signalNameObject = (VMStringObject) args[0];
                        String signalName = signalNameObject.getString();
                        int handle = InjectorSignal.this.getSignalOrCreateHandle(signalName);
                        return new VMInteger(frame, handle);
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "handle0",
                        "(IJ)J",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMInteger signalHandleValue = (VMInteger) args[0];
                        VMLong handlerValue = (VMLong) args[1];
                        int signalHandle = signalHandleValue.asNumber().intValue();
                        long handler = handlerValue.asNumber().longValue();

                        Long current = InjectorSignal.this.signalHandler.put(signalHandle, handler);
                        // すでにハンドラが登録されている場合は、古いハンドラを返す
                        // 新しいハンドラを登録した場合は、0を返す
                        return new VMLong(frame, Objects.requireNonNullElse(current, 0L));
                    }
                }
        );
    }

}
