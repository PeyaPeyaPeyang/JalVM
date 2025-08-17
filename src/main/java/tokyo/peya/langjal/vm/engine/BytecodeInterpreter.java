package tokyo.peya.langjal.vm.engine;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMInterpreter;
import tokyo.peya.langjal.vm.engine.threads.VMThread;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

import java.util.Scanner;

public class BytecodeInterpreter implements VMInterpreter
{
    private final Scanner scanner = new Scanner(System.in);
    private final JalVM vm;
    private final VMThread engine;
    private final VMFrame frame;

    private final boolean isDebugging;
    private final MethodNode method;
    private final boolean stepIn;
    private AbstractInsnNode current;

    public BytecodeInterpreter(@NotNull JalVM vm, @NotNull VMThread engine, @NotNull VMFrame frame,
                               @NotNull MethodNode method, boolean debugging)
    {
        this.vm = vm;
        this.engine = engine;
        this.frame = frame;
        this.isDebugging = debugging;
        this.method = method;

        if (method.instructions != null)
            this.current = method.instructions.getFirst();

        this.stepIn = this.isDebugging;
    }

    @Override
    public boolean hasNextInstruction()
    {
        return !(this.current == null || this.current.getNext() == null);
    }

    @Override
    public AbstractInsnNode feedNextInstruction()
    {
        if (!this.hasNextInstruction())
            throw new VMPanic("No next instruction available.");

        if (this.current == null)
            return null;

        while (this.current != null && this.current.getOpcode() == -1)
            this.current = this.current.getNext();

        AbstractInsnNode instruction = this.current;
        this.current = this.current.getNext();
        return instruction;
    }

    private static void out(String message, Object... args)
    {
        System.out.printf((message) + "%n", args);
    }
}
