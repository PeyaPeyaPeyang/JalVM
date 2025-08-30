package tokyo.peya.langjal.vm.engine.stacking.instructions.references;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.MethodDescriptor;
import tokyo.peya.langjal.compiler.jvm.TypeDescriptor;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMReferenceValue;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

public class InvocationHelper
{
    public static InvocationContext retrieveCtxt(@NotNull String owner,
                                                 @NotNull MethodDescriptor descriptor,
                                                 @NotNull VMFrame frame)
    {
        TypeDescriptor[] argumentTypeDescriptors = descriptor.getParameterTypes();
        VMValue[] arguments = new VMValue[argumentTypeDescriptors.length];
        VMType<?>[] argumentTypes = new VMType[argumentTypeDescriptors.length];
        for (int i = arguments.length - 1; i >= 0; i--)  // スタックの順序は逆なので、最後からポップする
        {
            argumentTypes[i] = VMType.of(frame, argumentTypeDescriptors[i]);
            arguments[i] = frame.getStack().popType(argumentTypes[i]);
        }

        return new InvocationContext(
                arguments,
                argumentTypes,
                findOwner(frame.getVM(), owner)
        );
    }

    private static VMType<? extends VMReferenceValue> findOwner(@NotNull JalVM vm, @NotNull String owner)
    {
        if (owner.startsWith("["))
            return VMType.ofGenericArray(vm.getHeap());

        return vm.getClassLoader().findClass(ClassReference.of(owner));
    }

    public record InvocationContext(
            @NotNull
            VMValue[] arguments,
            @NotNull
            VMType<?>[] argumentTypes,
            @NotNull
            VMType<? extends VMReferenceValue> ownerType
    ) {}
}
