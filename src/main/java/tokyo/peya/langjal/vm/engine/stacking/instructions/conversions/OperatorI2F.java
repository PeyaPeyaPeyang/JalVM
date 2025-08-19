package tokyo.peya.langjal.vm.engine.stacking.instructions.conversions;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMFloat;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorI2F extends AbstractInstructionOperator<InsnNode>
{

    public OperatorI2F()
    {
        super(EOpcodes.I2F, "i2f");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        VMInteger value = frame.getStack().popType(VMType.INTEGER);
        VMFloat result = new VMFloat(value.asNumber().floatValue());
        frame.getTracer().pushHistory(
                ValueTracingEntry.manipulation(
                        result,
                        value,
                        frame.getMethod(),
                        operand
                )
        );
        frame.getStack().push(result);
    }
}
