package tokyo.peya.langjal.vm.engine.members;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.FieldNode;
import tokyo.peya.langjal.vm.VMClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

@Getter
public class VMField {
    private final VMClass clazz;
    private final FieldNode fieldNode;

    private final VMType type;
    private final String name;

    public VMField(@NotNull VMClass clazz, @NotNull VMType fieldType, FieldNode fieldNode) {
        this.clazz = clazz;
        this.fieldNode = fieldNode;
        this.type = fieldType;
        this.name = fieldNode.name;
    }

    public VMValue defaultValue() {
        return this.type.defaultValue();
    }

    public void linkType(@NotNull VMClassLoader cl) {
        this.type.linkClass(cl);
    }
}
