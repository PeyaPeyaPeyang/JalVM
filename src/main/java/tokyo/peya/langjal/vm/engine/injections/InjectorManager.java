package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.references.ClassReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class InjectorManager
{
    private final @NotNull List<Injector> injectors;

    public InjectorManager()
    {
        this.injectors = new LinkedList<>();
        this.initialiseDefaultInjectors();
    }

    private void initialiseDefaultInjectors()
    {
        this.injectors.addAll(Arrays.asList(
                new InjectorClass(),
                new InjectorConsole(),
                new InjectorFileDescriptor(),
                new InjectorInet4Address(),
                new InjectorInet6Address(),
                new InjectorNativeEntryPoint(),
                new InjectorObject(),
                new InjectorPerf(),
                new InjectorPrintStream(),
                new InjectorRandomAccessFile(),
                new InjectorReflection(),
                new InjectorRuntime(),
                new InjectorSystem(),
                new InjectorSystemPropsRaw(),
                new InjectorThread(),
                new InjectorUnsafe(),
                new InjectorVM()
        ));
    }

    public Injector findInjector(@NotNull ClassReference reference)
    {
        for (Injector injector : this.injectors)
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
