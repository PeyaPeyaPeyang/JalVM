package tokyo.peya.langjal.vm;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.AbstractInsnNode;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.VMThread;

import java.util.Scanner;

public class DebugInterpreter implements VMInterpreter {
    private final JalVM vm;
    private final VMThread engine;
    private final VMFrame frame;

    private final Scanner scanner;

    private boolean isRunning = true;

    public DebugInterpreter(@NotNull JalVM vm, @NotNull VMThread engine, @NotNull VMFrame frame) {
        this.vm = vm;
        this.engine = engine;
        this.frame = frame;

        this.scanner = new Scanner(System.in);
    }

    private static void out(String message, Object... args) {
        System.out.printf(message, args);
    }

    @Override
    public boolean hasNextInstruction() {
        return this.isRunning;
    }

    @Override
    public AbstractInsnNode feedNextInstruction() {
        if (!this.isRunning) {
            throw new IllegalStateException("Interpreter is not running.");
        }

        String input = this.okNext();
        if (input.equalsIgnoreCase("exit")) {
            this.isRunning = false;
            System.out.println("Bye!");
            return null;
        }

        return null;
    }

    private String okNext() {
        System.out.print("OK >");
        return this.scanner.nextLine();
    }
}
