package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMStringObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class InjectorWinNTFileSystem implements Injector
{

    public static final ClassReference CLAZZ = ClassReference.of("java/io/WinNTFileSystem");

    private static final int BA_EXISTS = 0x01;
    private static final int BA_DIRECTORY = 0x02;
    private static final int BA_WRITEABLE = 0x04;
    private static final int BA_HIDDEN = 0x08;

    @Override
    public @NotNull ClassReference suitableClass()
    {
        return CLAZZ;
    }

    @Override
    public void inject(@NotNull VMSystemClassLoader cl, @NotNull VMClass clazz)
    {
        clazz.injectMethod(
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
                    @Override
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        return null;  // VOID
                    }
                }
        );
        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_NATIVE,
                        "canonicalize0",
                        "(Ljava/lang/String;)Ljava/lang/String;",
                        null,
                        null
                )
                )
                {
                    @Override
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        String path = ((VMStringObject) args[0]).getString();
                        Path p = Path.of(path).toAbsolutePath().normalize();
                        return VMStringObject.createString(frame, p.toString());
                    }
                }
        );
        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_NATIVE,
                        "getFinalPath0",
                        "(Ljava/lang/String;)Ljava/lang/String;",
                        null,
                        null
                )
                )
                {
                    @Override
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        String path = ((VMStringObject) args[0]).getString();
                        Path p = Path.of(path).toAbsolutePath().normalize();
                        try
                        {
                            p = p.toRealPath();
                        }
                        catch (IOException e)
                        {
                            throw new VMPanic(e);
                        }
                        return VMStringObject.createString(frame, p.toString());
                    }
                }
        );
        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_NATIVE,
                        "getBooleanAttributes0",
                        "(Ljava/lang/File;)I",
                        null,
                        null
                )
                )
                {
                    @Override
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMObject file = (VMObject) args[0];
                        String path = ((VMStringObject) file.getField("path")).getString();
                        File f = new File(path);
                        int attr = 0;
                        if (f.exists())
                            attr |= BA_EXISTS;
                        if (f.isDirectory())
                            attr |= BA_DIRECTORY;
                        if (f.canWrite())
                            attr |= BA_WRITEABLE;
                        if (f.isHidden())
                            attr |= BA_HIDDEN;
                        return new VMInteger(frame, attr);
                    }
                }
        );
    }

}
