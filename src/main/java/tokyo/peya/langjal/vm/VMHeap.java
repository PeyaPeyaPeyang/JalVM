package tokyo.peya.langjal.vm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.metaobjects.VMStringObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VMHeap
{
    private final List<VMClass> loadedClasses;
    private final HashMap<String, VMStringObject> stringPool;

    public VMHeap()
    {
        this.loadedClasses = new ArrayList<>();
        this.stringPool = new HashMap<>();
    }

    public void addClass(VMClass vmClass)
    {
        this.loadedClasses.add(vmClass);
    }

    public void removeClass(@NotNull String name)
    {
        this.loadedClasses.removeIf(vmClass -> vmClass.getReference().isEqualClass(name));
    }

    @Nullable
    public VMClass getLoadedClass(@NotNull String className)
    {
        for (VMClass vmClass : this.loadedClasses)
            if (vmClass.getReference().isEqualClass(className))
                return vmClass;

        return null;
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
