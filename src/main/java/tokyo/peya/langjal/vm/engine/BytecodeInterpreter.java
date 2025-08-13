package tokyo.peya.langjal.vm.engine;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.vm.VMInterpreter;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

public class BytecodeInterpreter implements VMInterpreter
{
    private final MethodNode method;
    private AbstractInsnNode current;

    public BytecodeInterpreter(@NotNull MethodNode method) {
        this.method = method;

        if (method.instructions != null)
            this.current = method.instructions.getFirst();;
    }

    @Override
    public boolean hasNextInstruction() {
        return  !(current == null || current.getNext() == null);
    }

    @Override
    public AbstractInsnNode feedNextInstruction() {
        if (!this.hasNextInstruction()) {
            throw new VMPanic("No next instruction available.");
        }
        AbstractInsnNode next;
        while ((next = current.getNext()) != null) {
            if (next.getOpcode() != -1) { // ラベルやフレームではない場合
                break;
            }

            current = next; // ラベルやフレームをスキップする
        }

        return next;
    }
}
