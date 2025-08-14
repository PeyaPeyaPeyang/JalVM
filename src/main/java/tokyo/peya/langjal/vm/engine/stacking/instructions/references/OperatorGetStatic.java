package tokyo.peya.langjal.vm.engine.stacking.instructions.references;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.FieldInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMValue;


public class OperatorGetStatic extends AbstractInstructionOperator<FieldInsnNode> {

    public OperatorGetStatic() {
        super(EOpcodes.GETSTATIC, "getstatic");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull FieldInsnNode operand) {
        String owner = operand.owner;
        String name = operand.name;
        String desc = operand.desc;

        // Retrieve the static field value from the class
        VMClass clazz = frame.getVm().getClassLoader().findClass(ClassReference.of(owner));
        VMField field = clazz.findStaticField(name);
        VMValue value = clazz.getStaticFieldValue(field);

        frame.getTracer().pushHistory(
                ValueTracingEntry.fieldAccess(
                        value,
                        frame.getMethod(),
                        null,
                        operand,
                        field
                )
        );

        frame.getStack().push(value);
    }
}
