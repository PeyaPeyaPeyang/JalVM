package tokyo.peya.langjal.vm.values.metaobjects;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMPrimitive;

@Getter
public class VMBoxedPrimitiveObject extends VMObject
{
    private final VMPrimitive value;

    public VMBoxedPrimitiveObject(@NotNull VMClass boxedClass, VMPrimitive value)
    {
        super(boxedClass);
        this.value = value;

        this.setField("value", value);
        this.forceInitialise(boxedClass.getVm().getClassLoader());
    }
}
