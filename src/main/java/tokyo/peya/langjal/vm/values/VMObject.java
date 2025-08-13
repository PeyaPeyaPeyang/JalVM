package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.compiler.jvm.AccessAttribute;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threads.VMThread;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VMObject implements VMValue {
    private final VMClass objectType;
    private final Map<VMField, VMValue> fields;

    private boolean isInitialised = false;

    public VMObject(@NotNull VMClass objectType) {
        this.objectType = objectType;
        this.fields = createFields();
    }

    private Map<VMField, VMValue> createFields()
    {
        Map<VMField, VMValue> fields = new HashMap<>();
        for (VMField field : this.objectType.getFields())
            fields.put(field, null);

        return fields;
    }

    public void initialiseInstance(@NotNull VMThread thread, @Nullable VMClass caller, @NotNull VMValue[] args) {
        if (this.isInitialised)
            throw new VMPanic("Object has already been initialized: " + this.objectType.getReference().getFullQualifiedName());

        for (VMField field : this.objectType.getFields()) {
            if (!field.getType().isPrimitive() && field.getAccessAttributes().has(AccessAttribute.FINAL))
                throw new VMPanic("Field is final and not initialized: " + field.getName() + " in " + this.objectType.getReference().getFullQualifiedName());

            // フィールドのデフォルト値を設定
            this.fields.put(field, field.defaultValue());
        }

        VMType[] argTypes = Arrays.stream(args)
                .map(VMValue::getType)
                .toArray(VMType[]::new);
        // 初期化を呼び出す
        VMMethod constructorMethod = this.objectType.findConstructor(caller, argTypes);
        if (constructorMethod == null)
            throw new IllegalStateException("No suitable constructor found for class: " + this.objectType.getReference().getFullQualifiedName());

        // コンストラクタを実行
        constructorMethod.invokeVirtual(
                thread,
                this.objectType,
                this,
                args
        );

        this.isInitialised = true;
    }

    @Override
    public @NotNull VMType getType() {
        return this.objectType.getType();
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other) {
        VMType otherType;
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
