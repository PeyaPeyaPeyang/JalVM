package tokyo.peya.langjal.vm;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.api.VMEventHandler;
import tokyo.peya.langjal.vm.api.VMListener;
import tokyo.peya.langjal.vm.api.events.VMFrameInEvent;
import tokyo.peya.langjal.vm.api.events.VMFrameOutEvent;
import tokyo.peya.langjal.vm.api.events.VMStartupEvent;
import tokyo.peya.langjal.vm.api.events.VMStepInEvent;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMEngine;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.references.ClassReference;

import java.util.Arrays;
import java.util.Scanner;
import java.util.stream.Collectors;

public class DebugMain
{
    public static void main(String[] args)
    {
        JalVM jalVM = new JalVM(VMConfiguration.builder(ClassReference.of("tokyo/peya/langjal/vm/TestClass"))
                                               .enableAssertions(true)
                                               .debugVM(true)
                                               .build()
        );

        jalVM.getEventManager().registerListener(new EventListeners());

        ClassNode classNode = new ClassNode();
        classNode.visit(
                Opcodes.V11,
                0, // Class access flags
                "tokyo/peya/langjal/vm/TestClass", // Class name
                null, // Signature
                "java/lang/Object", // Super class
                null // Interfaces
        );

        classNode.visitSource("TestClass.java", null);

        MethodNode methodNode = new MethodNode(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, // Access flags
                "main", // Method name
                "([Ljava/lang/String;)V", // Method descriptor
                null, // Signature
                null // Exceptions
        );
        methodNode.visitCode();
        // System.out.println("Hello, World!");
        // helloWorld(methodNode);
        helloWorld(methodNode);
        // a(methodNode);
        // comparisons(methodNode);
        // getProp(methodNode);
        methodNode.visitInsn(Opcodes.RETURN); // Return instruction
        methodNode.visitMaxs(-1, -1); // Max stack and local variables
        methodNode.visitEnd();
        classNode.methods.add(methodNode);
        VMClass clazz = jalVM.getClassLoader().defineClass(classNode);
/*
        System.out.println(jalVM.getHeap().getLoadedClasses().size());
*/
        jalVM.executeEntrypointInClass(clazz, new String[]{});
    }

    private static void comparisons(MethodNode node)
    {
        node.visitIntInsn(EOpcodes.SIPUSH, 20);
        node.visitIntInsn(EOpcodes.SIPUSH, 30);
        node.visitInsn(EOpcodes.IADD);
        node.visitIntInsn(EOpcodes.SIPUSH, 50);
        node.visitInsn(EOpcodes.ISUB);
    }

    public static void a(MethodNode node) {
        // Pattern.compile("test")
        // node.visitTypeInsn(Opcodes.NEW, "java/util/regex/Pattern");
        // node.visitInsn(Opcodes.DUP);
        node.visitLdcInsn("test"); // パターン文字列
        node.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/util/regex/Pattern",
                "compile",
                "(Ljava/lang/String;)Ljava/util/regex/Pattern;",
                false // Is interface
        );
    }

    private static void parrotInput(MethodNode node)
    {
        // new java.util.Scanner(System.in)
        node.visitTypeInsn(Opcodes.NEW, "java/util/Scanner");
        node.visitInsn(Opcodes.DUP);
        node.visitFieldInsn(
                Opcodes.GETSTATIC,
                "java/lang/System",
                "in",
                "Ljava/io/InputStream;"
        );
        node.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/util/Scanner",
                "<init>",
                "(Ljava/io/InputStream;)V",
                false
        );

        // ローカル変数1に Scanner を保存
        node.visitVarInsn(Opcodes.ASTORE, 1);

        // System.out
        node.visitFieldInsn(
                Opcodes.GETSTATIC,
                "java/lang/System",
                "out",
                "Ljava/io/PrintStream;"
        );

        // scanner.nextLine()
        node.visitVarInsn(Opcodes.ALOAD, 1);
        node.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/util/Scanner",
                "nextLine",
                "()Ljava/lang/String;",
                false
        );

        // PrintStream.println(String)
        node.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/io/PrintStream",
                "println",
                "(Ljava/lang/String;)V",
                false
        );
    }
    private static void helloWorld(MethodNode node)
    {
        node.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        node.visitLdcInsn("Hello, World!");
        node.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/io/PrintStream",
                "println",
                "(Ljava/lang/String;)V",
                false // Is interface
        );
    }

    private static void getProp(MethodNode node)
    {
        node.visitLdcInsn("java.home");
        node.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/System",
                "getProperty",
                "(Ljava/lang/String;)Ljava/lang/String;",
                false // Is interface
        );
    }
    private static class EventListeners implements VMListener {

        private static final Scanner scanner = new Scanner(System.in);

        private final StepController step = new StepController();

        private boolean quiet = true;
        private int currentDepth = 0;

        // =========================
        // STARTUP
        // =========================
        @VMEventHandler
        public void onStartup(@NotNull VMStartupEvent event) {
            this.quiet = false;
            this.currentDepth = 0;
            this.step.reset();
        }

        // =========================
        // STEP EVENT
        // =========================
        @VMEventHandler
        public void onStepIn(@NotNull VMStepInEvent event) {

            if (!this.step.shouldStop(event)) {
                return;
            }

            if (!this.quiet) {
                printStep(event);
            }

            debugLoop(event);
        }

        private void printStep(VMStepInEvent event) {
            System.out.printf(
                    """
                    
                    ▶ STEP (depth=%d)
                      opcode : %s
                      insn   : %s
                      frame  : %s
                    """,
                    this.currentDepth,
                    EOpcodes.getName(event.getInstruction().getOpcode()),
                    getInstructionText(event.getInstruction()),
                    event.getFrame()
            );
        }

        // =========================
        // DEBUG CONSOLE
        // =========================
        private void debugLoop(VMStepInEvent event) {
            if (this.quiet) {
                return;
            }

            while (true) {
                System.out.print("debug> ");
                String input = scanner.nextLine().trim();

                if (input.isEmpty()) continue;

                String[] parts = input.split("\\s+");
                String cmd = parts[0].toLowerCase();

                switch (cmd) {

                    case "h", "?", "help" -> printHelp();

                    case "x", "show" ->
                            printFrame(event.getFrame(), event.getFrame().getVM().getEngine());

                    case "s", "step" -> {
                        this.step.stepIn();
                        return;
                    }

                    case "c", "next" -> {
                        this.step.stepOver(event.getFrame());
                        return;
                    }

                    case "z", "out" -> {
                        this.step.stepOut(event.getFrame());
                        return;
                    }

                    case "q", "continue" -> {
                        if (parts.length >= 2) {
                            try {
                                this.step.cont(Integer.parseInt(parts[1]));
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid step count: " + parts[1]);
                                continue;
                            }
                        } else {
                            this.step.contInfinite();
                        }
                        return;
                    }

                    default -> {
                        System.out.println("Unknown command: " + cmd);
                    }
                }
            }
        }

        private void printHelp() {
            System.out.println("""
        === Debug Commands ===

        h, ?, help   : Show this help message
        x, show      : Show current frame snapshot
        s, step       : Step into the next instruction
        c, next       : Step over (execute current instruction and stop at the next one)
        z, out        : Step out (run until the current method returns)
        q, continue [n] : Continue execution for n steps (or indefinitely if n is not provided)

        ======================
        """);
        }

        // =========================
        // FRAME EVENTS
        // =========================
        @VMEventHandler
        public void onFrameIn(@NotNull VMFrameInEvent e) {

            if (!this.quiet)
                System.out.printf("→ FRAME IN  : %s%n", e.getFrame());
        }

        @VMEventHandler
        public void onFrameOut(@NotNull VMFrameOutEvent e) {
            this.step.notifyFrameOut(e.getFrame());

            if (!this.quiet)
                System.out.printf("← FRAME OUT : %s%n", e.getFrame());
        }

        // =========================
        // FRAME SNAPSHOT
        // =========================
        private void printFrame(VMFrame frame, VMEngine engine) {
            System.out.printf("""
                ===== FRAME SNAPSHOT =====
                Thread : %s
                Method : %s
                Frame  : %s
                
                [Stack]
                %s
                
                [Locals]
                %s
                =========================
                %n""",
                              engine.getCurrentThread().getName(),
                              frame.getMethod().getMethodNode().name,
                              frame,
                              indent(frame.getStack()),
                              indent(frame.getLocals())
            );
        }

        // =========================
        // STEP CONTROLLER
        // =========================
        private static class StepController {

            private Mode mode = Mode.STEP_IN;

            private int remainingSteps = 0;

            private VMFrame targetFrame;
            private boolean forceStop = false;

            enum Mode {
                STEP_IN,
                STEP_OVER,
                STEP_OUT,
                CONTINUE
            }

            void reset() {
                this.mode = Mode.STEP_IN;
                this.remainingSteps = 0;
                this.targetFrame = null;
                this.forceStop = false;
            }

            void stepIn() {
                this.mode = Mode.STEP_IN;
            }

            void stepOver(VMFrame frame) {
                this.mode = Mode.STEP_OVER;
                this.targetFrame = frame;
            }

            void stepOut(VMFrame frame) {
                this.mode = Mode.STEP_OUT;
                this.targetFrame = frame;
            }

            void cont(int n) {
                this.mode = Mode.CONTINUE;
                this.remainingSteps = n;
            }

            void contInfinite() {
                this.mode = Mode.CONTINUE;
                this.remainingSteps = -1;
            }

            void notifyFrameOut(VMFrame frame) {
                if (this.mode == Mode.STEP_OUT && frame == this.targetFrame) {
                    this.forceStop = true;
                }
            }

            boolean shouldStop(VMStepInEvent event) {

                if (this.forceStop) {
                    this.forceStop = false;
                    this.mode = Mode.STEP_IN;
                    return true;
                }

                VMFrame current = event.getFrame();

                return switch (this.mode) {

                    case STEP_IN -> true;

                    case CONTINUE -> this.remainingSteps >= 0 && this.remainingSteps-- <= 0;

                    case STEP_OVER -> current == this.targetFrame;

                    case STEP_OUT -> false;
                };
            }
        }

        // =========================
        // UTIL
        // =========================
        private String indent(Object obj) {
            if (obj == null) return "  (null)";

            return Arrays.stream(obj.toString().split("\n"))
                         .map(s -> "  " + s)
                         .collect(Collectors.joining("\n"));
        }

        private String getInstructionText(AbstractInsnNode insn) {
            Printer printer = new Textifier();
            TraceMethodVisitor tmv = new TraceMethodVisitor(printer);
            insn.accept(tmv);

            String text = printer.getText().toString();

            return text.endsWith("\n]")
                    ? text.substring(1, text.length() - 2)
                    : text.trim();
        }
    }
}
