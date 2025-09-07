package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.MethodDescriptor;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMValue;

public class InjectorVM implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("jdk/internal/misc/VM");

    @Override
    public @NotNull ClassReference suitableClass()
    {
        return CLAZZ;
    }

    @Override
    public void inject(@NotNull VMSystemClassLoader cl, @NotNull VMClass clazz)
    {
        VMMethod originalInitLevel = clazz.findMethod("initLevel", MethodDescriptor.parse("(I)V"));
        assert originalInitLevel != null : "VM.initLevel method not found!";

        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_STATIC,
                        "initLevel",
                        "(I)V",
                        null,
                        null
                )
                )
                {
                    @Override
                    @Nullable
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMInteger level = (VMInteger) args[0];
                        int initLevel = level.asNumber().intValue();
                        String levelName = switch (initLevel)
                        {
                            case 0 -> "DOWN";
                            case 1 -> "JAVA_LANG_SYSTEM_INITIALIZED";
                            case 2 -> "MODULE_SYSTEM_INITIALIZED";
                            case 3 -> "SYSTEM_LOADER_INITIALIZING";
                            case 4 -> "SYSTEM_BOOTED";
                            case 5 -> "SYSTEM_SHUTDOWN";
                            default -> "UNKNOWN_LEVEL(" + initLevel + ")";
                        };

                        System.out.println("VM INITIALISATION LEVEL CHANGED: " + levelName);

                        frame.getThread().invokeInterrupting(
                                originalInitLevel,
                                _ -> {},
                                args
                        );

                        return null;
                    }
                }
        );
        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "initialize",
                        "()V",
                        null,
                        null
                )
                )
                {
                    @Override
                    @Nullable
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        return null;
                    }
                }
        );
    }

}
