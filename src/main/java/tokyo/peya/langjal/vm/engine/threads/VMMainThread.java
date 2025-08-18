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
    private VMMethod entryPointMethod;
    private VMArray entryPointArgs;
    private boolean isStartPhaseConverged;

    public VMMainThread(@NotNull JalVM vm)
    {
        super(vm, "main");
    }


    public void startMainThread(@NotNull VMMethod entryPointMethod, @NotNull String[] args)
    {
        this.entryPointMethod = entryPointMethod;
        this.entryPointArgs = createArgsArray(args);
        this.isStartPhaseConverged = false;
    }

    @Override
    public void heartbeat()
    {
        super.heartbeat();
        if (this.currentFrame == null && !this.isStartPhaseConverged)
        {
            this.isStartPhaseConverged = true;
            this.invokeEntryPoint();
        }
    }

    private void invokeEntryPoint()
    {
        this.firstFrame = this.createFrame(
                this.entryPointMethod,
                true,
                this.entryPointArgs
        );
        this.currentFrame = this.firstFrame;
        this.firstFrame.activate();
    }

    private VMArray createArgsArray(@NotNull String[] args)
    {
        VMArray argsArray = new VMArray(VMType.STRING, args.length);
        for (int i = 0; i < args.length; i++)
            argsArray.set(i, VMStringCreator.createString(args[i]));

        return argsArray;
    }
}
