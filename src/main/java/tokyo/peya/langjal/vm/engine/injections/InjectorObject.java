package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.threading.VMMonitor;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMValue;

import java.time.Duration;

public class InjectorObject implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("java/lang/Object");

    @Override
    public ClassReference suitableClass()
    {
        return CLAZZ;
    }

    @Override
    public void inject(@NotNull VMSystemClassLoader cl, @NotNull VMClass clazz)
    {
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                        "getClass",
                        "()Ljava/lang/Class;",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        assert instance != null : "Instance must not be null";
                        return instance.getObjectType().getClassObject();
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                        "hashCode",
                        "()I",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        assert instance != null : "Instance must not be null";
                        return new VMInteger(instance.insideHashCode());
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_FINAL | EOpcodes.ACC_NATIVE,
                        "wait0",
                        "(J)V",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        assert instance != null : "Instance must not be null";
                        VMLong timeout = (VMLong) args[0];
                        long timeoutMillis = timeout.asNumber().longValue();
                        VMMonitor monitor = instance.getMonitor();
                        if (!monitor.isOwner(thread))
                            throw new VMPanic(
                                    "Thread " + thread.getName() + " is not the owner of the monitor for " +
                                            instance.getObjectType().getReference().getFullQualifiedName()
                            );

                        Duration duration = Duration.ofMillis(timeoutMillis);
                        if (duration.isNegative() || duration.isZero())
                            monitor.waitFor(thread);  // 無限に待機する
                        else
                            monitor.waitForTimed(thread, duration); // タイムアウト付きで待機する

                        return null;
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_FINAL | EOpcodes.ACC_NATIVE,
                        "notify",
                        "()V",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        assert instance != null : "Instance must not be null";
                        VMMonitor monitor = instance.getMonitor();
                        if (!monitor.isOwner(thread))
                            throw new VMPanic(
                                    "Thread " + thread.getName() + " is not the owner of the monitor for " +
                                            instance.getObjectType().getReference().getFullQualifiedName()
                            );

                        monitor.notifyOne();

                        return null;
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_FINAL | EOpcodes.ACC_NATIVE,
                        "notifyAll",
                        "()V",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        assert instance != null : "Instance must not be null";
                        VMMonitor monitor = instance.getMonitor();
                        if (!monitor.isOwner(thread))
                            throw new VMPanic(
                                    "Thread " + thread.getName() + " is not the owner of the monitor for " +
                                            instance.getObjectType().getReference().getFullQualifiedName()
                            );

                        monitor.notifyForAll();

                        return null;
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_FINAL | EOpcodes.ACC_NATIVE,
                        "clone",
                        "()Ljava/lang/Object;",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        assert instance != null : "Instance must not be null";
                        return instance.cloneValue();
                    }
                }
        );
    }

}
