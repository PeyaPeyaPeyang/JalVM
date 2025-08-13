package tokyo.peya.langjal.vm.values;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threads.VMThread;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

@UtilityClass
public class VMStringCreator {
    public static VMValue createString(@NotNull VMThread thread, @Nullable String value) {
        if (value == null) {
            return new VMNull(VMType.STRING);
        }
        VMObject stringObject = VMType.STRING.createInstance();
        VMMethod constructor = VMType.STRING.getLinkedClass().findConstructor(
                null,
                VMType.CHAR
        );
        if (constructor == null)
            throw new VMPanic("VM BUG!!! No suitable constructor found for String class");

        VMValue[] args = new VMValue[value.length()];
        for (int i = 0; i < value.length(); i++)
            args[i] = new VMChar((char) i);
        constructor.invokeVirtual(
                thread,
                null,
                stringObject,
                args
        );

        return stringObject;
    }
}
