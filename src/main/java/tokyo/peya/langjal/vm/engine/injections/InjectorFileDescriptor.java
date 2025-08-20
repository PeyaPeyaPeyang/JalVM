package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMBoolean;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMValue;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.SyncFailedException;
import java.util.HashMap;
import java.util.Map;

public class InjectorFileDescriptor implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("java/io/FileDescriptor");

    private final Map<Long, FileDescriptor> handles;
    private final Map<Integer, Boolean> appends;
    private final Map<Integer, Long> fdToHandleNums;

    @Override
    public ClassReference suitableClass()
    {
        return CLAZZ;
    }

    public InjectorFileDescriptor()
    {
        this.handles = new HashMap<>();

        this.handles.put(0L, FileDescriptor.in);
        this.handles.put(1L, FileDescriptor.out);
        this.handles.put(2L, FileDescriptor.err);

        this.appends = new HashMap<>();
        this.appends.put(0, false);
        this.appends.put(1, false);
        this.appends.put(2, false);

        this.fdToHandleNums = new HashMap<>();
        this.fdToHandleNums.put(0, 0L);
        this.fdToHandleNums.put(1, 1L);
        this.fdToHandleNums.put(2, 2L);
    }

    @Override
    public void inject(@NotNull VMSystemClassLoader cl, @NotNull VMClass clazz)
    {
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "initIDs",
                        "()V",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        return null;
                    }
                }
        );

        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "getHandle",
                        "(I)J",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMInteger fdValue = (VMInteger) args[0];
                        int fd = fdValue.asNumber().intValue();
                        return new VMLong(InjectorFileDescriptor.this.fdToHandleNums.get(fd));
                    }
                }
        );

        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "getAppend",
                        "(I)Z",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMInteger fdValue = (VMInteger) args[0];
                        int fd = fdValue.asNumber().intValue();
                        return VMBoolean.of(InjectorFileDescriptor.this.appends.get(fd));
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_NATIVE,
                        "close0",
                        "()V",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        assert instance != null;
                        VMValue handleField = instance.getField("handle");
                        VMValue fdField = instance.getField("fd");

                        long handleNum = ((VMLong) handleField).asNumber().longValue();
                        int fd = ((VMInteger) fdField).asNumber().intValue();

                        InjectorFileDescriptor.this.handles.remove(handleNum);
                        InjectorFileDescriptor.this.fdToHandleNums.remove(fd);
                        InjectorFileDescriptor.this.appends.remove(fd);

                        return null;
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_NATIVE,
                        "sync0",
                        "()V",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        assert instance != null;
                        VMValue handleField = instance.getField("handle");
                        long handleNum = ((VMLong) handleField).asNumber().longValue();
                        FileDescriptor fd = InjectorFileDescriptor.this.handles.get(handleNum);
                        try
                        {
                            fd.sync();
                        }
                        catch (SyncFailedException e)
                        {
                            throw new VMPanic("Failed to sync file descriptor", e);
                        }
                        return null;
                    }
                }
        );
    }

}
