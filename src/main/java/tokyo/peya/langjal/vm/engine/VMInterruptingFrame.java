package tokyo.peya.langjal.vm.engine;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.values.VMValue;

import java.util.function.Consumer;

@Getter
public class VMInterruptingFrame extends VMFrame
{
    private final @NotNull Consumer<? super VMValue> callback;

    public VMInterruptingFrame(@NotNull JalVM vm, @NotNull VMThread thread, @NotNull VMMethod method,
                               @NotNull VMValue[] args, @Nullable VMFrame prevFrame, @NotNull Consumer<? super VMValue> callback)
    {
        super(vm, thread, true, method, args, prevFrame);
        this.callback = callback;
    }
}
