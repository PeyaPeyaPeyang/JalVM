package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threads.VMThread;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMValue;

public abstract class InjectedMethod extends VMMethod {
    public InjectedMethod(@NotNull VMClass clazz, @NotNull MethodNode methodNode) {
        super(clazz, methodNode);
    }

    @Override
    public void invokeVirtual(@NotNull VMThread thread, @Nullable VMClass caller, @NotNull VMObject instance, boolean isVMDecree, @NotNull VMValue... args) {
        VMValue returning = this.invoke(thread, caller, instance, args);
        if (returning != null)
            thread.getCurrentFrame().getStack().push(returning);
    }

    @Override
    public void invokeStatic(@NotNull VMThread thread, @Nullable VMClass caller, boolean isVMDecree, @NotNull VMValue... args) {
        VMValue returning = this.invoke(thread, caller, null, args);
        if (returning != null)
            thread.getCurrentFrame().getStack().push(returning);
    }

    @Nullable
    abstract VMValue invoke(
            @NotNull VMThread thread,
            @Nullable VMClass caller,
            @Nullable VMObject instance,
            @NotNull VMValue[] args);
}
