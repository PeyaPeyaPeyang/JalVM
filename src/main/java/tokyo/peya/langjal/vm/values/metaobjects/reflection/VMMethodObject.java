package tokyo.peya.langjal.vm.values.metaobjects.reflection;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.metaobjects.VMStringObject;

import java.util.Arrays;

@Getter
public class VMMethodObject extends VMObject
{
    private final JalVM vm;
    private final VMMethod method;

    public VMMethodObject(@NotNull JalVM vm, @NotNull VMMethod method, @NotNull VMClass objectType)
    {
        super(objectType);
        this.vm = vm;
        this.method = method;

        this.setFieldIfExists("clazz", method.getClazz().getClassObject());
        this.setFieldIfExists("name", VMStringObject.createString(vm, method.getName()));
        this.setFieldIfExists("parameterTypes", new VMArray(
                vm,
                VMType.ofClassName(vm, "java/lang/Class"),
                Arrays.stream(method.getParameterTypes())
                        .map(t -> t.getLinkedClass().getClassObject())
                        .toArray(VMObject[]::new)
        ));
        this.setFieldIfExists("returnType", method.getReturnType().getLinkedClass().getClassObject());
        this.setFieldIfExists("exceptionTypes",
                              new VMArray(vm, VMType.ofClassName(vm, "java/lang/Class"))
        );
        this.setFieldIfExists("modifiers", new VMInteger(vm, method.getMethodNode().access));
        this.setFieldIfExists("signature", VMStringObject.createString(vm, method.getMethodNode().signature));
        this.setFieldIfExists("annotations", new VMArray(vm, VMType.of(vm, PrimitiveTypes.BYTE)));
        this.setFieldIfExists("parameterAnnotations", new VMArray(vm, VMType.of(vm, PrimitiveTypes.BYTE)));
        this.setFieldIfExists("annotationDefault", new VMArray(vm, VMType.of(vm, PrimitiveTypes.BYTE)));

        this.forceInitialise(vm.getClassLoader());
    }


    public VMMethodObject(@NotNull JalVM vm, @NotNull VMMethod method)
    {
        this(vm, method, vm.getClassLoader().findClass(ClassReference.of("java/lang/reflect/Method")));
    }
}
