package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.ClassReferenceType;
import tokyo.peya.langjal.compiler.jvm.Type;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.members.VMField;

import java.util.HashMap;
import java.util.Map;

public class VMObject implements VMValue {
    private final VMClass objectType;
    private final Map<VMField, VMValue> fields;

    public VMObject(@NotNull VMClass objectType) {
        this.objectType = objectType;
        this.fields = initialiseFields();
    }

    private Map<VMField, VMValue> initialiseFields()
    {
        Map<VMField, VMValue> fields = new HashMap<>();
        for (VMField field : this.objectType.getFields())
            fields.put(field, field.defaultValue());

        return fields;
    }


    @Override
    public Type getType() {
        return ClassReferenceType.parse(this.objectType.getReference().getFullQualifiedName());
    }
}
