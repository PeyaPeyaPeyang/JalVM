package tokyo.peya.langjal.vm.values.metaobjects;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMBoolean;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;

@Getter
public class VMFieldObject extends VMObject
{
    private final VMSystemClassLoader classLoader;
    private final VMField field;

    public VMFieldObject(@NotNull VMSystemClassLoader classLoader, @NotNull VMField field)
    {
        super(classLoader.findClass(ClassReference.of("java/lang/reflect/Field")));
        this.field = field;
        this.classLoader = classLoader;

        this.setField("clazz", field.getOwningClass().getClassObject());
        this.setField("name", VMStringObject.createString(classLoader, field.getName()));
        this.setField("type", field.getClazz().getClassObject());
        this.setField("modifiers", new VMInteger(field.getFieldNode().access));
        this.setField("trustedFinal", VMBoolean.FALSE);
        this.setField("slot", new VMInteger(field.getFieldSlot()));
        this.setField("signature", VMStringObject.createString(classLoader, field.getFieldNode().signature));
        this.setField("annotations", new VMArray(classLoader, VMType.BYTE, 0));

        this.forceInitialise();
    }
}
