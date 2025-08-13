package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.compiler.jvm.AccessAttribute;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.injections.InjectedField;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threads.VMThread;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class VMObject implements VMValue {
    private final VMClass objectType;
    private final Map<VMField, VMValue> fields;

    private boolean isInitialised = false;

    public VMObject(@NotNull VMClass objectType) {
        this.objectType = objectType;
        this.fields = createFields();
    }

    private Map<VMField, VMValue> createFields() {
        Map<VMField, VMValue> fields = new HashMap<>();
        for (VMField field : this.objectType.getFields())
            fields.put(field, null);

        return fields;
    }

    /* non-public */ void forceInitialise() {  // 強制的に初期化を行う。
        if (this.isInitialised)
            throw new VMPanic("Object has already been initialized: " + this.objectType.getReference().getFullQualifiedName());

        this.isInitialised = true;
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
                true,
                args
        );

        this.isInitialised = true;
    }

    public void setField(@NotNull String fieldName, @NotNull VMValue value) {
        VMField field = this.fields.keySet().stream()
                .filter(f -> f.getName().equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new VMPanic("Field not found: " + fieldName + " in " + this.objectType.getReference().getFullQualifiedName()));
        if (!field.getType().isAssignableFrom(value.getType()))
            throw new VMPanic("Incompatible value type for field: " + fieldName);

        if (field instanceof InjectedField injected)
            injected.set(this.objectType, this, value);
        else
            this.fields.put(field, value);
    }

    public @Nullable VMValue getField(@NotNull String fieldName) {
        VMField field = this.fields.keySet().stream()
                .filter(f -> f.getName().equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new VMPanic("Field not found: " + fieldName + " in " + this.objectType.getReference().getFullQualifiedName()));
        if (field instanceof InjectedField injected)
            return injected.get(this.objectType, this);
        else
            return this.fields.get(field);
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
                    .append(entry.getValue() == null ? "?" : entry.getValue().toString());
        sb.append("\n}");
        return sb.toString();
    }
}
