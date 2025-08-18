package tokyo.peya.langjal.vm;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;

public interface VMInterpreter
{
    boolean hasNextInstruction();

    AbstractInsnNode feedNextInstruction();

    void stepForward();
    void stepBackward();
    void setCurrent(int instructionIndex);
    int getCurrentInstructionIndex();
    int getLabelInstructionIndex(@NotNull Label label);
}
