package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.AccessAttribute;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMBoolean;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMNull;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMPrimitive;
import tokyo.peya.langjal.vm.values.VMReferenceValue;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMClassObject;
import tokyo.peya.langjal.vm.values.metaobjects.VMStringObject;

import java.nio.ByteOrder;

public class InjectorUnsafeConstants implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("jdk/internal/misc/UnsafeConstants");

    @Override
    public @NotNull ClassReference suitableClass()
    {
        return CLAZZ;
    }

    @Override
    public void inject(@NotNull VMSystemClassLoader cl, @NotNull VMClass clazz)
    {
        clazz.injectField(
                new InjectedField(
                        clazz,
                        VMType.of(clazz, PrimitiveTypes.INT),
                        new FieldNode(
                                EOpcodes.ACC_STATIC | EOpcodes.ACC_FINAL,
                                "ADDRESS_SIZE0",
                                "I",
                                null,
                                null
                        )
                ) {
                    @Override
                    public VMValue get(@NotNull VMClass caller, @Nullable VMObject instance)
                    {
                        boolean is64Bit = System.getProperty("os.arch").contains("64");
                        return new VMInteger(caller, is64Bit ? 8 : 4);
                    }

                    @Override
                    public void set(@NotNull VMClass caller, @Nullable VMObject instance, @NotNull VMValue value)
                    {
                    }
                }
        );
        clazz.injectField(
                new InjectedField(
                        clazz,
                        VMType.of(clazz, PrimitiveTypes.INT),
                        new FieldNode(
                                EOpcodes.ACC_STATIC | EOpcodes.ACC_FINAL,
                                "PAGE_SIZE",
                                "I",
                                null,
                                null
                        )
                ) {
                    @Override
                    public VMValue get(@NotNull VMClass caller, @Nullable VMObject instance)
                    {
                        return new VMInteger(caller, 4096);
                    }

                    @Override
                    public void set(@NotNull VMClass caller, @Nullable VMObject instance, @NotNull VMValue value)
                    {
                    }
                }
        );
        clazz.injectField(
                new InjectedField(
                        clazz,
                        VMType.of(clazz, PrimitiveTypes.BOOLEAN),
                        new FieldNode(
                                EOpcodes.ACC_STATIC | EOpcodes.ACC_FINAL,
                                "BIG_ENDIAN",
                                "Z",
                                null,
                                null
                        )
                ) {
                    @Override
                    public VMValue get(@NotNull VMClass caller, @Nullable VMObject instance)
                    {
                        return VMBoolean.ofFalse(caller);
                    }

                    @Override
                    public void set(@NotNull VMClass caller, @Nullable VMObject instance, @NotNull VMValue value)
                    {
                    }
                }
        );
        clazz.injectField(
                new InjectedField(
                        clazz,
                        VMType.of(clazz, PrimitiveTypes.BOOLEAN),
                        new FieldNode(
                                EOpcodes.ACC_STATIC | EOpcodes.ACC_FINAL,
                                "UNALIGNED_ACCESS",
                                "Z",
                                null,
                                null
                        )
                ) {
                    @Override
                    public VMValue get(@NotNull VMClass caller, @Nullable VMObject instance)
                    {
                        return VMBoolean.ofTrue(caller);
                    }

                    @Override
                    public void set(@NotNull VMClass caller, @Nullable VMObject instance, @NotNull VMValue value)
                    {
                    }
                }
        );
        clazz.injectField(
                new InjectedField(
                        clazz,
                        VMType.of(clazz, PrimitiveTypes.INT),
                        new FieldNode(
                                EOpcodes.ACC_STATIC | EOpcodes.ACC_FINAL,
                                "DATA_CACHE_LINE_FLUSH_SIZE",
                                "I",
                                null,
                                null
                        )
                ) {
                    @Override
                    public VMValue get(@NotNull VMClass caller, @Nullable VMObject instance)
                    {
                        return new VMInteger(caller, 0);
                    }

                    @Override
                    public void set(@NotNull VMClass caller, @Nullable VMObject instance, @NotNull VMValue value)
                    {
                    }
                }
        );


    }
}
