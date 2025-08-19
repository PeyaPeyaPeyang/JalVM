package tokyo.peya.langjal.vm.engine.stacking.instructions.references;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import tokyo.peya.langjal.compiler.jvm.ClassReferenceType;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMObject;
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
        VMInteger sizeValue = frame.getStack().popType(VMType.INTEGER);
        int size = sizeValue.asNumber().intValue();
        if (size < 0)
            throw new VMPanic("Array size cannot be negative: " + size);

        VMType<?> arrayType = getArrayType(typeCode);
        VMArray array = new VMArray(arrayType, size);
        frame.getTracer().pushHistory(
                ValueTracingEntry.generation(array, frame.getMethod(), operand)
        );

        frame.getStack().push(array);
    }

    private static VMType<?> getArrayType(int typeCode)
    {
        return switch (typeCode)
        {
            case EOpcodes.T_BOOLEAN -> VMType.BOOLEAN;
            case EOpcodes.T_CHAR -> VMType.CHAR;
            case EOpcodes.T_FLOAT -> VMType.FLOAT;
            case EOpcodes.T_DOUBLE -> VMType.DOUBLE;
            case EOpcodes.T_BYTE -> VMType.BYTE;
            case EOpcodes.T_SHORT -> VMType.SHORT;
            case EOpcodes.T_INT -> VMType.INTEGER;
            case EOpcodes.T_LONG -> VMType.LONG;
            default -> throw new VMPanic("Unknown array type code: " + typeCode);
        };
    }
}
