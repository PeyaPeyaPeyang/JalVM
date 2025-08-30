package tokyo.peya.langjal.vm.engine.stacking.instructions.references;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import tokyo.peya.langjal.compiler.jvm.ClassReferenceType;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.TypeDescriptor;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorMultiANewArray extends AbstractInstructionOperator<MultiANewArrayInsnNode>
{

    public OperatorMultiANewArray()
    {
        super(EOpcodes.MULTIANEWARRAY, "multianewarray");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull MultiANewArrayInsnNode operand)
    {
        VMSystemClassLoader cl = frame.getVm().getClassLoader();

        String descriptor = operand.desc;
        if (descriptor.startsWith("["))
            descriptor = descriptor.substring(descriptor.lastIndexOf('[') + 1);
        VMType<?> type = VMType.of(TypeDescriptor.parse(descriptor)).linkClass(cl);
        VMClass vmClass = type.getLinkedClass();

        int dimensions = operand.dims;
        if (dimensions < 1)
            throw new VMPanic("Array dimensions must be greater or equal to 1: " + dimensions);

        VMArray array = new VMArray(cl, vmClass, 0);
        for (int i = dimensions - 1; i >= 0; i--)
        {
            int count = frame.getStack().popType(VMType.INTEGER).asNumber().intValue();
            if (count < 0)
                throw new VMPanic("Array length cannot be negative: " + count);

            VMArray newArray = new VMArray(cl, array.getArrayType().getLinkedClass(), count);
            newArray.linkClass(cl);
            for (int j = 0; j < count; j++)
                newArray.set(j, array);
            frame.getTracer().pushHistory(
                    ValueTracingEntry.generation(
                            newArray,
                            frame.getMethod(),
                            operand
                    )
            );
            array = newArray;
        }

        frame.getTracer().pushHistory(
                ValueTracingEntry.generation(
                        array,
                        frame.getMethod(),
                        operand
                )
        );
        frame.getStack().push(array);
    }
}
