package tokyo.peya.langjal.vm.engine.stacking.instructions.references;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.FieldInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMReferenceValue;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

public class OperatorGetField extends AbstractInstructionOperator<FieldInsnNode>
{

    public OperatorGetField()
    {
        super(EOpcodes.GETFIELD, "getfield");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull FieldInsnNode operand)
    {
        String name = operand.name;

        VMReferenceValue referenceValue = frame.getStack().popType(VMType.ofGenericObject(frame));
        if (!(referenceValue instanceof VMObject obj))
            throw new VMPanic("Expected an object to access field '" + name + "', but got " + referenceValue.getClass().getSimpleName());

        // クラスをframe.getCL().findClassで取得しないのは，ラムダ用などの動的クラスは名前が重複しているから。
        VMClass clazz = referenceValue.type().getLinkedClass();
        VMField field = clazz.findField(name, ClassReference.of(operand.owner));
        if (!obj.getObjectType().isSubclassOf(clazz))
            throw new VMPanic("Object type " + obj.getObjectType().getReference().getFullQualifiedName()
                                      + " is not a subclass of " + clazz.getReference().getFullQualifiedName());

        if (!field.canAccessFrom(frame.getMethod().getClazz()))
            throw new VMPanic("Field " + name + " cannot be accessed from method "
                                      + frame.getMethod().getClazz().getReference().getFullQualifiedName());

        VMValue value = obj.getField(field);
        VMValue conformed = value.conformValue(field.getType());

        frame.getTracer().pushHistory(
                ValueTracingEntry.fieldAccess(
                        conformed,
                        frame.getMethod(),
                        null,
                        operand,
                        field
                )
        );

        frame.getStack().push(conformed);
    }
}
