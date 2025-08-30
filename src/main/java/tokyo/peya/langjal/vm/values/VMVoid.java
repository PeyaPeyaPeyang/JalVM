package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

import java.util.HashMap;

public class VMVoid implements VMPrimitive
{
    public static final HashMap<JalVM, VMVoid> INSTANCES = new HashMap<>();

    private final JalVM vm;

    private VMVoid(@NotNull JalVM vm)
    {
        this.vm = vm;
    }

    public static VMVoid instance(@NotNull JalVM vm)
    {
        return INSTANCES.computeIfAbsent(vm, VMVoid::new);
    }

    @Override
    public @NotNull VMType<?> type()
    {
        return VMType.of(this.vm, PrimitiveTypes.VOID);
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other)
    {
        return other instanceof VMVoid;
    }

    @Override
    public @NotNull VMVoid cloneValue()
    {
        return this;
    }

    @Override
    public @NotNull String toString()
    {
        return "VOID";
    }

    @Override
    public @NotNull Number asNumber()
    {
        throw new VMPanic("Cannot convert VMVoid to Number");
    }
}
