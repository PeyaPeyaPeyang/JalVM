package tokyo.peya.langjal.vm.engine.stacking.instructions.references;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.IntInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorNewArray extends AbstractInstructionOperator<IntInsnNode>
{

    public OperatorNewArray()
    {
        super(EOpcodes.NEWARRAY, "newarray");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull IntInsnNode operand)
    {
        int typeCode = operand.operand;
        VMInteger sizeValue = frame.getStack().popType(VMType.of(frame.getVM(), PrimitiveTypes.INT));
        int size = sizeValue.asNumber().intValue();
        if (size < 0)
            throw new VMPanic("Array size cannot be negative: " + size);

        VMType<?> arrayType = getArrayType(frame, typeCode);
        VMArray array = new VMArray(frame.getVM(), arrayType, size);
        frame.getTracer().pushHistory(
                ValueTracingEntry.generation(array, frame.getMethod(), operand)
        );

        frame.getStack().push(array);
    }

    private static VMType<?> getArrayType(@NotNull VMFrame frame, int typeCode)
    {
        JalVM vm = frame.getVM();
        return switch (typeCode)
        {
            case EOpcodes.T_BOOLEAN -> VMType.of(vm, PrimitiveTypes.BOOLEAN);
            case EOpcodes.T_CHAR -> VMType.of(vm, PrimitiveTypes.CHAR);
            case EOpcodes.T_FLOAT -> VMType.of(vm, PrimitiveTypes.FLOAT);
            case EOpcodes.T_DOUBLE -> VMType.of(vm, PrimitiveTypes.DOUBLE);
            case EOpcodes.T_BYTE -> VMType.of(vm, PrimitiveTypes.BYTE);
            case EOpcodes.T_SHORT -> VMType.of(vm, PrimitiveTypes.SHORT);
            case EOpcodes.T_INT -> VMType.of(vm, PrimitiveTypes.INT);
            case EOpcodes.T_LONG -> VMType.of(vm, PrimitiveTypes.LONG);
            default -> throw new VMPanic("Unknown array type code: " + typeCode);
        };
    }
}
