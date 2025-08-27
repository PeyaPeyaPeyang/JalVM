package tokyo.peya.langjal.vm.values.metaobjects.reflection;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
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
    private final VMSystemClassLoader classLoader;
    private final VMMethod method;

    public VMMethodObject(@NotNull VMSystemClassLoader classLoader, @NotNull VMMethod method, @NotNull VMClass objectType)
    {
        super(objectType);
        this.classLoader = classLoader;
        this.method = method;

        this.setFieldIfExists("clazz", method.getClazz().getClassObject());
        this.setFieldIfExists("name", VMStringObject.createString(classLoader, method.getName()));
        this.setFieldIfExists("parameterTypes", new VMArray(
                classLoader,
                VMType.ofClassName("java/lang/Class"),
                Arrays.stream(method.getParameterTypes())
                        .map(t -> t.getLinkedClass().getClassObject())
                        .toArray(VMObject[]::new)
        ));
        this.setFieldIfExists("returnType", method.getReturnType().getLinkedClass().getClassObject());
        this.setFieldIfExists("exceptionTypes", new VMArray(classLoader, VMType.ofClassName("java/lang/Class")));
        this.setFieldIfExists("modifiers", new VMInteger(method.getMethodNode().access));
        this.setFieldIfExists("signature", VMStringObject.createString(classLoader, method.getMethodNode().signature));
        this.setFieldIfExists("annotations", new VMArray(classLoader, VMType.BYTE));
        this.setFieldIfExists("parameterAnnotations", new VMArray(classLoader, VMType.BYTE));
        this.setFieldIfExists("annotationDefault", new VMArray(classLoader, VMType.BYTE));

        this.forceInitialise();
    }


    public VMMethodObject(@NotNull VMSystemClassLoader classLoader, @NotNull VMMethod method)
    {
        this(classLoader, method, classLoader.findClass(ClassReference.of("java/lang/reflect/Method")));
    }
}
