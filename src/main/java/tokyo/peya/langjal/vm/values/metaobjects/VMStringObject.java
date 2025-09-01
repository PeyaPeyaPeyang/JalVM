package tokyo.peya.langjal.vm.values.metaobjects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMBoolean;
import tokyo.peya.langjal.vm.values.VMByte;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMNull;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

import java.util.HashMap;
import java.util.Map;

public class VMStringObject extends VMObject
{
    private static final byte LATIN1 = 0; // JDK互換
    private static final byte UTF16  = 1; // JDK互換

    private static final Map<String, VMValue> STRING_CACHE = new HashMap<>();

    private VMStringObject(@NotNull VMComponent component, @NotNull String content)
    {
        super(component.getClassLoader().findClass(ClassReference.of("java/lang/String")));

        this.createString0(component, content);
    }

    public VMStringObject(@NotNull VMComponent component)
    {
        super(component.getClassLoader().findClass(ClassReference.of("java/lang/String")));
    }

    private void createString0(@NotNull VMComponent component, @NotNull String value)
    {
        final int len = value.length();
        if (len == 0)
        {
            this.setField("value", new VMArray(component, VMType.of(component, PrimitiveTypes.BYTE), new VMByte[0]));
            this.setField("hash", new VMInteger(component, 0));
            this.setField("hashIsZero", VMBoolean.ofTrue(component));
            this.setField("coder", new VMByte(component, LATIN1)); // JDK でも空文字は coder=LATIN1
            this.forceInitialise(component.getClassLoader());
            return;
        }

        // Latin-1 に収まるか判定（全演舞が U+00FF 以下）
        boolean canLatin1 = true;
        for (int i = 0; i < len; i++)
        {
            char c = value.charAt(i);
            if (c > 0xFF)
            {
                canLatin1 = false;
                break;
            }
        }

        VMByte[] vmBytes;
        byte coder;
        if (canLatin1)
        {
            // Latin-1 圧縮（1 バイト/文字）
            coder = LATIN1;
            vmBytes = new VMByte[len];
            for (int i = 0; i < len; i++)
            {
                char c = value.charAt(i);
                vmBytes[i] = new VMByte(component, (byte) (c & 0xFF));
            }
        }
        else
        {
            // UTF-16（2 バイト/char）— JDK の StringUTF16 と同じく「上位→下位」順で格納
            coder = UTF16;
            vmBytes = new VMByte[len * 2];
            for (int i = 0; i < len; i++)
            {
                char c = value.charAt(i);
                byte hi = (byte) ((c >>> 8) & 0xFF);
                byte lo = (byte) (c & 0xFF);
                int base = i * 2;
                vmBytes[base]     = new VMByte(component, hi); // 上位バイト
                vmBytes[base + 1] = new VMByte(component, lo); // 下位バイト
            }
        }

        int hash = value.hashCode();
        boolean hashIsZero = (hash == 0);

        this.setField(
                "value",
                new VMArray(component, VMType.of(component, PrimitiveTypes.BYTE), vmBytes)
        );
        this.setField("hash", new VMInteger(component, hash));
        this.setField("hashIsZero", VMBoolean.of(component, hashIsZero));
        this.setField("coder", new VMByte(component, coder));

        this.forceInitialise(component.getClassLoader());
    }

    public static VMValue createString(@NotNull VMComponent component, @Nullable String value)
    {
        if (value == null)
            return new VMNull<>(VMType.ofClassName(component, "java/lang/String"));
        if (STRING_CACHE.containsKey(value))
            return STRING_CACHE.get(value);

        VMStringObject stringValue = new VMStringObject(component, value);
        STRING_CACHE.put(value, stringValue);

        return stringValue;
    }

    public static VMArray createStringArray(@NotNull VMComponent component, @NotNull String[] values)
    {
        VMValue[] stringArray = new VMValue[values.length];
        for (int i = 0; i < values.length; i++)
            stringArray[i] = createString(component, values[i]);

        return new VMArray(component, VMType.ofClassName(component, "java/lang/String"), stringArray);
    }

    public String getString()
    {
        if (!this.type().getTypeDescriptor().equals("Ljava/lang/String;"))
            throw new VMPanic("Expected a VMObject of type String, but got: " + this.type());

        VMArray valueArray = (VMArray) this.getField("value");

        StringBuilder sb;

        byte coder = ((VMByte) this.getField("coder")).asNumber().byteValue();
        if (coder == LATIN1) // LATIN1
        {
            // 1 byte = 1 char (0x00..0xFF)
            sb = new StringBuilder(valueArray.length());
            for (int i = 0; i < valueArray.length(); i++)
            {
                byte b = ((VMByte) valueArray.get(i)).asNumber().byteValue();
                char c = (char) (b & 0xFF); // ゼロ拡張
                sb.append(c);
            }
        }
        else if (coder == UTF16) // UTF16
        {
            // 2 bytes = 1 char (big endian, hi→lo)
            sb = new StringBuilder(valueArray.length() / 2);
            for (int i = 0; i < valueArray.length(); i += 2)
            {
                byte hi = ((VMByte) valueArray.get(i)).asNumber().byteValue();
                byte lo = ((VMByte) valueArray.get(i + 1)).asNumber().byteValue();
                char c = (char) (((hi & 0xFF) << 8) | (lo & 0xFF));
                sb.append(c);
            }
        }
        else
            throw new VMPanic("Unknown codec: " + coder);

        return sb.toString();
    }

    @Override
    public @NotNull String toString()
    {
        byte[] value = ((VMArray) this.getField("value")).toByteArray();
        byte coder = ((VMByte) this.getField("coder")).asNumber().byteValue();
        String str = this.getString();

        return "string(value: " + str + ", length: " + value.length +
                ", coder: " + (coder == LATIN1 ? "LATIN1" : "UTF16") + ")";
    }
}
