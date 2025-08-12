package tokyo.peya.langjal.vm.engine.threads;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMMethod;

public class VMMainThread extends VMThread {

    public VMMainThread(@NotNull JalVM vm) {
        super(vm, "main");
    }

    public VMFrame executeEntryPointMethod(@NotNull VMMethod entryPointMethod) {
        this.firstFrame = createFrame(entryPointMethod);
        this.currentFrame = this.firstFrame;

        this.runThread();
        return this.firstFrame;
    }
}
