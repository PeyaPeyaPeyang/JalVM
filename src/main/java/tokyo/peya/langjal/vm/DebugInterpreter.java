package tokyo.peya.langjal.vm;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.threads.VMThread;

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
        String[] parts = input.split(" ");
        if (parts.length == 0)
            return null;

        String insn = parts[0];
        switch (insn) {
            case "exit" -> {
                this.isRunning = false;
                out("Bye!");
                return null;
            }
            case "show" -> {
                out("Current frame: %s%n", this.frame);
                out("Current thread: %s%n", this.engine.getName());
                out("Current method: %s%n", this.frame.getMethod().getMethodNode().name);
                out("Stack: %s%n", this.frame.getStack());
                out("Locals: %s%n", this.frame.getLocals());
                return null;
            }
            case "sipush" -> {
                if (!checkArgs(parts, 2)) {
                    return null;
                }
                int value = asInt(parts[1]);
                return new IntInsnNode(EOpcodes.SIPUSH, value);
            }
            case "iadd" -> {
                return new InsnNode(EOpcodes.IADD);
            }

            default -> {
                out("Unknown instruction: ", input);
                return null;
            }
        }
    }

    private boolean checkArgs(String[] parts, int expected, int minExpected) {
        if (parts.length < minExpected) {
            out("Usage: %s%n", parts[0]);
            return false;
        }
        if (parts.length > expected) {
            out("Too many arguments for %s%n", parts[0]);
            return false;
        }
        return true;
    }

    private boolean checkArgs(String[] parts, int expected) {
        return checkArgs(parts, expected, expected);
    }

    private int asInt(@NotNull String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            out("Invalid integer: %s%n", value);
            return 0; // or throw an exception
        }
    }

    private String okNext() {
        System.out.print(this.frame.getThread().getName() + " OK >");
        return this.scanner.nextLine();
    }
}
