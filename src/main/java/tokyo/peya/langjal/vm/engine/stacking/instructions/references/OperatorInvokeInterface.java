package tokyo.peya.langjal.vm.engine.stacking.instructions.references;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.MethodInsnNode;
import tokyo.peya.langjal.compiler.jvm.AccessAttribute;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.MethodDescriptor;
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

public class OperatorInvokeInterface extends AbstractInstructionOperator<MethodInsnNode>
{

    public OperatorInvokeInterface()
    {
        super(EOpcodes.INVOKEINTERFACE, "invokeinterface");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull MethodInsnNode operand)
    {
        String owner = operand.owner;
        String name = operand.name;
        String desc = operand.desc;

        VMClass caller = frame.getMethod().getClazz();
        VMClass clazz = frame.getVm().getClassLoader().findClass(ClassReference.of(owner));

        MethodDescriptor methodDescriptor = MethodDescriptor.parse(desc);
        InvocationHelper.InvocationContext ctxt = InvocationHelper.retrieveCtxt(owner, methodDescriptor, frame);
        VMReferenceValue referenceValue = frame.getStack().popType(clazz);
        if (!(referenceValue instanceof VMObject instance))
            throw new VMPanic("Expected an object to access instance '" + name + "', but got " + referenceValue.getClass().getSimpleName());

        if (!instance.isInitialised())
            throw new LinkagePanic("Cannot invoke method on uninitialised instance: " + owner + "." + name + desc);

        VMMethod method = instance.getObjectType().findSuitableMethod(
                caller,
                null,
                name,
                null,
                ctxt.argumentTypes()
        );
        if (method == null)
            throw new LinkagePanic("No suitable static method found: " + owner + "->" + name + desc);
        else if (method.getAccessAttributes().has(AccessAttribute.ABSTRACT))
            throw new LinkagePanic("Cannot invoke abstract method: " + method);

        if (method.getAccessAttributes().has(AccessAttribute.STATIC))
            throw new IllegalInvocationTypePanic(
                    frame.getThread(), method,
                    "Cannot invoke static method " + owner + "." + name + desc + " as a virtual method"
            );

        method.invokeInstanceMethod(
                operand,
                frame.getThread(),
                caller,
                instance,
                frame.isVMDecree(),
                ctxt.arguments()
        );
    }
}
