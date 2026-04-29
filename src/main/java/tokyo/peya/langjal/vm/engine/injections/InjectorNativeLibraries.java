package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.ffi.NativeCaller;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMBoolean;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMNull;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMStringObject;
import tokyo.peya.langjal.vm.values.metaobjects.reflection.VMConstructorObject;

import java.nio.file.Files;
import java.nio.file.Path;

public class InjectorNativeLibraries implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("jdk/internal/loader/NativeLibraries");

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
                        "findBuiltinLib",
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
                        String libName = ((VMStringObject) args[0]).getString();
                        // VM を動かしているパスを取得する
                        Path javaHome = Path.of(System.getProperty("java.home"));
                        Path javaBin = javaHome.resolve("bin");

                        Path libPath = javaBin.resolve(libName);
                        if (Files.exists(libPath))
                            return VMStringObject.createString(frame, libPath.toString());
                        else
                            return new VMNull<>(VMType.ofClassName(frame, "java/lang/String"));
                    }
                }
        );
        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "load",
                        "(Ljdk/internal/loader/NativeLibraries$NativeLibraryImpl;Ljava/lang/String;ZZ)Z",
                        null,
                        null
                )
                )
                {
                    @Override
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        assert caller != null;

                        VMObject nativeLibImpl = (VMObject) args[0];
                        String libPath = ((VMStringObject) args[1]).getString();
                        boolean isBuiltin = ((VMBoolean) args[2]).asBoolean();
                        boolean throwExceptionIfFail = ((VMBoolean) args[3]).asBoolean();

                        try
                        {
                            NativeCaller.NativeLibrary library = frame.getVM().getNativeCaller().registerLibrary(
                                    caller.getReference(),
                                    libPath
                            );
                            nativeLibImpl.setField(
                                    "handle",
                                    new VMLong(frame, library.handle())
                            );
                            // jniVersion は使われないので 0 で良い

                            return VMBoolean.ofTrue(frame);
                        }
                        catch (Throwable t)
                        {
                            if (throwExceptionIfFail)
                                throw new VMPanic("Failed to load native library: " + libPath, t);
                            return VMBoolean.ofFalse(frame);
                        }
                    }
                }
        );
    }
}
