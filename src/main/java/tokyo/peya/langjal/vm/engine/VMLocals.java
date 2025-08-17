package tokyo.peya.langjal.vm.engine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.exceptions.NoReferencePanic;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMValue;

import java.util.HashMap;
import java.util.Map;

public class VMLocals
{
    private final VMFrame frame;
    private final int maxSize;
    private final Map<Integer, VMValue> locals;

    public VMLocals(@NotNull VMFrame frame, int maxSize, @NotNull VMValue[] args)
    {
        this.frame = frame;
        this.maxSize = maxSize;
        this.locals = new HashMap<>();

        this.initialiseArgs(args);
    }

    public void initialiseArgs(@NotNull VMValue[] args)
    {
        int slot = 0;
        for (VMValue arg : args)
        {
            this.setSlot(slot++, arg);
            if (arg.isCategory2())
                slot++; // 次のスロットは TOP になるのでスキップ
        }
    }

    public void setSlot(int index, @NotNull VMValue value)
    {
        this.setSlot(index, value, null);
    }

    public void setSlot(int index, @NotNull VMValue value, @Nullable AbstractInsnNode performer)
    {
        if (index < 0 || (this.maxSize > 0 && index >= this.maxSize))
            throw new NoReferencePanic("Local variable index " + index + " is out of bounds. Max size: " + this.maxSize);

        VMMethod method = this.frame.getMethod();
        this.frame.getTracer().pushHistory(
                ValueTracingEntry.localSet(
                        value,
                        method,
                        performer
                )
        );

        this.locals.put(index, value);
    }

    public @NotNull VMValue getLocal(int index)
    {
        return this.getLocal(index, null);
    }

    public @NotNull VMValue getLocal(int index, @Nullable AbstractInsnNode performer)
    {
        if (index < 0 || (this.maxSize > 0 && index >= this.maxSize))
            throw new NoReferencePanic("Local variable index " + index + " is out of bounds. Max size: " + this.maxSize);

        VMValue value = this.locals.get(index);
        if (value == null)
            throw new NoReferencePanic("Local variable at index " + index + " does not exist."
                                               + "May be referencing a 2-category value with its last index?");

        VMMethod method = this.frame.getMethod();
        this.frame.getTracer().pushHistory(
                ValueTracingEntry.localSet(
                        value,
                        method,
                        performer
                )
        );

        return value;
    }

    @Override
    public String toString()
    {
        return "[" + this.locals.entrySet().stream()
                                .map(entry -> entry.getKey() + ": " + entry.getValue().toString())
                                .reduce((a, b) -> a + ", " + b)
                                .orElse("") + "]";
    }

    @NotNull
    public <T extends VMValue> T getType(int i, @NotNull Class<T> vmIntegerClass, @Nullable AbstractInsnNode performer)
    {
        VMValue value = this.getLocal(i, performer);
        if (vmIntegerClass.isInstance(value))
            return vmIntegerClass.cast(value);
        else
            throw new VMPanic("Local variable at index " + i + " is not of type " + vmIntegerClass.getSimpleName());
    }

    public <T extends VMValue> T getType(int i, @NotNull Class<T> vmIntegerClass)
    {
        return this.getType(i, vmIntegerClass, null);
    }
}
