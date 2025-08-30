package tokyo.peya.langjal.vm.engine.stacking.instructions.references;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.FieldInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

public class OperatorGetStatic extends AbstractInstructionOperator<FieldInsnNode>
{

    public OperatorGetStatic()
    {
        super(EOpcodes.GETSTATIC, "getstatic");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull FieldInsnNode operand)
    {
        String owner = operand.owner;
        String name = operand.name;

        // Retrieve the static field value from the class
        VMClass clazz = frame.getClassLoader().findClass(ClassReference.of(owner));
        if (!clazz.isInitialised())
        {
            // クラスが初期化されていない場合は初期化をして，この命令を再実行する
            clazz.initialise(frame.getThread());
            frame.rerunInstruction();
            return;
        }

        VMField field = clazz.findField(name);
        if (!field.canAccessFrom(frame.getMethod().getClazz()))
            throw new VMPanic("Static field " + name + " cannot be accessed from method "
                                      + frame.getMethod().getClazz().getReference().getFullQualifiedName());

        VMValue value = clazz.getStaticFieldValue(field);
        VMValue conformedValue = value.conformValue(field.getType());

        frame.getTracer().pushHistory(
                ValueTracingEntry.fieldAccess(
                        conformedValue,
                        frame.getMethod(),
                        null,
                        operand,
                        field
                )
        );

        frame.getStack().push(conformedValue);
    }
}
