package tokyo.peya.langjal.vm.values.metaobjects.reflection.invoke;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.MethodDescriptor;
import tokyo.peya.langjal.compiler.jvm.TypeDescriptor;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;

import java.util.Arrays;

@Getter
public class VMMethodTypeObject extends VMObject
{
    private final VMType<?> returnType;
    private final VMType<?>[] parameterTypes;

    public VMMethodTypeObject(@NotNull JalVM vm, @NotNull VMType<?> returnType, @NotNull VMType<?>... parameterTypes)
    {
        super(vm.getClassLoader().findClass(ClassReference.of("java/lang/invoke/MethodType")));
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;

        this.setField("rtype", returnType.getLinkedClass().getClassObject());
        this.setField("ptypes", new VMArray(
                vm,
                vm.getClassLoader().findClass(ClassReference.of("java/lang/Class")),
                Arrays.stream(parameterTypes)
                      .map(VMType::getLinkedClass)
                      .map(VMClass::getClassObject)
                      .toArray(VMObject[]::new)
        ));

        this.forceInitialise(vm.getClassLoader());
    }

    public static VMMethodTypeObject of(@NotNull JalVM vm, @NotNull MethodDescriptor desc)
    {
        TypeDescriptor returnDesc = desc.getReturnType();
        TypeDescriptor[] paramDescs = desc.getParameterTypes();

        VMType<?> returnType = VMType.of(vm, returnDesc);
        VMType<?>[] paramTypes = Arrays.stream(paramDescs)
                                       .map(m -> VMType.of(vm, m))
                                       .toArray(VMType[]::new);

        return new VMMethodTypeObject(vm, returnType, paramTypes);
    }

    @Override
    public @NotNull VMObject cloneValue()
    {
        return super.cloneValue();
    }
}
