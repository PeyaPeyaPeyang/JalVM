package tokyo.peya.langjal.vm.engine;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;

@Getter
public class VMThread {
    protected final String threadName;
    protected final int currentFrameIndex;
    private final JalVM vm;
    @Getter
    protected VMFrame firstFrame;
    @Getter
    protected VMFrame currentFrame;

    public VMThread(@NotNull JalVM vm, @NotNull String name) {
        this.vm = vm;
        this.threadName = name;

        this.firstFrame = null;
        this.currentFrameIndex = 0;
        this.currentFrame = null;
    }

    public void runThread() {
        System.out.println("Thread[" + this.threadName + "] is running...");
        if (this.firstFrame == null)
            throw new IllegalStateException("No entry point method set. Cannot run the instructions!!!");

        this.firstFrame.startRunning();
    }

    public void heartbeat() {
        System.out.println("Thread[" + this.threadName + "] heartbeat.");
        this.currentFrame.heartbeat();
    }

    public VMFrame createFrame(@NotNull VMMethod method) {
        VMFrame newFrame = new VMFrame(
                vm,
                this,
                this.currentFrame,
                method
        );
        if (this.currentFrame != null)
            this.currentFrame.setNextFrame(newFrame);
        this.currentFrame = newFrame;

        return newFrame;
    }

    public VMFrame restoreFrame() {
        if (this.currentFrame == null)
            throw new IllegalStateException("Frame underflow.");
        return this.currentFrame = this.currentFrame.getPrevFrame();
    }

    public boolean isAlive() {
        return !(this.firstFrame == null || !this.firstFrame.isRunning());
    }
}
