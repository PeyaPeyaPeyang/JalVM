package tokyo.peya.langjal.vm.engine.stacking.instructions.stack;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.VMStack;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.exceptions.IllegalOperationPanic;
import tokyo.peya.langjal.vm.values.VMValue;

public class OperatorSwap extends AbstractInstructionOperator<InsnNode>
{
    public OperatorSwap()
    {
        super(EOpcodes.SWAP, "swap");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        VMStack stack = frame.getStack();

        VMValue value = stack.pop();
        VMValue value2 = stack.pop();
        if (value.isCategory2() || value2.isCategory2())
            throw new IllegalOperationPanic("Cannot swap category 2 values with SWAP");

        stack.push(value);
        stack.push(value2);
    }
}
