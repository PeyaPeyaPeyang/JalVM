package tokyo.peya.langjal.vm.engine.threads;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.values.VMValue;

@Getter
public class VMThread {
    protected final String name;
    protected final int currentFrameIndex;
    private final JalVM vm;
    @Getter
    protected VMFrame firstFrame;
    @Getter
    protected VMFrame currentFrame;

    public VMThread(@NotNull JalVM vm, @NotNull String name) {
        this.vm = vm;
        this.name = name;

        this.firstFrame = null;
        this.currentFrameIndex = 0;
        this.currentFrame = null;
    }

    public void runThread() {
        System.out.println("Thread[" + this.name + "] is running...");
        if (this.firstFrame == null)
            throw new IllegalStateException("No entry point method set. Cannot run the instructions!!!");

        this.firstFrame.activate();
    }

    public void heartbeat() {
        this.currentFrame.heartbeat();
    }

    public VMFrame invokeMethod(@NotNull VMMethod method, @NotNull VMValue... args) {
        if (this.currentFrame != null)
            throw new IllegalStateException("Current frame is already set. Cannot create a new one!");

        VMFrame newFrame = this.createFrame(method, args);
        this.currentFrame = newFrame;
        newFrame.activate();
        return newFrame;
    }

    public VMFrame createFrame(@NotNull VMMethod method, @NotNull VMValue... args) {
        VMFrame newFrame = new VMFrame(
                vm,
                this,
                method,
                args,
                this.currentFrame
        );
        if (this.firstFrame == null)
            this.firstFrame = newFrame;
        else if (this.currentFrame != null)
            this.currentFrame.setNextFrame(newFrame);

        this.currentFrame = newFrame;

        return newFrame;
    }

    public VMFrame restoreFrame() {
        if (this.currentFrame == null)
            throw new IllegalStateException("Frame underflow.");

        // 親フレームと戻り値を，スタックに積んでおく
        VMFrame prevFrame = this.currentFrame.getPrevFrame();
        VMValue returnValue = this.currentFrame.getReturnValue();
        if (!(returnValue == null || prevFrame == null))
            prevFrame.getStack().push(returnValue);

        return this.currentFrame = prevFrame;
    }

    public boolean isAlive() {
        return !(this.firstFrame == null || !this.firstFrame.isRunning());
    }
}
