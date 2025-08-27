package tokyo.peya.langjal.vm.values.metaobjects.reflection.invoke;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.MethodDescriptor;
import tokyo.peya.langjal.compiler.jvm.TypeDescriptor;
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

    public VMMethodTypeObject(@NotNull VMSystemClassLoader cl, @NotNull VMType<?> returnType, @NotNull VMType<?>... parameterTypes)
    {
        super(cl.findClass(ClassReference.of("java/lang/invoke/MethodType")));
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;

        this.setField("rtype", returnType.getLinkedClass().getClassObject());
        this.setField("ptypes", new VMArray(
                cl,
                cl.findClass(ClassReference.of("java/lang/Class")),
                Arrays.stream(parameterTypes)
                      .map(VMType::getLinkedClass)
                      .map(VMClass::getClassObject)
                      .toArray(VMObject[]::new)
        ));

        this.forceInitialise(cl);
    }

    public static VMMethodTypeObject of(@NotNull VMSystemClassLoader cl, @NotNull MethodDescriptor desc)
    {
        TypeDescriptor returnDesc = desc.getReturnType();
        TypeDescriptor[] paramDescs = desc.getParameterTypes();

        VMType<?> returnType = VMType.of(returnDesc).linkClass(cl);
        VMType<?>[] paramTypes = Arrays.stream(paramDescs)
                                       .map(VMType::of)
                                       .map(t -> t.linkClass(cl))
                                       .toArray(VMType[]::new);

        return new VMMethodTypeObject(cl, returnType, paramTypes);
    }
}
