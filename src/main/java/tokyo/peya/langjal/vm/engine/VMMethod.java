package tokyo.peya.langjal.vm.engine;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.vm.DebugInterpreter;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMInterpreter;

@Getter
public class VMMethod {
    private final VMClass clazz;
    private final MethodNode methodNode;
    public VMMethod(@NotNull VMClass clazz, @NotNull MethodNode methodNode) {
        this.clazz = clazz;
        this.methodNode = methodNode;
    }

    public VMInterpreter createInterpreter(
            @NotNull JalVM vm,
            @NotNull VMEngine engine,
            @NotNull Frame frame)
    {
        return new DebugInterpreter(
                vm,
                engine,
                frame
        );
    }
}
