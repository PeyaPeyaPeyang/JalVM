package tokyo.peya.langjal.vm.engine;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.values.VMType;

@Getter
public class VMPrimitiveClass extends VMClass
{

    private final VMType<?> primitiveType;

    public VMPrimitiveClass(@NotNull VMType<?> primitiveType,
                            @NotNull String primitiveName)
    {
        super(null, createClassNode(primitiveName));
        this.primitiveType = primitiveType;
    }

    @Override
    public void linkClass(@NotNull VMSystemClassLoader cl)
    {
    }

    @Override
    public String getTypeDescriptor()
    {
        return this.primitiveType.getTypeDescriptor();
    }

    @Override
    public boolean isSubclassOf(@NotNull VMClass maySuper)
    {
        return maySuper == this || maySuper == VMType.GENERIC_OBJECT.getLinkedClass()
                || this.getReference().equals(maySuper.getReference());
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
