package tokyo.peya.langjal.vm.engine.members;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.FieldNode;
import tokyo.peya.langjal.compiler.jvm.AccessAttributeSet;
import tokyo.peya.langjal.compiler.jvm.AccessLevel;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.reflection.VMFieldObject;

@Getter
public class VMField implements RestrictedAccessor
{
    private final VMSystemClassLoader classLoader;

    private final VMClass clazz;
    private final int fieldSlot;
    private final long fieldID;
    private final FieldNode fieldNode;

    private final AccessLevel accessLevel;
    private final AccessAttributeSet accessAttributes;

    private final VMType<?> type;
    private final String name;

    @Getter(lombok.AccessLevel.NONE)
    private VMFieldObject fieldObject;

    public VMField(@NotNull VMSystemClassLoader classLoader, @NotNull VMClass clazz, int fieldSlot, long id,
                   @NotNull VMType<?> fieldType, @NotNull FieldNode fieldNode)
    {
        this.fieldSlot = fieldSlot;
        this.fieldID = id;
        this.classLoader = classLoader;
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
            this.fieldObject = new VMFieldObject(this.classLoader, this);
        return this.fieldObject;
    }

    public VMValue defaultValue()
    {
        Object fieldDefaultValue = this.fieldNode.value;
        if (fieldDefaultValue == null)
            return this.type.defaultValue();

        VMValue value = VMValue.fromJavaObject(this.classLoader, fieldDefaultValue);
        return value.conformValue(this.type);
    }

    public void linkType(@NotNull VMSystemClassLoader cl)
    {
        this.type.linkClass(cl);
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
