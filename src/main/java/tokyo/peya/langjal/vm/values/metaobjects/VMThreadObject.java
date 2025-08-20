package tokyo.peya.langjal.vm.values.metaobjects;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.threads.VMThread;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMObject;

@Getter
public class VMThreadObject extends VMObject
{
    private final JalVM vm;
    private final VMThread thread;

    public VMThreadObject(@NotNull JalVM vm, @NotNull VMThread thread)
    {
        super(vm.getClassLoader().findClass(ClassReference.of("java/lang/Thread")));
        this.vm = vm;
        this.thread = thread;

        this.forceInitialise();
    }
}
