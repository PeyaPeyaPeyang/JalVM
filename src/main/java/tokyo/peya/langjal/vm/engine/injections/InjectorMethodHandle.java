package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMClassObject;

public class InjectorMethodHandle implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("java/lang/invoke/MethodHandle");

    @Override
    public @NotNull ClassReference suitableClass()
    {
        return CLAZZ;
    }

    @Override
    public void inject(@NotNull VMSystemClassLoader cl, @NotNull VMClass clazz)
    {
        VMClass fieldAccessor = cl.findClass(ClassReference.of("java/lang/invoke/DirectMethodHandle$Accessor"));
        VMClass fieldAccessorStatic = cl.findClass(ClassReference.of("java/lang/invoke/DirectMethodHandle$StaticAccessor"));

        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_FINAL | EOpcodes.ACC_NATIVE,
                        "invokeExact",
                        "([Ljava/lang/Object;)Ljava/lang/Object;",
                        null,
                        null
                )
                )
                {
                    // @SignaturePolymorphic らしいので，戻り値と引数の型は問わない
                    @Override
                    protected boolean checkReturnTypeSuitability(@Nullable VMType<?> returnType)
                    {
                        return true;
                    }

                    @Override
                    protected boolean checkArgumentsSuitability(@NotNull VMType<?>... args)
                    {
                        return true;
                    }

                    @Override
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        if (fieldAccessor.isInstance(instance))
                            return null; // TODO: implement
                        else if (fieldAccessorStatic.isInstance(instance))
                        {
                            assert instance != null;
                            VMClass fieldType = ((VMClassObject) instance.getField("fieldType")).getRepresentingClass();
                            long staticOffset = ((VMLong) instance.getField("staticOffset")).asNumber().longValue();
                            VMField field = cl.getVM().getHeap().getStaticFieldByID(staticOffset);
                            if (field == null)
                                throw new NullPointerException("Static field not found by ID: " + staticOffset + " (field type: " + fieldType.getReference().toString() + ")");

                            return field.getClazz().getStaticFieldValue(field);
                        }

                        throw new UnsupportedOperationException("Unsupported MethodHandle type: " + (instance == null ? "null" : instance.getClass().getName()));
                    }
                }
        );
    }
}
