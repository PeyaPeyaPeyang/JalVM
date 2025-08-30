package tokyo.peya.langjal.vm.engine;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMPrimitive;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.metaobjects.VMBoxedPrimitiveObject;

@Getter
public class VMPrimitiveClass extends VMClass
{
    private final VMType<?> primitiveType;
    private final ClassReference boxedReference;

    private VMClass boxedClass;

    public VMPrimitiveClass(@NotNull JalVM vm,
                            @NotNull VMType<?> primitiveType,
                            @NotNull PrimitiveTypes primitiveName)
    {
        super(vm, createClassNode(primitiveName.getName()));
        this.primitiveType = primitiveType;
        this.boxedReference = getBoxedReference(primitiveName);
        this.linkedClass = this;
    }

    private static ClassReference getBoxedReference(@NotNull PrimitiveTypes primitiveName)
    {
        return switch (primitiveName)
                {
                    case INT -> ClassReference.of("java/lang/Integer");
                    case BYTE -> ClassReference.of("java/lang/Byte");
                    case SHORT -> ClassReference.of("java/lang/Short");
                    case LONG -> ClassReference.of("java/lang/Long");
                    case FLOAT -> ClassReference.of("java/lang/Float");
                    case DOUBLE -> ClassReference.of("java/lang/Double");
                    case CHAR -> ClassReference.of("java/lang/Character");
                    case BOOLEAN -> ClassReference.of("java/lang/Boolean");
                    case VOID -> ClassReference.of("java/lang/Void");
                };
    }

    @NotNull
    public VMObject createBoxedInstance(@NotNull VMPrimitive value)
    {
        if (!value.type().equals(this.primitiveType))
            throw new VMPanic("Cannot box " + value.type() + " with " + this.primitiveType);

        return new VMBoxedPrimitiveObject(this.boxedClass, value);
    }

    @Override
    public void link(@NotNull JalVM vm)
    {
        this.boxedClass = vm.getClassLoader().findClass(this.boxedReference);
    }

    @Override
    public @NotNull String getTypeDescriptor()
    {
        return this.primitiveType.getTypeDescriptor();
    }

    @Override
    public boolean isSubclassOf(@NotNull VMClass maySuper)
    {
        return maySuper == this || maySuper == VMType.ofGenericObject(maySuper.getVm()).getLinkedClass()
                || this.getReference().equals(maySuper.getReference());
    }

    @Override
    public boolean isPrimitive()
    {
        return true;
    }

    @Override
    public VMClass getLinkedClass()
    {
        return this;
    }

    private static ClassNode createClassNode(@NotNull String primitiveName)
    {
        ClassNode classNode = new ClassNode();
        classNode.version = 52; // Java 8
        classNode.access = 0x20; // ACC_PUBLIC
        classNode.name = primitiveName;
        return classNode;
    }
}
