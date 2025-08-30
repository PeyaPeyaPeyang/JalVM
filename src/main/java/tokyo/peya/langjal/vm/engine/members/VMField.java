package tokyo.peya.langjal.vm.engine.members;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.FieldNode;
import tokyo.peya.langjal.compiler.jvm.AccessAttributeSet;
import tokyo.peya.langjal.compiler.jvm.AccessLevel;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.reflection.VMFieldObject;

@Getter
public class VMField implements AccessibleObject
{
    private final JalVM vm;

    private final VMClass clazz;
    private final int slot;
    private final long fieldID;
    private final FieldNode fieldNode;

    private final AccessLevel accessLevel;
    private final AccessAttributeSet accessAttributes;

    private final VMType<?> type;
    private final String name;

    @Getter(lombok.AccessLevel.NONE)
    private VMFieldObject fieldObject;

    public VMField(@NotNull JalVM vm, @NotNull VMClass clazz, int slot, long id,
                   @NotNull VMType<?> fieldType, @NotNull FieldNode fieldNode)
    {
        this.slot = slot;
        this.fieldID = id;
        this.vm = vm;
        this.clazz = clazz;
        this.fieldNode = fieldNode;

        this.accessLevel = AccessLevel.fromAccess(fieldNode.access);
        this.accessAttributes = AccessAttributeSet.fromAccess(fieldNode.access);

        this.type = fieldType;
        this.name = fieldNode.name;
    }

    public VMFieldObject getFieldObject()
    {
        if (this.fieldObject == null)
            this.fieldObject = new VMFieldObject(this.vm, this);
        return this.fieldObject;
    }

    public VMValue defaultValue()
    {
        Object fieldDefaultValue = this.fieldNode.value;
        if (fieldDefaultValue == null)
            return this.type.defaultValue();

        VMValue value = VMValue.fromJavaObject(this.vm, fieldDefaultValue);
        return value.conformValue(this.type);
    }

    @Override
    public VMClass getOwningClass()
    {
        return this.clazz;
    }

    @Override
    public String toString()
    {
        return this.name + "@" + this.clazz.getReference().getFullQualifiedName() +
                " (" + this.type.getType().getDescriptor() + ")" +
                " [" + this.accessLevel + "]";
    }
}
