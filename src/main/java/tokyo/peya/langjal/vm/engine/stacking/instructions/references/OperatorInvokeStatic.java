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
import tokyo.peya.langjal.vm.references.ClassReference;

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

        VMClass caller = frame.getMethod().getClazz();
        VMClass clazz = frame.getClassLoader().findClass(ClassReference.of(owner));
        if (!clazz.isInitialised())
        {
            // クラスが初期化されていない場合は初期化をして，この命令を再実行する
            clazz.initialise(frame.getThread());
            frame.rerunInstruction();
            return;
        }

        MethodDescriptor methodDescriptor = MethodDescriptor.parse(desc);
        InvocationHelper.InvocationContext ctxt = InvocationHelper.retrieveCtxt(owner, methodDescriptor, frame);

        VMMethod method = clazz.findSuitableMethod(
                caller,
                null,
                name,
                null,
                ctxt.argumentTypes()
        );
        if (method == null)
            throw new IllegalStateException("No suitable static method found: " + owner + "." + name + desc);

        if (!method.getAccessAttributes().has(AccessAttribute.STATIC))
            throw new IllegalStateException("Method is not static: " + owner + "." + name + desc);

        method.invokeStatic(
                operand,
                frame,
                caller,
                frame.isVMDecree(),
                ctxt.arguments()
        );
    }
}
