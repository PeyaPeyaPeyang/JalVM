package tokyo.peya.langjal.vm.engine.stacking.instructions.references;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.TypeInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.values.VMNull;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

public class OperatorCheckCast extends AbstractInstructionOperator<TypeInsnNode>
{

    public OperatorCheckCast()
    {
        super(EOpcodes.CHECKCAST, "checkcast");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull TypeInsnNode operand)
    {
        VMValue object = frame.getStack().peekType(VMType.ofGenericObject(frame));
        if (object instanceof VMNull<?>)
            return;  // null はなんでも OK

        int checkTypeResult = OperatorInstanceOf.checkType(frame.getVM(), object, operand);
        if (checkTypeResult == 0)
            throw new VMPanic("Cannot cast " + object.type() + " to " + operand.desc);
    }
}
