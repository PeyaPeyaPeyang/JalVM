package tokyo.peya.langjal.vm.engine;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import tokyo.peya.langjal.compiler.jvm.AccessAttribute;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMInterpreter;
import tokyo.peya.langjal.vm.api.events.VMStepInEvent;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.stacking.VMStack;
import tokyo.peya.langjal.vm.engine.stacking.VMStackMachine;
import tokyo.peya.langjal.vm.engine.threads.VMThread;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.tracing.VMValueTracer;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

@Getter
public class VMFrame
{
    private final JalVM vm;
    private final VMThread thread;
    private final boolean isVMDecree;
    private final VMMethod method;
    private final VMValue[] args;
    private final VMValueTracer tracer;

    private final VMStack stack;
    private final VMLocals locals;

    private final VMFrame prevFrame;
    private VMFrame nextFrame;

    private VMInterpreter interpreter;
    private boolean isRunning;

    private VMValue returnValue;

    public VMFrame(
            @NotNull JalVM vm,
            @NotNull VMThread thread,
            boolean isVMDecree,
            @NotNull VMMethod method,
            @NotNull VMValue[] args,
            @Nullable VMFrame prevFrame)
    {
        this.vm = vm;
        this.thread = thread;
        this.isVMDecree = isVMDecree;  // VMが決めたフレームかどうか
        this.method = method;
        this.args = args;
        this.tracer = new VMValueTracer();

        this.prevFrame = prevFrame;

        checkArgumentTypes(method, args);
        this.stack = new VMStack(method.getMaxStackSize());
        this.locals = new VMLocals(this, method.getMaxLocals(), args);

        this.bookArgumentsHistory(args);
    }

    private void bookArgumentsHistory(@NotNull VMValue[] args)
    {
        for (VMValue arg : args)
            this.tracer.pushHistory(
                    ValueTracingEntry.passing(
                            arg,
                            this.method
                    )
            );
    }

    public void setNextFrame(@NotNull VMFrame nextFrame)
    {
        this.nextFrame = nextFrame;
    }

    public void activate()
    {
        if (this.isRunning)
            throw new VMPanic("Frame is already running.");
        else if (this.interpreter != null)
            throw new VMPanic("Frame has already started by other interpreter.");

        this.interpreter = this.method.createInterpreter(this.vm);
        this.isRunning = true;
    }

    public void heartbeat()
    {
        if (!this.isRunning)
            throw new VMPanic("Frame is not running.");

        if (this.interpreter.hasNextInstruction())
        {
            try
            {
                AbstractInsnNode next = this.interpreter.feedNextInstruction();
                if (next == null)
                    return;  // そういうこともある。

                this.vm.getEventManager().dispatchEvent(new VMStepInEvent(
                        this,
                        next
                ));

                VMStackMachine.executeInstruction(this, next);
            }
            catch (Throwable e)
            {
                System.err.println("Error while executing instruction in frame: " + e.getMessage());
                e.printStackTrace();
            }
        }
        else
        {
            // 実行が終わったら，フレームを戻す
            this.isRunning = false;
            this.thread.restoreFrame();
        }
    }

    protected void setReturnValue0(@NotNull VMValue value)
    {
        this.returnValue = value;
    }

    public void propagateReturningValue(@NotNull VMValue value, @NotNull AbstractInsnNode returnInsn)
    {
        this.setReturnValue0(value);
        this.tracer.pushHistory(
                ValueTracingEntry.returning(value, this.method, returnInsn)
        );
    }

    @Override
    public String toString()
    {
        return "Calling " + this.method.getMethodNode().name + " with " +
                this.args.length + " arguments in frame of " + this.thread.getName() +
                (this.isVMDecree ? " (VM Decree)" : "") +
                (this.prevFrame != null ? ", previous frame: " + this.prevFrame.method.getMethodNode().name : "");
    }

    private static void checkArgumentTypes(@NotNull VMMethod method, @NotNull VMValue[] args)
    {
        VMType[] parameterTypes = method.getParameterTypes();
        int expectedArgs = parameterTypes.length;
        int actualArgs = args.length;

        boolean needThis = !method.getAccessAttributes().has(AccessAttribute.STATIC);
        if (needThis)
            expectedArgs++;  // インスタンスメソッドの場合、this引数があるので1つ多い
        boolean canOmitLastArray = method.getAccessAttributes().has(AccessAttribute.VARARGS);
        if (canOmitLastArray && expectedArgs > 0)
        {
            VMType lastType = parameterTypes[expectedArgs - 1];
            if (lastType.getArrayDimensions() > 0 && actualArgs + 1 == expectedArgs)
            {
                // 最後の引数が配列である場合、最後の引数を省略できる
                expectedArgs--;
            }
        }

        if (expectedArgs != actualArgs)
            throw new VMPanic("Method " + method.getMethodNode().name + " expects " + expectedArgs +
                                      " arguments, but got " + actualArgs + (needThis ? ", missing 'this' at first?": "."));

        for (int i = 0; i < parameterTypes.length; i++)
            if (!args[i].type().isAssignableFrom(parameterTypes[i]))
                throw new VMPanic("Argument " + i + " of method " + method.getMethodNode().name +
                                          " is of type " + args[i].type() + ", but expected " + parameterTypes[i] + ".");
    }

    public void jumpTo(@NotNull Label label)
    {
        int labelIndex = this.interpreter.getLabelInstructionIndex(label);
        if (labelIndex < 0)
            throw new VMPanic("Label " + label + " not found in method " + this.method.getMethodNode().name);

        this.interpreter.setCurrent(labelIndex);
    }
}
