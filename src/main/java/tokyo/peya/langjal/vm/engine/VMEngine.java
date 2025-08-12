package tokyo.peya.langjal.vm.engine;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMInterpreter;

public class VMEngine {
    private final JalVM vm;

    @Getter
    private Frame firstFrame;
    private final int currentFrameIndex;
    @Getter
    private Frame currentFrame;

    public VMEngine(@NotNull JalVM vm) {
        this.vm = vm;
        this.firstFrame = null;
        this.currentFrameIndex = 0;
        this.currentFrame = null;
    }

    public void run()
    {
        System.out.println("VMEngine is running...");
    }

    public Frame createFrame(@NotNull VMMethod method) {
        Frame newFrame = new Frame(
                vm,
                this,
                this.currentFrame,
                method,
                this.currentFrameIndex + 1
        );
        if (this.currentFrame != null)
            this.currentFrame.setNextFrame(newFrame);
        this.currentFrame = newFrame;

        if (this.firstFrame != null) // first への代入は execFirst で
            this.currentFrame.setPrevFrame(this.currentFrame);

        return newFrame;
    }

    public Frame restoreFrame()
    {
        if (this.currentFrame == null)
            throw new IllegalStateException("Frame underflow.");
        return this.currentFrame = this.currentFrame.getPrevFrame();
    }

    public Frame executeEntryPointMethod(@NotNull VMMethod entryPointMethod) {
        this.firstFrame = createFrame(entryPointMethod);
        this.currentFrame = this.firstFrame;

        this.run();
        return this.firstFrame;
    }
}
