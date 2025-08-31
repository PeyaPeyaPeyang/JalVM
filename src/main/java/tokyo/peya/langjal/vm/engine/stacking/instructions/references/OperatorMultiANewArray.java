package tokyo.peya.langjal.vm.engine.stacking.instructions.references;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import tokyo.peya.langjal.compiler.jvm.ClassReferenceType;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.compiler.jvm.TypeDescriptor;
import tokyo.peya.langjal.vm.JalVM;
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
        JalVM vm = frame.getVM();

        String descriptor = operand.desc;
        if (descriptor.startsWith("["))
            descriptor = descriptor.substring(descriptor.lastIndexOf('[') + 1);
        VMType<?> type = VMType.of(frame, TypeDescriptor.parse(descriptor));
        VMClass vmClass = type.getLinkedClass();

        int dimensions = operand.dims;
        if (dimensions < 1)
            throw new VMPanic("Array dimensions must be greater or equal to 1: " + dimensions);

        int[] sizes = new int[dimensions];
        for (int d = dimensions - 1; d >= 0; d--)
        {
            sizes[d] = frame.getStack().popType(VMType.of(vm, PrimitiveTypes.INT))
                            .asNumber()
                            .intValue();
            if (sizes[d] <= 1)
                throw new VMPanic("Array length must be greater or equal to 1: " + sizes[d]);
        }

        VMArray array = createMultiArrayRecursive(vm, vmClass, frame, sizes, 0, operand);
        frame.getTracer().pushHistory(
                ValueTracingEntry.generation(
                        array,
                        frame.getMethod(),
                        operand
                )
        );
        frame.getStack().push(array);
    }

    private VMArray createMultiArrayRecursive(JalVM vm, VMClass vmClass, VMFrame frame,
                                              int[] sizes, int depth, MultiANewArrayInsnNode operand)
    {
        VMArray array = new VMArray(vm, vmClass, sizes[depth]);
        if (depth >= sizes.length - 1)
            return array;

        VMClass componentClass = array.getArrayType().getLinkedClass();
        for (int i = 0; i < sizes[depth]; i++)
        {
            VMArray child = createMultiArrayRecursive(vm, componentClass, frame, sizes, depth + 1, operand);
            array.set(i, child);
            frame.getTracer().pushHistory(
                    ValueTracingEntry.generation(child, frame.getMethod(), operand)
            );
        }
        return array;
    }
}
