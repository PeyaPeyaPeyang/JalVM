package tokyo.peya.langjal.vm;

import org.objectweb.asm.tree.AbstractInsnNode;

public interface VMInterpreter {
    boolean hasNextInstruction();

    AbstractInsnNode feedNextInstruction();
}
