package tokyo.peya.langjal.vm.engine.threading;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.metaobjects.VMStringObject;

public class VMMainThread extends VMThread
{
    public VMMainThread(@NotNull JalVM vm)
    {
        super(vm, "main");
    }

    public void startMainThread(@NotNull VMMethod entryPointMethod, @NotNull String[] args)
    {
        this.firstFrame = this.createFrame(
                entryPointMethod,
                true,
                VMStringObject.createStringArray(entryPointMethod.getOwningClass().getClassLoader(), args)
        );
        this.currentFrame = this.firstFrame;

        this.firstFrame.activate();
    }
}
