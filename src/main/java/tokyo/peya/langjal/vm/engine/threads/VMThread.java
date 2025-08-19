package tokyo.peya.langjal.vm.engine.threads;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.compiler.jvm.AccessAttribute;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.events.VMFrameInEvent;
import tokyo.peya.langjal.vm.api.events.VMFrameOutEvent;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.VMInterruptingFrame;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.exceptions.IllegalOperationPanic;
import tokyo.peya.langjal.vm.exceptions.LinkagePanic;
import tokyo.peya.langjal.vm.tracing.FrameTracingEntry;
import tokyo.peya.langjal.vm.tracing.VMFrameTracer;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Getter
public class VMThread
{
    protected final String name;
    protected final int currentFrameIndex;
    private final JalVM vm;
    private final VMFrameTracer tracer;

    @Getter
    protected VMFrame firstFrame;
    @Getter
    protected VMFrame currentFrame;


    public VMThread(@NotNull JalVM vm, @NotNull String name)
    {
        this.vm = vm;
        this.tracer = new VMFrameTracer();
        this.name = name;

        this.firstFrame = null;
        this.currentFrameIndex = 0;
        this.currentFrame = null;
    }

    public void runThread()
    {
        System.out.println("Thread[" + this.name + "] is running...");
        if (this.firstFrame == null)
            throw new LinkagePanic("No entry point method set. Cannot run the instructions!!!");

        this.firstFrame.activate();
    }

    public void heartbeat()
    {
        this.currentFrame.heartbeat();
    }

    public void invokeMethod(@NotNull VMMethod method, boolean isVMDecree, @Nullable VMObject thisObject, @NotNull VMValue... args)
    {
        // ネイティブの場合は，FFIで呼び出し
        if (method.getAccessAttributes().has(AccessAttribute.NATIVE))
        {
            VMType<?> returningType = VMType.of(method.getDescriptor().getReturnType());
            VMValue respond = this.vm.getNativeCaller().callFFI(
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
            newFrame.getLocals().setSlot(0, thisObject);  // this オブジェクトをローカル変数の最初のスロットにセット
        newFrame.activate();
    }

    public VMFrame createInterrupting(@NotNull VMMethod method, @NotNull Consumer<? super VMValue> callback, @NotNull VMValue... args)
    {
        VMInterruptingFrame newFrame = new VMInterruptingFrame(
                this.vm,
                this,
                method,
                args,
                this.currentFrame,
                callback
        );
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

    public VMFrame restoreFrame()
    {
        if (this.currentFrame == null)
            throw new IllegalOperationPanic("Frame underflow.");

        // 親フレームと戻り値を，スタックに積んでおく
        VMValue returnValue = this.currentFrame.getReturnValue();
        VMFrame prevFrame = this.currentFrame.getPrevFrame();
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

        this.tracer.pushHistory(FrameTracingEntry.frameOut(this.currentFrame));

        this.vm.getEventManager().dispatchEvent(new VMFrameOutEvent(this.vm, this.currentFrame, prevFrame));
        return this.currentFrame = prevFrame;
    }

    public void kill()
    {
        this.firstFrame = null;
        this.currentFrame = null;
    }

    public boolean isAlive()
    {
        return !(this.firstFrame == null || !this.firstFrame.isRunning());
    }

    public Stream<VMFrame> getFrames()
    {
        return Stream.iterate(this.firstFrame, Objects::nonNull, VMFrame::getNextFrame);
    }
}
