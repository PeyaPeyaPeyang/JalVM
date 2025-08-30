package tokyo.peya.langjal.vm.engine.stacking.instructions.loads;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

public class OperatorXALoadSupporter
{
    public static void execute(@NotNull VMFrame frame, @NotNull InsnNode operand, @NotNull VMType<?> type)
    {
        VMInteger index = frame.getStack().popType(VMType.of(frame, PrimitiveTypes.INT));
        VMArray array = frame.getStack().popType(VMType.ofGenericArray(frame));

        VMValue value = array.get(index.asNumber().intValue());
        VMValue conformedValue = value.conformValue(type);

        frame.getStack().push(conformedValue);
    }
}
