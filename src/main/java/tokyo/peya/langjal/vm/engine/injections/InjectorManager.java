package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.references.ClassReference;

public class InjectorManager
{
    private static final Injector[] INJECTORS = {
            new InjectorClass(),
            new InjectorConsole(),
            new InjectorFileDescriptor(),
            new InjectorInet4Address(),
            new InjectorInet6Address(),
            new InjectorNativeEntryPoint(),
            new InjectorPerf(),
            new InjectorPrintStream(),
            new InjectorRandomAccessFile(),
            new InjectorReflection(),
            new InjectorSystem(),
            new InjectorThread(),
            new InjectorUnsafe(),
    };

    public Injector findInjector(@NotNull ClassReference reference)
    {
        for (Injector injector : INJECTORS)
            if (injector.suitableClass().equals(reference))
                return injector;
        return null;
    }

    public void injectClass(@NotNull VMSystemClassLoader classLoader, @NotNull VMClass clazz)
    {
        Injector injector = this.findInjector(clazz.getReference());
        if (injector == null)
            return;

        injector.inject(classLoader, clazz);
    }
}
