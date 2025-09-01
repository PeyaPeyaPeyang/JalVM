package tokyo.peya.langjal.vm.engine;

import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import tokyo.peya.langjal.compiler.jvm.AccessAttribute;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.events.VMPanicOccurredEvent;
import tokyo.peya.langjal.vm.api.events.VMStepInEvent;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.stacking.VMStack;
import tokyo.peya.langjal.vm.engine.stacking.VMStackMachine;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.panics.InternalErrorVMPanic;
import tokyo.peya.langjal.vm.panics.PanicCreator;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.tracing.FrameTracingEntry;
import tokyo.peya.langjal.vm.tracing.VMValueTracer;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

@Getter
public class VMFrame implements VMComponent
{
    @Getter(AccessLevel.NONE)
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
        this.locals = new VMLocals(this,
                                   method.getMaxLocals(),
                                   method.getAccessAttributes().has(AccessAttribute.STATIC),
                                   method.getAccessAttributes().has(AccessAttribute.ENUM),
                                   args
        );

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

    public void setNextFrame(@Nullable VMFrame nextFrame)
    {
        this.nextFrame = nextFrame;
    }

    public void activate()
    {
        if (this.isRunning)
            throw new InternalErrorVMPanic("Frame is already running.");
        else if (this.interpreter != null)
            throw new InternalErrorVMPanic("Frame has already started by other interpreter.");

        this.interpreter = this.method.createInterpreter();
        this.isRunning = true;
    }

    public void rerunInstruction()
    {
        this.interpreter.stepBackward();
    }

    public void heartbeat()
    {
        if (!this.isRunning)
            throw new InternalErrorVMPanic("Frame is not running.");

        if (!this.interpreter.hasNextInstruction())
        {
            // 実行が終わったら，フレームを戻す
            this.isRunning = false;
            this.thread.restoreFrame();
            return;
        }

        try
        {
            AbstractInsnNode next = this.interpreter.feedNextInstruction();
            if (next == null)
                return; // そういうこともある。

            this.vm.getEventManager().dispatchEvent(new VMStepInEvent(
                    this,
                    next
            ));

            VMStackMachine.executeInstruction(this, next);
        }
        catch (VMPanic p)
        {
            this.handleOccurredPanic(p);
        }
        catch (Throwable e)
        {
            throw PanicCreator.createInternalPanic(
                    this,
                    "An internal VM error has occurred: " + e.getMessage(),
                    PanicCreator.copyThrowable(this, e)
            );
        }
    }


    private void handleOccurredPanic(@NotNull VMPanic panic)
    {
        VMPanicOccurredEvent event = new VMPanicOccurredEvent(this, panic);
        this.vm.getEventManager().dispatchEvent(event);

        boolean handled = this.handlePanic(panic);
        if (!handled)
            throw panic;  // 処理できなかった場合は，上に伝搬する

        // なお，finally については何もしない。
        // なぜならバイト・コード側が適切にジャンプを処理してくれるからである。
    }


    public boolean handlePanic(@NotNull VMPanic panic)
    {
        VMObject associatedThrowableObject = panic.getAssociatedThrowable();
        if (associatedThrowableObject == null)
            return false;  // 例外オブジェクトがない場合は処理ができないので，他に伝搬

        int currentIndex = this.interpreter.getCurrentInstructionIndex();
        VMClass throwableClass = associatedThrowableObject.getObjectType();
        ExceptionHandlerDirective handler =
                this.interpreter.getExceptionHandlerFor(currentIndex, throwableClass);
        if (handler == null)
            return false;  // ハンドラが見つからなかった場合もダメ

        this.thread.getTracer().pushHistory(
                FrameTracingEntry.exceptionThrown(
                        this,
                        panic,
                        handler
                )
        );

        // 例外オブジェクトをスタックに積んで，ハンドラの位置に飛ばす
        this.stack.push(associatedThrowableObject);
        int handlerIndex = handler.startInstructionIndex();
        this.interpreter.setCurrent(handlerIndex);

        return true;
    }

    protected void setReturnValue0(@NotNull VMValue value)
    {
        this.returnValue = value;
    }

    public void returnFromMethod(@NotNull VMValue value, @NotNull AbstractInsnNode returnInsn)
    {
        this.setReturnValue0(value);
        this.tracer.pushHistory(
                ValueTracingEntry.returning(value, this.method, returnInsn)
        );

        this.returnFromMethod();
    }

    public void returnFromMethod()
    {
        this.isRunning = false;
        this.thread.restoreFrame();
    }


    @Override
    public String toString()
    {
        return "Calling " + this.method + " with " +
                this.args.length + " arguments in frame of " + this.thread.getName() +
                (this.isVMDecree ? " (VM Decree)" : "") +
                (this.prevFrame != null ? ", previous frame: " + this.prevFrame.method.getMethodNode().name : "");
    }

    @Override
    public @NotNull JalVM getVM()
    {
        return this.vm;
    }

    private static void checkArgumentTypes(@NotNull VMMethod method, @NotNull VMValue[] args)
    {
        VMType<?>[] parameterTypes = method.getParameterTypes();
        int expectedArgs = parameterTypes.length;
        int actualArgs = args.length;

        boolean canOmitLastArray = method.getAccessAttributes().has(AccessAttribute.VARARGS);
        if (canOmitLastArray && expectedArgs > 0)
        {
            VMType<?> lastType = parameterTypes[expectedArgs - 1];
            if (lastType.getComponentType()  != null && actualArgs + 1 == expectedArgs)
            {
                // 最後の引数が配列である場合、最後の引数を省略できる
                expectedArgs--;
            }
        }

        if (expectedArgs != actualArgs)
            throw new InternalErrorVMPanic("Method " + method.getMethodNode().name + " expects " + expectedArgs +
                                      " arguments, but got " + actualArgs);

        for (int i = 0; i < parameterTypes.length; i++)
            if (!parameterTypes[i].isAssignableFrom(args[i].type()))
                throw new InternalErrorVMPanic("Argument " + i + " of method " + method.getMethodNode().name +
                                          " is of type " + args[i].type() + ", but expected " + parameterTypes[i] + ".");
    }

    public void jumpTo(@NotNull Label label, @NotNull AbstractInsnNode performer)
    {
        int labelIndex = this.interpreter.getLabelInstructionIndex(label);
        this.thread.getTracer().pushHistory(FrameTracingEntry.insideJump(
                this,
                performer,
                labelIndex
        ));
        this.interpreter.setCurrent(labelIndex );  // 次は，進めてから見るため，-1する
    }
}
