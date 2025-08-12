package tokyo.peya.langjal.vm.engine.members;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.MethodDescriptor;
import tokyo.peya.langjal.vm.DebugInterpreter;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMClassLoader;
import tokyo.peya.langjal.vm.VMInterpreter;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.threads.VMThread;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.values.VMType;

import java.util.Arrays;

@Getter
public class VMMethod {
    private final VMClass clazz;
    private final MethodNode methodNode;

    private final VMType returnType;
    private final VMType[] parameterTypes;

    public VMMethod(@NotNull VMClass clazz, @NotNull MethodNode methodNode) {
        this.clazz = clazz;
        this.methodNode = methodNode;

        MethodDescriptor descriptor = MethodDescriptor.parse(methodNode.desc);
        this.returnType = new VMType(descriptor.getReturnType());
        this.parameterTypes = Arrays.stream(descriptor.getParameterTypes())
                .map(VMType::new)
                .toArray(VMType[]::new);
    }

    public VMInterpreter createInterpreter(
            @NotNull JalVM vm,
            @NotNull VMThread engine,
            @NotNull VMFrame frame) {
        return new DebugInterpreter(
                vm,
                engine,
                frame
        );
    }

    public void linkTypes(@NotNull VMClassLoader cl) {
        this.returnType.linkClass(cl);
        for (VMType type : this.parameterTypes) {
            type.linkClass(cl);
        }
    }

    public int getMaxStackSize() {
        return this.methodNode.maxStack;
    }

    public int getMaxLocals() {
        return this.methodNode.maxLocals;
    }
}
