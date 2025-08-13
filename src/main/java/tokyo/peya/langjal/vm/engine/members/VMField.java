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

@Getter
public class VMField implements RestrictedAccessor {
    private final VMClass clazz;
    private final FieldNode fieldNode;

    private final AccessLevel accessLevel;
    private final AccessAttributeSet accessAttributes;

    private final VMType type;
    private final String name;

    public VMField(@NotNull VMClass clazz, @NotNull VMType fieldType, @NotNull FieldNode fieldNode) {
        this.clazz = clazz;
        this.fieldNode = fieldNode;

        this.accessLevel = AccessLevel.fromAccess(fieldNode.access);
        this.accessAttributes = AccessAttributeSet.fromAccess(fieldNode.access);

        this.type = fieldType;
        this.name = fieldNode.name;
    }

    public VMValue defaultValue() {
        return this.type.defaultValue();
    }

    public void linkType(@NotNull VMSystemClassLoader cl) {
        this.type.linkClass(cl);
    }

    @Override
    public VMClass getOwningClass() {
        return this.clazz;
    }
}
