package tokyo.peya.langjal.vm.engine.stacking.instructions.references;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.TypeDescriptor;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.panics.CodeThrownVMPanic;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMReferenceValue;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorAThrow extends AbstractInstructionOperator<InsnNode>
{

    public OperatorAThrow()
    {
        super(EOpcodes.ATHROW, "athrow");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        VMReferenceValue referenceValue = frame.getStack().popType(VMType.of(
                frame,
                TypeDescriptor.className("java/lang/Throwable")
        ));

        if (!(referenceValue instanceof VMObject obj))
            throw new VMPanic("Expected an object to throw, but got " + referenceValue.getClass().getSimpleName());

        throw new CodeThrownVMPanic(obj);
    }
}
