package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMObject;

public interface Injector
{
    @NotNull
    ClassReference suitableClass();

    void inject(@NotNull VMSystemClassLoader cl, @NotNull VMClass clazz);
}
