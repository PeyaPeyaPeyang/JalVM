package tokyo.peya.langjal.vm.engine.stacking.instructions.loads;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.exceptions.IllegalOperationPanic;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMValue;

public class OperatorXALoadSupporter {
    public static void execute(@NotNull VMFrame frame, @NotNull InsnNode operand, @Nullable Class<? extends VMValue> type) {
        VMInteger index = frame.getStack().popType(VMInteger.class);
        VMArray array = frame.getStack().popType(VMArray.class);

        VMValue value = array.get(index.asNumber().intValue());
        if (!type.isInstance(value))
            throw new IllegalOperationPanic("Expected an " + type.getSimpleName() + " but got " + value.getClass().getSimpleName());

        frame.getStack().push(value);
    }
}
