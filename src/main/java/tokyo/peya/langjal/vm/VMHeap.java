package tokyo.peya.langjal.vm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.metaobjects.VMStringObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VMHeap
{
    private final List<VMClass> loadedClasses;
    private final HashMap<String, VMType<?>> typePool;
    private final HashMap<String, VMStringObject> stringPool;

    public VMHeap()
    {
        this.loadedClasses = new ArrayList<>();
        this.typePool = new HashMap<>();
        this.stringPool = new HashMap<>();
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

    public VMStringObject internString(@NotNull VMStringObject value)
    {
        String valueStr = value.getString();
        if (this.stringPool.containsKey(valueStr))
            return this.stringPool.get(valueStr);

        this.stringPool.put(valueStr, value);
        return value;
    }
}
