package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.FieldNode;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

public abstract class InjectedField extends VMField {

    public InjectedField(@NotNull VMClass clazz, @NotNull VMType fieldType, @NotNull FieldNode fieldNode) {
        super(clazz, fieldType, fieldNode);
    }

    public abstract VMValue get(
            @NotNull VMClass caller,
            @Nullable VMObject instance
    );

    public abstract void set(
            @NotNull VMClass caller,
            @Nullable VMObject instance,
            @NotNull VMValue value
    );
}
