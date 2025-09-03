package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMValue;

public class InjectorCDS implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("jdk/internal/misc/CDS");

    @Override
    public ClassReference suitableClass()
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
                        "getCDSConfigStatus",
                        "()I",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        return new VMInteger(frame, 0); // 0 = CDS無効
                    }
                }
        );

        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "initializeFromArchive",
                        "(Ljava/lang/Class;)V",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        return null; // なにもしない
                    }
                }
        );

        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "getRandomSeedForDumping",
                        "()J",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        String release   = System.getProperty("java.runtime.version");
                        String dbgLevel  = "release"; // 通常は "release" or "fastdebug" とか
                        String version   = System.getProperty("java.vm.version");
                        int major        = Runtime.version().feature();
                        int minor        = Runtime.version().interim();
                        int security     = Runtime.version().update();
                        int patch        = Runtime.version().patch();
                        return new VMLong(
                                frame,
                                getRandomSeedForDumping(release, dbgLevel, version, major, minor, security, patch)
                        );
                    }
                }
        );
    }
    
    private static long getRandomSeedForDumping(
            String release,
            String dbgLevel,
            String version,
            int major,
            int minor,
            int security,
            int patch)
    {
        long seed = release.hashCode() ^ dbgLevel.hashCode() ^ version.hashCode();

        seed += major;
        seed += minor;
        seed += security;
        seed += patch;

        if (seed == 0)
            seed = 0x87654321L;

        return seed;
    }
}
