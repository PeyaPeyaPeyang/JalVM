package tokyo.peya.langjal.vm.values.metaobjects;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMValue;

public class VMListAccessor
{
    public static VMValue[] values(@NotNull VMObject list)
    {
        // 標準のリストから要素を取り出す。
        VMClass listType = list.getObjectType();
        String internalName = listType.getReference().getFullQualifiedName();
        switch (internalName)
        {
            case "java/util/ArrayList" -> {
                return ((VMArray) list.getField("elementData")).getElements();
            }
            case "java/util/LinkedList" -> {
                VMObject first = (VMObject) list.getField("first");
                VMValue[] values = new VMValue[((VMInteger) list.getField("size")).asNumber().intValue()];
                for (int i = 0; i < values.length; i++)
                {
                    values[i] = first.getField("item");
                    first = (VMObject) first.getField("next");
                }
                return values;
            }
            case "java/util/ImmutableCollections$ListN" -> {
                return ((VMArray) list.getField("elements")).getElements();
            }
            case "java/util/ImmutableCollections$List12" -> {
                VMValue[] elements = new VMValue[2];
                elements[0] = list.getField("e0");
                elements[1] = list.getField("e1");

                // 取得したリストから EMPTY をハネる
                VMClass listClass = list.getObjectType().getClassLoader().findClass(
                        ClassReference.of("java/util/ImmutableCollections")
                );
                VMField emptyField = listClass.findField("EMPTY");
                VMObject emptyValue = (VMObject) listClass.getStaticFieldValue(emptyField);
                if (elements[0] == emptyValue)
                    return new VMValue[0];
                else if (elements[1] == emptyValue)
                    return new VMValue[]{elements[0]};

                return elements;
            }
            case "java/util/Collections$UnmodifiableList" -> {
                return values((VMObject) list.getField("list"));
            }
            default -> throw new IllegalArgumentException("Unsupported list type: " + internalName);
        }
    }
}
