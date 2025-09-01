package tokyo.peya.langjal.vm.engine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;

public interface VMInterpreter
{
    boolean hasNextInstruction();

    AbstractInsnNode feedNextInstruction();
    AbstractInsnNode getCurrentInstruction();

    void stepForward();
    void stepBackward();
    void setCurrent(int instructionIndex);
    int getCurrentInstructionIndex();
    int getLabelInstructionIndex(@NotNull Label label);

    @Nullable
    Label getInstructionLabel(int instructionIndex);
    int getLineNumberOf(int instructionIndex);

    @Nullable
    ExceptionHandlerDirective getExceptionHandlerFor(int instructionIndex, @NotNull VMClass exceptionClass);
}
