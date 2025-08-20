package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMBoolean;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMValue;

import java.io.FileOutputStream;
import java.io.IOException;

public class InjectorFileOutputStream implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("java/io/FileOutputStream");

    private final InjectorFileDescriptor fdInjector;

    public InjectorFileOutputStream(InjectorFileDescriptor fd)
    {
        this.fdInjector = fd;
    }

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
                        EOpcodes.ACC_PUBLIC,
                        "writeBytes",
                        "([BIIZ)V",
                        null,
                        null
                )
                )
                {
                    @Override
                    @Nullable VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMArray array = (VMArray) args[0];
                        int offset = ((VMInteger) args[1]).asNumber().intValue();
                        int length = ((VMInteger) args[2]).asNumber().intValue();
                        boolean flush = ((VMBoolean) args[3]).asBoolean();

                        assert instance != null;
                        VMObject fd = (VMObject) instance.getField("fd");
                        long handleValue = ((VMLong) fd.getField("handle")).asNumber().longValue();

                        FileOutputStream outputStream =
                                InjectorFileOutputStream.this.fdInjector.getStream(handleValue);

                        try {
                            byte[] bytes = array.toByteArray();
                            outputStream.write(bytes, offset, length);
                            if (flush)
                                outputStream.flush();
                        } catch (IOException e) {
                            throw new VMPanic("Failed to write bytes to output stream", e);
                        }
                        return null;
                    }
                }
        );
    }

}
