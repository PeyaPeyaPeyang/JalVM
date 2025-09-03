package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.FieldNode;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

import java.lang.reflect.Modifier;

public abstract class InjectedField extends VMField
{

    public InjectedField(@NotNull VMClass clazz, @NotNull VMType<?> fieldType, @NotNull FieldNode fieldNode)
    {
        super(
              clazz.getVM(),
              clazz,
              retrieveOriginalSlot(clazz, fieldNode),
              retrieveOriginalID(clazz, fieldNode),
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
                .orElseGet(() -> (node.access & Modifier.STATIC) == 0
                        ? clazz.getNextSlot()
                        : clazz.getVM().getHeap().assignStaticFieldID());
    }

    private static int retrieveOriginalSlot(@NotNull VMClass clazz, @NotNull FieldNode node)
    {
        return clazz.getFields().stream()
                    .filter(f -> f.getName().equals(node.name))
                    .findFirst()
                    .map(VMField::getSlot)
                    .orElseGet(clazz::getNextSlot);
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
