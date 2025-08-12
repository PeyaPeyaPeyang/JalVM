package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.ClassReferenceType;
import tokyo.peya.langjal.compiler.jvm.Type;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMField;

import java.util.Map;

public class VMObject implements VMValue {
    private final VMClass objectType;
    private final Map<VMField, VMValue> fields;

    public VMObject(@NotNull VMClass objectType) {
        this.objectType = objectType;
    }


    @Override
    public Type getType() {
        return ClassReferenceType.parse(this.objectType.getReference().getFullQualifiedName());
    }
}
