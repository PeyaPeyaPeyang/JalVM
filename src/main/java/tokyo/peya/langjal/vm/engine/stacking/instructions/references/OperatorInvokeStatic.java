package tokyo.peya.langjal.vm.engine.stacking.instructions.references;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.MethodInsnNode;
import tokyo.peya.langjal.compiler.jvm.AccessAttribute;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.MethodDescriptor;
import tokyo.peya.langjal.compiler.jvm.TypeDescriptor;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

public class OperatorInvokeStatic extends AbstractInstructionOperator<MethodInsnNode>
{

    public OperatorInvokeStatic()
    {
        super(EOpcodes.INVOKESTATIC, "invokestatic");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull MethodInsnNode operand)
    {
        String owner = operand.owner;
        String name = operand.name;
        String desc = operand.desc;

        MethodDescriptor methodDescriptor = MethodDescriptor.parse(desc);

        VMClass caller = frame.getMethod().getClazz();
        VMClass clazz = frame.getVm().getClassLoader().findClass(ClassReference.of(owner));

        TypeDescriptor[] parameterTypes = methodDescriptor.getParameterTypes();
        VMValue[] arguments = new VMValue[parameterTypes.length];
        VMType[] vmTypes = new VMType[parameterTypes.length];
        for (int i = 0; i < arguments.length; i++)
        {
            VMValue arg = arguments[i] = frame.getStack().pop();
            vmTypes[i] = arg.type();
        }

        VMMethod method = clazz.findSuitableMethod(
                caller,
                name,
                null,
                vmTypes
        );
        if (method == null)
            throw new IllegalStateException("No suitable static method found: " + owner + "." + name + desc);

        if (!method.getAccessAttributes().has(AccessAttribute.STATIC))
            throw new IllegalStateException("Method is not static: " + owner + "." + name + desc);

        if (method.getAccessAttributes().has(AccessAttribute.NATIVE))
        {
            VMType returningType = new VMType(method.getDescriptor().getReturnType());

            frame.getVm().getNativeCaller().callFFI(
                    frame.getMethod().getClazz().getReference(),
                    method.getName(),
                    returningType,
                    arguments
            );
            return;
        }

        method.invokeStatic(
                frame.getThread(),
                caller,
                false,
                arguments
        );
    }
}
