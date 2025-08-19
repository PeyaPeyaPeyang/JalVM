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
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMReferenceValue;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

public class OperatorPutField extends AbstractInstructionOperator<FieldInsnNode>
{

    public OperatorPutField()
    {
        super(EOpcodes.PUTFIELD, "putfield");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull FieldInsnNode operand)
    {
        String owner = operand.owner;
        VMClass clazz = frame.getVm().getClassLoader().findClass(ClassReference.of(owner));
        String name = operand.name;

        VMValue value = frame.getStack().pop();

        VMReferenceValue referenceValue = frame.getStack().popType(clazz);
        if (!(referenceValue instanceof VMObject object))
            throw new VMPanic("Expected an object to access field '" + name + "', but got " + referenceValue.getClass().getSimpleName());

        VMField field = clazz.findField(name);
        if (!object.getObjectType().isSubclassOf(clazz))
            throw new VMPanic("Object type " + object.getObjectType().getReference().getFullQualifiedName()
                                      + " is not a subclass of " + clazz.getReference().getFullQualifiedName());
        if (!field.canAccessFrom(frame.getMethod().getClazz()))
            throw new VMPanic("Field " + name + " cannot be accessed from method "
                                      + frame.getMethod().getClazz().getReference().getFullQualifiedName());

        VMType<?> fieldType = field.getType();
        VMValue conformedValue = value.conformValue(fieldType);

        frame.getTracer().pushHistory(
                ValueTracingEntry.fieldSet(
                        conformedValue,
                        frame.getMethod(),
                        operand,
                        field
                )
        );
        object.setField(field, conformedValue);
    }
}
