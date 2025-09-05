package tokyo.peya.langjal.vm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.metaobjects.VMStringObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class VMHeap
{
    private final List<VMClass> loadedClasses;
    private final HashMap<String, VMType<?>> typePool;
    private final HashMap<String, VMStringObject> stringPool;

    private long staticFieldID = 0xFFFF;  // 静的フィールドIDの初期値を高めに設定しておく

    private final Map<Long, WeakReference<VMField>> staticFieldPool;

    public VMHeap()
    {
        this.loadedClasses = new CopyOnWriteArrayList<>();
        this.typePool = new HashMap<>();
        this.stringPool = new HashMap<>();
        this.staticFieldPool = new HashMap<>();
    }

    public void addClass(@NotNull VMClass vmClass)
    {
        this.loadedClasses.add(vmClass);
    }

    public void removeClass(@NotNull String name)
    {
        this.loadedClasses.removeIf(vmClass -> vmClass.getReference().isEqualClass(name));
    }

    public void addType(@NotNull VMType<?> type)
    {
        this.typePool.put(type.getTypeDescriptor(), type);
    }

    public void removeType(@NotNull String descriptor)
    {
        this.typePool.remove(descriptor);
    }

    public VMType<?> getType(@NotNull String descriptor)
    {
        return this.typePool.get(descriptor);
    }

    @Nullable
    public VMClass getLoadedClass(@NotNull ClassReference className)
    {
        for (VMClass vmClass : this.loadedClasses)
            if (vmClass.getReference().equals(className))
                return vmClass;

        return null;
    }

    public List<VMClass> getLoadedClasses()
    {
        return new ArrayList<>(this.loadedClasses);
    }

    @NotNull
    public VMStringObject internString(@NotNull VMStringObject value)
    {
        String valueStr = value.getString();
        if (this.stringPool.containsKey(valueStr))
            return this.stringPool.get(valueStr);

        this.stringPool.put(valueStr, value);
        return value;
    }

    public long assignStaticFieldID()
    {
        return this.staticFieldID += 16;
    }

    public void recognizeStaticField(@NotNull VMField field)
    {
        if (this.staticFieldPool.containsKey(field.getFieldID()))
            throw new IllegalStateException("Static field ID " + field.getFieldID() + " is already registered!");

        long fieldId = field.getFieldID();
        if (fieldId < 0xFFFF)  // fieldID は原則クラス内で一意かつ0始まり。65536 / 16 個も非性的フィールドを持つクラスは存在しないと考えられるため，初期値65535以下のIDは不正とみなす。
            throw new IllegalStateException("Static field ID must be an ID assigned by VMHeap#assignStaticFieldID(), but got " + fieldId);

        this.staticFieldPool.put(fieldId, new WeakReference<>(field));
    }

    @Nullable
    public VMField getStaticFieldByID(long id)
    {
        WeakReference<VMField> ref = this.staticFieldPool.get(id);
        if (ref == null)
            return null;
        VMField field = ref.get();
        if (field == null)
            this.staticFieldPool.remove(id);
        return field;
    }
}
