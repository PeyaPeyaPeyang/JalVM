package tokyo.peya.langjal.vm.engine.threading;

import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.compiler.jvm.AccessAttribute;
import tokyo.peya.langjal.compiler.jvm.MethodDescriptor;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.events.VMFrameInEvent;
import tokyo.peya.langjal.vm.api.events.VMFrameOutEvent;
import tokyo.peya.langjal.vm.api.events.VMThreadChangeStateEvent;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.VMInterruptingFrame;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.scheduler.TaskScheduler;
import tokyo.peya.langjal.vm.panics.IllegalOperationPanic;
import tokyo.peya.langjal.vm.panics.InternalErrorVMPanic;
import tokyo.peya.langjal.vm.panics.LinkagePanic;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.tracing.FrameTracingEntry;
import tokyo.peya.langjal.vm.tracing.VMFrameTracer;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMThreadObject;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Getter
public class VMThread implements VMComponent
{
    @Getter(AccessLevel.NONE)
    protected final JalVM vm;
    protected final VMThreadGroup group;
    protected final String name;
    private final long id;

    protected final int currentFrameIndex;
    protected final VMFrameTracer tracer;
    protected final VMThreadObject threadObject;

    protected VMFrame firstFrame;
    protected VMFrame currentFrame;

    protected VMThreadState state;
    protected VMMonitor acquiringMonitor;

    protected int priority;
    private boolean daemon;

    private TaskScheduler scheduler;

    public VMThread(@NotNull JalVM vm, @NotNull VMThreadGroup group, @NotNull String name)
    {
        this.vm = vm;
        this.name = name;
        this.group = group;
        if (this instanceof VMMainThread)
            this.id = 3;
        else
            this.id = vm.getEngine().incrementAndGetThreadID();

        this.tracer = new VMFrameTracer();
        this.threadObject = new VMThreadObject(vm, this);

        this.state = VMThreadState.NEW;
        this.firstFrame = null;
        this.currentFrameIndex = 0;
        this.currentFrame = null;
    }

    public TaskScheduler getScheduler()
    {
        if (this.scheduler == null)
            this.scheduler = new TaskScheduler();  // 遅延初期化
        return this.scheduler;
    }

    public void heartbeat()
    {
        boolean isRunnable = !(this.firstFrame == null || !this.firstFrame.isRunning());
        switch (this.state)
        {
            case TERMINATED:
                break;
            case NEW:
                if (isRunnable)
                    this.setState(VMThreadState.RUNNABLE);
                else
                {
                    this.setState(VMThreadState.TERMINATED);
                    break;
                }
                /* fall-through */
            case RUNNABLE:
                if (!isRunnable)
                {
                    this.setState(VMThreadState.TERMINATED);
                    break;
                }

                this.heartbeatCurrentFrame();
                break;
        }

        if (this.scheduler != null)
            this.scheduler.heartbeat();
    }

    private void heartbeatCurrentFrame()
    {
        try
        {
            this.currentFrame.heartbeat();
        }
        catch (VMPanic p)
        {
            boolean handled = this.handlePanic(this.currentFrame, p);
            if (!handled)
                this.handleUncaughtPanic(p);
        }
    }

    private boolean handlePanic(@NotNull VMFrame occurred, @NotNull VMPanic p)
    {
        if (p.getAssociatedThrowable() == null)
            return false;

        VMFrame current = occurred.getPrevFrame();  // 親フレームから探索開始
        while(current != null)
        {
            boolean handled = current.handlePanic(p);
            if (handled)
                return true;

            current = current.getPrevFrame();
            this.killFrame();  // ハンドルされなかったフレームは破棄していく
        }

        // どの親でもハンドルされなかった場合は，スレッドを終了させる
        return false;
    }

    private void handleUncaughtPanic(@NotNull VMPanic panic)
    {
        VMObject associatedThrowable = panic.getAssociatedThrowable();
        if (associatedThrowable == null)
            throw new InternalErrorVMPanic("Cannot handle uncaught panic without associated Throwable object.", panic);

        // Thread.dispatchUncaughtException() を呼び出す
        VMMethod dispatchUncaught = this.threadObject.getObjectType().findMethod(
                "dispatchUncaughtException",
                MethodDescriptor.parse("(Ljava/lang/Throwable;)V")
        );
        if (dispatchUncaught == null)
            throw new InternalErrorVMPanic(new LinkagePanic("Cannot find Thread.dispatchUncaughtException(Throwable) method."));

        this.invokeInterrupting(
                dispatchUncaught,
                (_) -> this.kill(),  // 呼び出しが終わったらスレッドを終了させる
                this.threadObject,
                associatedThrowable
        );
    }



    public void invokeMethod(@NotNull VMMethod method, boolean isVMDecree, @Nullable VMObject thisObject, @NotNull VMValue... args)
    {
        // ネイティブの場合は，FFIで呼び出し
        if (method.getAccessAttributes().has(AccessAttribute.NATIVE))
        {
            VMType<?> returningType = VMType.of(this.vm, method.getDescriptor().getReturnType());
            VMValue respond = this.vm.getNativeCaller().callFFI(
                    this,
                    method.getClazz().getReference(),
                    method.getName(),
                    returningType,
                    args
            );
            this.currentFrame.getStack().push(respond);
            return;
        }

        VMFrame newFrame = this.createFrame(method, isVMDecree, args);
        if (thisObject != null)
            newFrame.getLocals().setArgumentSlot(0, thisObject);  // this オブジェクトをローカル変数の最初のスロットにセット
        newFrame.activate();
    }

    public VMFrame invokeInterrupting(@NotNull VMMethod method, @NotNull Consumer<? super VMValue> callback, @NotNull VMValue... args)
    {
        return this.invokeInterrupting(method, callback, null, args);
    }
    public VMFrame invokeInterrupting(@NotNull VMMethod method, @NotNull Consumer<? super VMValue> callback, @Nullable VMObject thisObject, @NotNull VMValue... args)
    {
        VMInterruptingFrame newFrame = new VMInterruptingFrame(
                this.vm,
                this,
                method,
                args,
                this.currentFrame,
                callback
        );
        if (thisObject != null)
            newFrame.getLocals().setArgumentSlot(0, thisObject);  // this オブジェクトをローカル変数の最初のスロットにセット

        newFrame.activate();
        if (this.firstFrame == null)
            this.firstFrame = newFrame;
        else if (this.currentFrame != null)
            this.currentFrame.setNextFrame(newFrame);

        this.currentFrame = newFrame;
        return newFrame;
    }

    public VMFrame createFrame(@NotNull VMMethod method, boolean isVMDecree, @NotNull VMValue... args)
    {
        VMFrame newFrame = new VMFrame(
                this.vm,
                this,
                isVMDecree,
                method,
                args,
                this.currentFrame
        );
        if (this.firstFrame == null)
            this.firstFrame = newFrame;
        else if (this.currentFrame != null)
            this.currentFrame.setNextFrame(newFrame);

        this.vm.getEventManager().dispatchEvent(new VMFrameInEvent(this.vm, newFrame));
        this.currentFrame = newFrame;

        this.tracer.pushHistory(FrameTracingEntry.frameIn(newFrame));

        return newFrame;
    }

    public void restoreFrame()
    {
        if (this.currentFrame == null)
            throw new InternalErrorVMPanic("Current frame is already null. Cannot restore frame.");

        // 親フレームと戻り値を，スタックに積んでおく
        VMValue returnValue = this.currentFrame.getReturnValue();
        if (this.currentFrame instanceof VMInterruptingFrame interruptingFrame)
            interruptingFrame.getCallback().accept(returnValue);
        else if (returnValue != null)
        {
            VMFrame returningFrame = this.currentFrame.getPrevFrame();
            do
            {
                if (returningFrame.getNextFrame() == this.currentFrame)
                {
                    returningFrame.getStack().push(returnValue);
                    break;
                }

                returningFrame = returningFrame.getPrevFrame();
            }
            while (returningFrame != null);
        }

        // フレームを破棄して，親フレームに戻る
        this.killFrame();
    }

    private void killFrame()
    {
        if (this.currentFrame == null)
            return;

        VMFrame prevFrame = this.currentFrame.getPrevFrame();
        if (prevFrame != null)
            prevFrame.setNextFrame(null);

        this.tracer.pushHistory(FrameTracingEntry.frameOut(this.currentFrame));
        this.vm.getEventManager().dispatchEvent(new VMFrameOutEvent(this.vm, this.currentFrame, prevFrame));

        this.currentFrame = prevFrame;
    }

    public void kill()
    {
        this.setState(VMThreadState.TERMINATED);
        this.firstFrame = null;
        this.currentFrame = null;
    }

    public void setState(@NotNull VMThreadState state)
    {
        if (this.state == state)
            return;

        VMThreadChangeStateEvent stateChangeEvent = new VMThreadChangeStateEvent(
                this.vm,
                this,
                this.state,
                state
        );
        this.vm.getEventManager().dispatchEvent(stateChangeEvent);
        this.state = state;

        this.threadObject.syncStateField();
    }

    public void setDaemon(boolean daemon)
    {
        this.daemon = daemon;
        this.threadObject.syncFields();
    }

    public void setPriority(int priority)
    {
        if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY)
            throw new IllegalOperationPanic("Thread priority must be between " + Thread.MIN_PRIORITY + " and " + Thread.MAX_PRIORITY + ", but got " + priority + ".");

        this.priority = priority;
        this.threadObject.syncFields();
    }

    public Stream<VMFrame> getFrames()
    {
        return Stream.iterate(this.firstFrame, Objects::nonNull, VMFrame::getNextFrame);
    }

    @Override
    public @NotNull JalVM getVM()
    {
        return this.vm;
    }
}
