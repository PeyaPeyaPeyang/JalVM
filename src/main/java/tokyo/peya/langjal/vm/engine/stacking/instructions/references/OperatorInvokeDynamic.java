package tokyo.peya.langjal.vm.engine.stacking.instructions.references;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.MethodDescriptor;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMObject;

public class OperatorInvokeDynamic extends AbstractInstructionOperator<InvokeDynamicInsnNode>
{
    private final DynamicInvocationHelper helper;


    public OperatorInvokeDynamic()
    {
        super(EOpcodes.INVOKEDYNAMIC, "invokedynamic");

        this.helper = new DynamicInvocationHelper();
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InvokeDynamicInsnNode operand)
    {
        String name = operand.name;
        MethodDescriptor descriptor = MethodDescriptor.parse(operand.desc);
        VMObject callSite = this.helper.resolveCallSite(frame, name, descriptor, operand);
        if (callSite == null)
        {
            // まだ解決されていない場合は，この命令を再実行する
            frame.rerunInstruction();
            return;
        }
    }

}
