package tokyo.peya.langjal.vm.engine.stacking.instructions.stores;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.panics.IllegalOperationPanic;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

public class OperatorXAStoreSupporter
{
    public static <T extends VMValue> void execute(@NotNull VMFrame frame, @NotNull InsnNode operand,
                                                   @NotNull VMType<T> type)
    {
        T value = frame.getStack().popType(type);
        VMInteger index = frame.getStack().popType(VMType.of(frame, PrimitiveTypes.INT));
        VMArray array = frame.getStack().popType(VMType.ofGenericArray(frame));

        int idx = index.asNumber().intValue();
        if (idx < 0 || idx >= array.length())
            throw new IllegalOperationPanic("Array index out of bounds: " + idx);

        array.set(idx, value);
    }
}
