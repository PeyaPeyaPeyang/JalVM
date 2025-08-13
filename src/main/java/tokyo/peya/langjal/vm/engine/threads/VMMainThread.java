package tokyo.peya.langjal.vm.engine.threads;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.values.*;

public class VMMainThread extends VMThread {

    public VMMainThread(@NotNull JalVM vm) {
        super(vm, "main");
    }

    private void sendFrame(@NotNull VMMethod entryPointMethod, @NotNull VMArray args) {
        this.firstFrame = this.createFrame(entryPointMethod, new VMValue[]{args});
        this.currentFrame = this.firstFrame;

        this.runThread();
    }

    public VMFrame startMainThread(@NotNull VMMethod entryPointMethod, @NotNull String[] args) {
        if (this.firstFrame != null)
            throw new IllegalStateException("VM is already started. Cannot start again!");

        this.sendFrame(entryPointMethod, createArgsArray(args));
        return this.firstFrame;
    }

    private VMArray createArgsArray(@NotNull String[] args) {
        VMArray argsArray = new VMArray(VMType.STRING, 1);
        for (int i = 0; i < args.length; i++)
            argsArray.set(i, VMStringCreator.createString(this, args[i]));

        return argsArray;
    }
}
