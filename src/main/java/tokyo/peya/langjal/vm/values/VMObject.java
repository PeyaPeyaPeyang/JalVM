package tokyo.peya.langjal.vm.values;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.injections.InjectedField;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threading.VMMonitor;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class VMObject implements VMValue, VMReferenceValue
{
    private final VMObject owner;
    @Getter
    private final VMClass objectType;
    @Getter
    private final VMMonitor monitor;

    private final Map<VMField, VMValue> fields;

    private VMObject superObject;
    @Getter
    private boolean isInitialised;

    public VMObject(@NotNull VMClass objectType, @Nullable VMObject owner)
    {
        this.owner = owner == null ? this : owner; // オーナーがnullの場合は自身をオーナーとする
        this.objectType = objectType;
        this.monitor = new VMMonitor(this);
        this.fields = createFields();
    }

    public VMObject(@NotNull VMClass objectType)
    {
        this(objectType, null); // オーナーをnullにしてインスタンスを作成
    }

    private VMObject(@NotNull VMObject owner, @NotNull VMClass objectType, @NotNull VMObject superObject,
                     @NotNull Map<VMField, VMValue> fields, boolean isInitialised)
    {
        this.owner = owner;
        this.objectType = objectType;
        this.monitor = new VMMonitor(this);
        this.superObject = superObject;
        this.fields = fields;
        this.isInitialised = isInitialised;
    }

    public VMObject getSuperObject()
    {
        if (this.superObject == null
                && !this.objectType.getReference().isEqualClass("java/lang/Object"))
                this.superObject = this.objectType.getSuperLink().createInstance(this.owner);

        return this.superObject;
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
        // 初期化を呼び出す
        VMMethod constructorMethod = this.objectType.findConstructor(caller, owner, argTypes);
        if (constructorMethod == null)
            throw new IllegalStateException("No suitable constructor found for class: " +
                                                    this.objectType.getReference().getFullQualifiedName());

        // ターゲットを選定する
        VMClass targetClass = constructorMethod.getClazz();
        VMObject targetObject = findSuitableTarget(targetClass, this.owner);

        boolean isOuterInitialise = false;
        VMFrame frame = thread.getCurrentFrame();
        if (frame != null)
        {
            VMMethod prevMethod = frame.getMethod();
            if (prevMethod == null || !(prevMethod.getClazz().equals(targetClass) && prevMethod.getName().equals("<init>")))
                isOuterInitialise = true;
        }

        if (isOuterInitialise)
        {
            if (targetObject.isInitialised)
                throw new VMPanic("Outer object is already initialised: " + this.objectType.getReference()
                                                                                          .getFullQualifiedName());
            targetObject.setDefaultValues();
            targetObject.isInitialised = true; // 初期化フラグを立てる
        }

        // コンストラクタを実行
        constructorMethod.invokeInstanceMethod(
                null,
                thread,
                this.objectType,
                this.owner,
                isVMDecree,
                args
        );
    }

    private static @NotNull VMObject findSuitableTarget(@NotNull VMClass targetClass, @NotNull VMObject owner)
    {
        VMObject targetObject = owner;
        do
        {
            if (targetObject.objectType.equals(targetClass))
                break; // ターゲットクラスと同じクラスのオブジェクトを見つけたら終了
            targetObject = targetObject.getSuperObject(); // スーパークラスのオブジェクトに移動
        } while (targetObject != null);
        if (targetObject == null)
            throw new VMPanic("Cannot find target object for constructor: " + targetClass.getReference()
                                                                                         .getFullQualifiedName());
        return targetObject;
    }

    public void setField(@NotNull VMField field, @NotNull VMValue value)
    {
        VMValue conformedValue = value.conformValue(field.getType()); // 値をフィールドの型に適合させる
        if (!field.getType().isAssignableFrom(conformedValue.type()))
            throw new VMPanic("Incompatible value type for field: " + field.getName());

        VMObject suitableTarget = findSuitableTarget(field.getClazz(), this.owner);

        if (field instanceof InjectedField injected)
            injected.set(suitableTarget.objectType, this, value);
        else
            suitableTarget.fields.put(field, value);
    }

    public void setField(@NotNull String fieldName, @NotNull VMValue value)
    {
        VMField field = this.getObjectType().findField(fieldName);
        this.setField(field, value);
    }

    public void setFieldIfExists(@NotNull String fieldName, @NotNull VMValue value)
    {
        VMField field = this.getObjectType().findFieldSafe(fieldName);
        if (field != null)
            this.setField(field, value);
    }
    public @NotNull VMValue getField(@NotNull VMField field)
    {
        VMObject suitableTarget = findSuitableTarget(field.getClazz(), this.owner);

        VMValue value;
        if (field instanceof InjectedField injected)
            value = injected.get(this.objectType, suitableTarget);
        else
            value = suitableTarget.fields.get(field);

        // フィールドがnullの場合はnull値を返す
        return Objects.requireNonNullElseGet(value, () -> new VMNull<>(field.getType()));
    }

    public @NotNull VMValue getField(@NotNull String fieldName)
    {
        VMField field = this.getObjectType().findField(fieldName);
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
            case VMArray array ->
            {
                VMType<?> arrayType = array.getObjectType();
                if (arrayType.isPrimitive())
                    return false;
                else
                    return this.objectType.getClazz().name.equals("java/lang/Object");
            }
            case VMObject objValue -> otherType = objValue.type();
            default ->
            {
                return false;
            }
        }

        return this.type().equals(otherType);
    }

    @Override
    public @NotNull VMObject cloneValue()
    {
        Map<VMField, VMValue> clonedFields = new HashMap<>();
        for (Map.Entry<VMField, VMValue> entry : this.fields.entrySet())
        {
            VMField field = entry.getKey();
            VMValue value = entry.getValue();
            clonedFields.put(field, value == null ? null : value.cloneValue());
        }

        VMObject clonedSuperObject = this.getSuperObject();
        if (clonedSuperObject != this)
            clonedSuperObject = clonedSuperObject.cloneValue(); // スーパークラスのオブジェクトもクローンする

        return new VMObject(this.owner, this.objectType, clonedSuperObject, clonedFields, this.isInitialised);
    }

    public int insideHashCode()
    {
        int hash = 7;
        for (Map.Entry<VMField, VMValue> entry : this.fields.entrySet())
        {
            VMField field = entry.getKey();
            VMValue value = entry.getValue();
            hash = 31 * hash + (field == null ? 0 : field.getName().hashCode());
            hash = 31 * hash + (value == null ? 0 : value.hashCode());
        }
        return hash;
    }

    @Override
    public @NotNull String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(this.objectType.getReference().getFullQualifiedName()).append(" {");
        for (Map.Entry<VMField, VMValue> entry : this.fields.entrySet())
            sb.append("\n  ")
                .append(entry.getKey().getName())
                .append(": ")
                .append(entry.getValue() == null ? "null" : entry.getValue().type().toString());
        sb.append("\n}");
        return sb.toString();
    }
}
