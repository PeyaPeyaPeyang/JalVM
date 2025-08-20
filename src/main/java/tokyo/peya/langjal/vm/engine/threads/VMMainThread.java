package tokyo.peya.langjal.vm.engine.threads;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMStringCreator;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

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
                createArgsArray(args)
        );
        this.currentFrame = this.firstFrame;

        this.firstFrame.activate();
    }

    private VMArray createArgsArray(@NotNull String[] args)
    {
        VMArray argsArray = new VMArray(VMType.STRING, args.length);
        for (int i = 0; i < args.length; i++)
            argsArray.set(i, VMStringCreator.createString(this, args[i]));

        return argsArray;
    }
}
