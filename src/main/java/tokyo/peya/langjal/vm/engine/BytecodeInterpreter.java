package tokyo.peya.langjal.vm.engine;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMInterpreter;
import tokyo.peya.langjal.vm.engine.threads.VMThread;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

import java.util.Scanner;

public class BytecodeInterpreter implements VMInterpreter {
    private final Scanner scanner = new Scanner(System.in);
    private final JalVM vm;
    private final VMThread engine;
    private final VMFrame frame;


    private final boolean isDebugging;
    private boolean stepIn;
    private final MethodNode method;
    private AbstractInsnNode current;

    public BytecodeInterpreter(@NotNull JalVM vm, @NotNull VMThread engine, @NotNull VMFrame frame,
                               @NotNull MethodNode method, boolean debugging) {
        this.vm = vm;
        this.engine = engine;
        this.frame = frame;
        this.isDebugging = debugging;
        this.method = method;

        if (method.instructions != null)
            this.current = method.instructions.getFirst();

        this.stepIn = isDebugging;
    }

    private void printFrame() {
        out("Current frame: %s", this.frame);
        out("Current thread: %s", this.engine.getName());
        out("Current method: %s", this.frame.getMethod().getMethodNode().name);
        out("Stack: %s", this.frame.getStack());
        out("Locals: %s", this.frame.getLocals());
    }

    private void debugOptions() {
        Printer printer = new Textifier();
        TraceMethodVisitor tmv = new TraceMethodVisitor(printer);
        this.current.accept(tmv);
        String instructionText = printer.getText().toString();
        // 末尾の \n を削除
        instructionText = instructionText.endsWith("\n]") ? instructionText.substring(1, instructionText.length() - 2) : instructionText;

        System.out.println("STEP: " + instructionText);
        while(true) {
            String input = this.scanner.nextLine();
            String[] parts = input.split(" ");
            if (parts.length == 0) {
                System.out.println("No command entered. Please try again.");
                continue;
            }
            String command = parts[0].toLowerCase();
            switch (command) {
                case "show", "z" -> {
                    this.printFrame();
                }
                case "next", "x" -> {
                    return;
                }
                case "quit", "q" -> {
                    System.out.println("Exiting debugger.");
                    this.stepIn = false;
                    return; // Exit the debugger
                }
                default -> {
                    System.out.println("Unknown command: " + command);
                    System.out.println("Commands: ");
                    System.out.println("  show (z) - Show current frame information");
                    System.out.println("  next (x) - Continue to the next instruction");
                    System.out.println("  quit (q) - Exit the debugger");
                }
            }
        }
    }

    @Override
    public boolean hasNextInstruction() {
        boolean hasNext = !(current == null || current.getNext() == null);
        if (!hasNext) {
            System.out.println("No more instructions to execute! J(al)VM will return to the previous frame, printing current frame information...");
            this.printFrame();
        }

        return hasNext;
    }

    @Override
    public AbstractInsnNode feedNextInstruction() {
        if (!this.hasNextInstruction())
            throw new VMPanic("No next instruction available.");

        if (this.current == null)
            return null;

        while (current != null && current.getOpcode() == -1)
            current = current.getNext();

        if (this.stepIn)
            this.debugOptions();

        AbstractInsnNode instruction = this.current;
        this.current = this.current.getNext();
        return instruction;
    }

    private static void out(String message, Object... args) {
        System.out.printf((message) + "%n", args);
    }
}
