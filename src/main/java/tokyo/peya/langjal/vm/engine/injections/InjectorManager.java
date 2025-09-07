package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.references.ClassReference;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class InjectorManager
{
    private final InjectorFileDescriptor fdInjector;
    private final @NotNull List<Injector> injectors;

    public InjectorManager()
    {
        this.injectors = new LinkedList<>();
        this.fdInjector = new InjectorFileDescriptor();
        this.initialiseDefaultInjectors();
    }

    private void initialiseDefaultInjectors()
    {
        this.injectors.addAll(Arrays.asList(
                new InjectorArray(),
                new InjectorCDS(),
                new InjectorClass(),
                new InjectorClassLoader(),
                new InjectorConsole(),
                new InjectorDouble(),
                this.fdInjector,
                new InjectorFileDescriptor(),
                new InjectorFileInputStream(),
                new InjectorFileOutputStream(this.fdInjector),
                new InjectorFinalizer(),
                new InjectorFloat(),
                new InjectorInet4Address(),
                new InjectorInet6Address(),
                new InjectorMethodHandle(),
                new InjectorMethodHandleNatives(),
                new InjectorNativeAccessor(),
                new InjectorNativeEntryPoint(),
                new InjectorObject(),
                new InjectorPerf(),
                new InjectorRandomAccessFile(),
                new InjectorReference(),
                new InjectorReflection(),
                new InjectorRuntime(),
                new InjectorScopedMemoryAccess(),
                new InjectorSignal(),
                 new InjectorStackTraceElement(),
                new InjectorString(),
                new InjectorSystem(),
                new InjectorSystemPropsRaw(),
                new InjectorThread(),
                new InjectorThreadFieldHolder(),
                new InjectorThreadGroup(),
                new InjectorThrowable(),
                new InjectorUnsafe(),
                new InjectorVM(),
                new InjectorWin32ErrorMode()
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
