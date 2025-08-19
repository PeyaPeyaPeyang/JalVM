package tokyo.peya.langjal.vm.values;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.injections.InjectedField;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threads.VMThread;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class VMObject implements VMValue, VMReferenceValue
{
    private final VMObject owner;

    @Getter
    private final VMClass objectType;
    private final VMObject superObject;
    private final Map<VMField, VMValue> fields;

    @Getter
    private boolean isInitialised;

    public VMObject(@NotNull VMClass objectType, @Nullable VMObject owner)
    {
        this.owner = owner == null ? this : owner; // オーナーがnullの場合は自身をオーナーとする
        this.objectType = objectType;
        this.fields = createFields();

        if (objectType.getReference().isEqualClass("java/lang/Object"))
            this.superObject = null; // Objectクラスはスーパークラスを持たないようにする。
        else
            this.superObject = objectType.getSuperLink().createInstance(this.owner);
    }

    public VMObject(@NotNull VMClass objectType)
    {
        this(objectType, null); // オーナーをnullにしてインスタンスを作成
    }

    private Map<VMField, VMValue> createFields()
    {
        Map<VMField, VMValue> fields = new HashMap<>();
        for (VMField field : this.objectType.getFields())
            fields.put(field, null);

        return fields;
    }

    protected void forceInitialise()
    {  // 強制的に初期化を行う。
        if (this.isInitialised)
            throw new VMPanic("Object has already been initialized: " + this.objectType.getReference()
                                                                                       .getFullQualifiedName());

        this.setDefaultValues();
        this.isInitialised = true;
    }

    private void setDefaultValues()
    {
        for (VMField field : this.objectType.getFields())
        {
            if (this.fields.containsKey(field) && this.fields.get(field) != null)
                continue; // 既にフィールドが存在する場合はスキップ
            // フィールドのデフォルト値を設定
            this.fields.put(field, field.defaultValue());
        }
    }

    public void initialiseInstance(@NotNull VMThread thread, @Nullable VMClass caller, @NotNull VMClass owner,
                                   @NotNull VMType<?>[] argTypes, @NotNull VMValue[] args, boolean isVMDecree)
    {
        this.setDefaultValues();

        // 初期化を呼び出す
        VMMethod constructorMethod = this.objectType.findConstructor(caller, owner, argTypes);
        if (constructorMethod == null)
            throw new IllegalStateException("No suitable constructor found for class: " + this.objectType.getReference()
                                                                                                         .getFullQualifiedName());

        if (this.owner.getObjectType().equals(owner) && this.isInitialised)
            throw new VMPanic("Object has already been initialized: " + this.objectType.getReference()
                                                                                       .getFullQualifiedName());

        // コンストラクタを実行
        constructorMethod.invokeInstanceMethod(
                null,
                thread,
                this.objectType,
                this.owner,
                isVMDecree,
                args
        );

        if (!owner.equals(caller))
        {
            VMObject target = this;
            do
            {
                target.isInitialised = true;
            }
            while ((target = target.superObject) != null); // スーパークラスのオブジェクトも初期化する
        }
    }

    public void setField(@NotNull VMField field, @NotNull VMValue value)
    {
        VMValue conformedValue = value.conformValue(field.getType()); // 値をフィールドの型に適合させる
        if (!field.getType().isAssignableFrom(conformedValue.type()))
            throw new VMPanic("Incompatible value type for field: " + field.getName());

        if (field instanceof InjectedField injected)
            injected.set(this.objectType, this, value);
        else
            this.fields.put(field, value);
    }

    public void setField(@NotNull String fieldName, @NotNull VMValue value)
    {
        VMField field = this.fields.keySet().stream()
                                   .filter(f -> f.getName().equals(fieldName))
                                   .findFirst()
                                   .orElseThrow(() -> new VMPanic("Field not found: " + fieldName + " in " + this.objectType.getReference()
                                                                                                                            .getFullQualifiedName()));
        this.setField(field, value);
    }

    public @NotNull VMValue getField(@NotNull VMField field)
    {
        VMValue value;
        if (field instanceof InjectedField injected)
            value = injected.get(this.objectType, this);
        else
            value = this.fields.get(field);
        if (value == null)
            throw new VMPanic("Field not initialized: " + field.getName() + " in " + this.objectType.getReference()
                                                                                                    .getFullQualifiedName());
        return value;
    }

    public @Nullable VMValue getField(@NotNull String fieldName)
    {
        VMField field = this.fields.keySet().stream()
                                   .filter(f -> f.getName().equals(fieldName))
                                   .findFirst()
                                   .orElseThrow(() -> new VMPanic("Field not found: " + fieldName + " in " + this.objectType.getReference()));
        return this.getField(field);
    }

    @Override
    public @NotNull VMType<?> type()
    {
        return this.objectType;
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other)
    {
        VMType<?> otherType;
        switch (other)
        {
            case VMNull(VMType<?> type) -> otherType = type;
            case VMObject objValue -> otherType = objValue.type();
            case VMArray array ->
            {
                VMType<?> arrayType = array.getObjectType();
                if (arrayType.isPrimitive())
                    return false;
                else
                    return this.objectType.getClazz().name.equals("java/lang/Object");
            }
            default ->
            {
                return false;
            }
        }

        return this.type().equals(otherType);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(this.objectType.getReference().getFullQualifiedName()).append(" {");
        for (Map.Entry<VMField, VMValue> entry : this.fields.entrySet())
            sb.append("\n  ")
              .append(entry.getKey().getName())
              .append(": ")
              .append(entry.getValue() == null ? "?": entry.getValue().toString());
        sb.append("\n}");
        return sb.toString();
    }
}
