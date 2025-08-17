package tokyo.peya.langjal.vm.engine.threads;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.events.VMFrameInEvent;
import tokyo.peya.langjal.vm.api.events.VMFrameOutEvent;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.exceptions.IllegalOperationPanic;
import tokyo.peya.langjal.vm.exceptions.LinkagePanic;
import tokyo.peya.langjal.vm.tracing.FrameManipulationType;
import tokyo.peya.langjal.vm.tracing.FrameTracingEntry;
import tokyo.peya.langjal.vm.tracing.VMFrameTracer;
import tokyo.peya.langjal.vm.values.VMValue;

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

    public VMFrame invokeMethod(@NotNull VMMethod method, boolean isVMDecree, @NotNull VMValue... args)
    {
        VMFrame newFrame = this.createFrame(method, isVMDecree, args);
        this.currentFrame = newFrame;
        newFrame.activate();
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

        this.tracer.pushHistory(
                new FrameTracingEntry(
                        FrameManipulationType.FRAME_IN,
                        newFrame
                )
        );

        return newFrame;
    }

    public VMFrame restoreFrame()
    {
        if (this.currentFrame == null)
            throw new IllegalOperationPanic("Frame underflow.");

        // 親フレームと戻り値を，スタックに積んでおく
        VMFrame prevFrame = this.currentFrame.getPrevFrame();
        if (this.currentFrame.isVMDecree())
        {
            VMValue returnValue = this.currentFrame.getReturnValue();
            if (!(returnValue == null || prevFrame == null))
                prevFrame.getStack().push(returnValue);
        }

        this.tracer.pushHistory(
                new FrameTracingEntry(
                        FrameManipulationType.FRAME_OUT,
                        this.currentFrame
                )
        );
        this.vm.getEventManager().dispatchEvent(new VMFrameOutEvent(this.vm, this.currentFrame, prevFrame));
        return this.currentFrame = prevFrame;
    }

    public void onDestruction()
    {
        this.firstFrame = null;
        this.currentFrame = null;
    }

    public boolean isAlive()
    {
        return !(this.firstFrame == null || !this.firstFrame.isRunning());
    }
}
