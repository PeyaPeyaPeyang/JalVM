package tokyo.peya.langjal.vm.engine.stacking.instructions.references;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.TypeInsnNode;
import tokyo.peya.langjal.compiler.jvm.ClassReferenceType;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorANewArray extends AbstractInstructionOperator<TypeInsnNode>
{

    public OperatorANewArray()
    {
        super(EOpcodes.ANEWARRAY, "anewarray");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull TypeInsnNode operand)
    {
        VMSystemClassLoader cl = frame.getClassLoader();

        String descriptor = operand.desc;
        ClassReferenceType classReferenceType = ClassReferenceType.parse(descriptor);
        VMClass vmClass = cl.findClass(ClassReference.of(classReferenceType));
        VMInteger arrayLength = frame.getStack().popType(VMType.of(frame, PrimitiveTypes.INT));

        int length = arrayLength.asNumber().intValue();
        if (length < 0)
            throw new VMPanic("Array length cannot be negative: " + length);

        VMArray array = new VMArray(frame.getVM(), vmClass, length);
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
