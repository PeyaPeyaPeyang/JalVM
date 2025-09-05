package tokyo.peya.langjal.vm.engine.threading;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.values.metaobjects.VMStringObject;

public final class VMMainThread extends VMThread
{
    public VMMainThread(@NotNull JalVM vm, @NotNull VMThreadGroup group)
    {
        super(vm, group, "main");
    }

    public void startMainThread(@NotNull VMMethod entryPointMethod, @NotNull String[] args)
    {
        this.firstFrame = this.createFrame(
                entryPointMethod,
                true,
                VMStringObject.createStringArray(this, args)
        );
        this.currentFrame = this.firstFrame;

        this.firstFrame.activate();
    }
}
