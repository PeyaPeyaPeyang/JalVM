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
    public @NotNull Type getType() {
        return ClassReferenceType.parse(this.objectType.getReference().getFullQualifiedName());
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other) {
        Type otherType;
        if (other instanceof VMNull nullValue)
             otherType = nullValue.getType();
        else if (other instanceof VMObject objValue)
            otherType = objValue.getType();
        else
            return false;

        return this.getType().equals(otherType);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.objectType.getReference().getFullQualifiedName()).append(" {");
        for (Map.Entry<VMField, VMValue> entry : fields.entrySet())
            sb.append("\n  ")
              .append(entry.getKey().getName())
              .append(": ")
              .append(entry.getValue().toString());
        sb.append("\n}");
        return sb.toString();
    }
}
