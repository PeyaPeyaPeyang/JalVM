package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.FieldNode;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

public abstract class InjectedField extends VMField
{

    public InjectedField(@NotNull VMClass clazz, @NotNull VMType<?> fieldType, @NotNull FieldNode fieldNode)
    {
        super(
                retrieveOriginalID(clazz, fieldNode),
              clazz.getClassLoader(),
              clazz,
              fieldType,
              fieldNode
        );
    }

    private static long retrieveOriginalID(@NotNull VMClass clazz, @NotNull FieldNode node)
    {
        return clazz.getFields().stream()
                .filter(f -> f.getName().equals(node.name))
                .findFirst()
                .map(VMField::getFieldID)
                .orElseThrow(() -> new IllegalStateException("Original field ID not found for " + node.name));
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
