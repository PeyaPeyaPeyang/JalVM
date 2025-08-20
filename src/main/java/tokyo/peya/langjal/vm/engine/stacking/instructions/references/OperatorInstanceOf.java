package tokyo.peya.langjal.vm.engine.stacking.instructions.references;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.TypeInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.TypeDescriptor;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMReferenceValue;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorInstanceOf extends AbstractInstructionOperator<TypeInsnNode>
{

    public OperatorInstanceOf()
    {
        super(EOpcodes.INSTANCEOF, "instanceof");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull TypeInsnNode operand)
    {
        VMReferenceValue object = frame.getStack().popType(VMType.GENERIC_OBJECT);
        VMInteger result = new VMInteger(checkType(frame.getVm().getClassLoader(), object, operand));
        frame.getTracer().pushHistory(
                ValueTracingEntry.generation(
                        result,
                        frame.getMethod(),
                        operand
                )
        );

        frame.getStack().push(result);
    }

    public static int checkType(@NotNull VMSystemClassLoader cl, @NotNull VMReferenceValue obj, @NotNull TypeInsnNode operand)
    {
        VMType<?> type = VMType.of(parseAmbiguousType(operand.desc));
        type.linkClass(cl);

        return checkType(obj, type);
    }

    private static TypeDescriptor parseAmbiguousType(@NotNull String mayDesc)
    {
        String desc = mayDesc.trim();

        // 配列の次元を数える
        int arrayDepth = 0;
        while (desc.startsWith("["))
        {
            arrayDepth++;
            desc = desc.substring(1);
        }

        // プリミティブはそのまま、クラス型なら L..; で囲む
        if (desc.length() > 1 || !isPrimitive(desc.charAt(0)))
            if (!desc.endsWith(";"))  // すでに末尾に ; がある場合は追加しない
                desc = "L" + desc + ";";

        // 配列の次元を戻す
        String normalized = "[".repeat(Math.max(0, arrayDepth)) + desc;
        return TypeDescriptor.parse(normalized);
    }

    private static boolean isPrimitive(char c)
    {
        return "BCDFIJSZ".indexOf(c) >= 0;
    }

    public static int checkType(@NotNull VMReferenceValue obj, @NotNull VMType<?> type)
    {
        VMClass typeClass = type.getLinkedClass();
        int arrayDimensions = type.getArrayDimensions();

        if (obj instanceof VMArray vmArray)
        {
            VMType<?> arrayType = vmArray.getArrayType();
            VMClass arrayClass = arrayType.getLinkedClass();
            if (arrayClass.equals(VMType.GENERIC_OBJECT.getLinkedClass()))
                return 1;  // Object[] は Object のサブクラス

            if (arrayType.getArrayDimensions() != arrayDimensions)
                return 0;  // 次元が違う場合はダメ

            return arrayType.getLinkedClass().isSubclassOf(typeClass) ? 1 : 0;
        }
        else if (obj instanceof VMObject vmObj)
        {
            VMClass objClass = vmObj.getObjectType();
            return objClass.isSubclassOf(typeClass) ? 1 : 0;
        }

        return 0;  // VMNull とかは 0
    }
}
