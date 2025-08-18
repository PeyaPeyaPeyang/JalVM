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
import tokyo.peya.langjal.vm.exceptions.LinkagePanic;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.exceptions.invocation.IllegalInvocationTypePanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMReferenceValue;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

public class OperatorInvokeSpecial extends AbstractInstructionOperator<MethodInsnNode>
{

    public OperatorInvokeSpecial()
    {
        super(EOpcodes.INVOKESPECIAL, "invokespecial");
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
        VMType<?>[] vmTypes = new VMType[parameterTypes.length];
        for (int i = 0; i < arguments.length; i++)
        {
            VMValue arg = arguments[i] = frame.getStack().pop();
            vmTypes[i] = arg.type();
        }

        VMReferenceValue referenceValue = frame.getStack().popType(clazz);
        if (!(referenceValue instanceof VMObject instance))
            throw new VMPanic("Expected an object to access instance '" + name + "', but got " + referenceValue.getClass().getSimpleName());

        VMMethod method = clazz.findSuitableMethod(
                caller,
                name,
                null,
                vmTypes
        );
        if (method == null)
            throw new LinkagePanic("No suitable static method found: " + owner + "." + name + desc);

        if (method.getAccessAttributes().has(AccessAttribute.STATIC))
            throw new IllegalInvocationTypePanic(
                    frame.getThread(), method,
                    "Cannot invoke static method " + owner + "." + name + desc + " as a special method"
            );

        method.invokeInstanceMethod(
                operand,
                frame.getThread(),
                caller,
                instance,
                false,
                arguments
        );
    }
}
