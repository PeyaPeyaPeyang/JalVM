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
import tokyo.peya.langjal.vm.panics.LinkagePanic;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.panics.invocation.IllegalInvocationTypePanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMReferenceValue;

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

        VMClass caller = frame.getMethod().getClazz();
        VMClass clazz = frame.getClassLoader().findClass(ClassReference.of(owner));

        MethodDescriptor methodDescriptor = MethodDescriptor.parse(desc);
        InvocationHelper.InvocationContext ctxt = InvocationHelper.retrieveCtxt(owner, methodDescriptor, frame);
        VMReferenceValue referenceValue = frame.getStack().popType(clazz);
        if (!(referenceValue instanceof VMObject instance))
            throw new VMPanic("Expected an object to access instance '" + name + "', but got " + referenceValue.getClass().getSimpleName());

        if (name.equals("<init>"))
        {
            instance.initialiseInstance(
                    frame,
                    caller,
                    clazz,
                    ctxt.argumentTypes(),
                    ctxt.arguments(),
                    frame.isVMDecree()
            );
            return; // コンストラクタはここで終わり
        }

        VMMethod method = clazz.findSuitableMethod(
                caller,
                clazz,
                name,
                null,
                ctxt.argumentTypes()
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
                frame,
                caller,
                instance,
                frame.isVMDecree(),
                ctxt.arguments()
        );
    }
}
