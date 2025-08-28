package tokyo.peya.langjal.vm.values.metaobjects.reflection;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMValue;

@Getter
public class VMConstructorObject extends VMMethodObject
{
    private final VMSystemClassLoader classLoader;
    private final VMMethod method;

    public VMConstructorObject(@NotNull VMSystemClassLoader classLoader, @NotNull VMMethod method)
    {
        super(classLoader, method, classLoader.findClass(ClassReference.of("java/lang/reflect/Constructor")));
        this.classLoader = classLoader;
        this.method = method;
    }

    public void call(@NotNull VMThread thread, @NotNull VMClass caller, @NotNull VMObject instance,
                     @NotNull VMValue[] args, boolean isVMDecree)
    {
        instance.initialiseInstance(
                thread,
                caller,
                this.method,
                args,
                isVMDecree
        );
    }
}
