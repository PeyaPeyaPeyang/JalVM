 package tokyo.peya.langjal.vm.engine.members;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.injections.InjectedMethod;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.values.VMAnnotation;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

public class VMAnnotationElementVirtualMethod extends InjectedMethod
{
    public VMAnnotationElementVirtualMethod(@NotNull VMClass clazz,
                                            @NotNull String name,
                                            @NotNull VMType<?> returnType)
    {
        super(clazz, new MethodNode(
                EOpcodes.ACC_PUBLIC,
                name,
                "()" + returnType.getTypeDescriptor(),
                null,
                null
        ));
    }

    @Override
    protected @Nullable VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller, @Nullable VMObject instance,
                                       @NotNull VMValue[] args)
    {
        if (!(instance instanceof VMAnnotation annotation))
            throw new VMPanic("Expected an annotation instance, but got " + (instance == null ? "null" : instance.getClass().getSimpleName()));

        return annotation.getValue(this.getName());
    }
}
