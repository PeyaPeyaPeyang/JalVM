package tokyo.peya.langjal.vm.values;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

@UtilityClass
public class VMStringCreator {
    public static VMValue createString(@Nullable String value) {
        if (value == null)
            return new VMNull(VMType.STRING);
        VMObject stringObject = VMType.STRING.createInstance();

        VMByte[] vmChars = new VMByte[value.length() * 2];
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            byte lowByte = (byte) (c & 0xFF);
            byte highByte = (byte) ((c >> 8) & 0xFF);
            vmChars[i * 2] = new VMByte(lowByte);
            vmChars[i * 2 + 1] = new VMByte(highByte);
        }

        int hash = value.hashCode();
        boolean hashIsZero = hash == 0;
        byte coder = 0; // Default coder for UTF-16

        stringObject.setField(
                "value",
                new VMArray(VMType.BYTE, vmChars)
        );
        stringObject.setField(
                "hash",
                new VMInteger(hash)
        );
        stringObject.setField(
                "hashIsZero",
                VMBoolean.of(hashIsZero)
        );
        stringObject.setField(
                "coder",
                new VMByte(coder)
        );

        stringObject.forceInitialise();
        return stringObject;
    }

    public static String getString(@NotNull VMObject stringObj) {
        if (!stringObj.getType().equals(VMType.STRING))
            throw new VMPanic("Expected a VMObject of type String, but got: " + stringObj.getType());

        VMArray valueArray = (VMArray) stringObj.getField("value");
        if (valueArray == null)
            throw new VMPanic("String value is null in VMObject: " + stringObj);

        StringBuilder sb = new StringBuilder(valueArray.length() / 2);

        for (int i = 0; i < valueArray.length(); i += 2) {
            byte lowByte = ((VMByte) valueArray.get(i)).asNumber().byteValue();
            byte highByte = ((VMByte) valueArray.get(i + 1)).asNumber().byteValue();
            char c = (char) ((highByte << 8) | (lowByte & 0xFF));
            sb.append(c);
        }

        return sb.toString();
    }
}
