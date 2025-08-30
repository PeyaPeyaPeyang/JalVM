package tokyo.peya.langjal.vm;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;
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
import tokyo.peya.langjal.vm.api.events.VMDefineClassEvent;
import tokyo.peya.langjal.vm.api.events.VMFrameInEvent;
import tokyo.peya.langjal.vm.api.events.VMFrameOutEvent;
import tokyo.peya.langjal.vm.api.events.VMStepInEvent;
import tokyo.peya.langjal.vm.api.events.VMThreadDeathEvent;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMEngine;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.tracing.FrameManipulationType;
import tokyo.peya.langjal.vm.tracing.FrameTracingEntry;
import tokyo.peya.langjal.vm.tracing.ThreadManipulationType;
import tokyo.peya.langjal.vm.tracing.ThreadTracingEntry;
import tokyo.peya.langjal.vm.tracing.VMFrameTracer;
import tokyo.peya.langjal.vm.tracing.VMThreadTracer;
import tokyo.peya.langjal.vm.tracing.VMValueTracer;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;

import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

public class DebugMain
{
    public static void main(String[] args)
    {
        JalVM jalVM = new JalVM();

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
        parrotInput(methodNode);
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
        jalVM.executeMain(clazz, new String[]{});
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

    private static class EventListeners implements VMListener
    {
        private static final Scanner scanner = new Scanner(System.in);

        private boolean stepIn = false;

        private void printFrame(VMFrame frame, VMEngine engine)
        {
            System.out.printf("Current frame: %s%n", frame.toString());
            System.out.printf("Current thread: %s%n", engine.getCurrentThread().getName());
            System.out.printf("Current method: %s%n", frame.getMethod().getMethodNode().name);
            System.out.printf("Stack: %s%n", frame.getStack());
            System.out.printf("Locals: %s%n", frame.getLocals());
        }

        private String getInstructionText(AbstractInsnNode insn)
        {
            Printer printer = new Textifier();
            TraceMethodVisitor tmv = new TraceMethodVisitor(printer);
            insn.accept(tmv);
            String instructionText = printer.getText().toString();
            // 末尾の \n を削除
            return instructionText.endsWith("\n]") ? instructionText.substring(
                    1,
                    instructionText.length() - 2
            ): instructionText;
        }

        private void debugOptions(VMStepInEvent event)
        {
            String instructionText = this.getInstructionText(event.getInstruction());
            System.out.println("STEP: " + instructionText);
            while (true)
            {
                String input = scanner.nextLine();
                String[] parts = input.split(" ");
                if (parts.length == 0)
                {
                    System.out.println("No command entered. Please try again.");
                    continue;
                }
                String command = parts[0].toLowerCase();
                switch (command)
                {
                    case "show", "z" ->
                    {
                        this.printFrame(event.getFrame(), event.getFrame().getVm().getEngine());
                    }
                    case "next", "x" ->
                    {
                        return;
                    }
                    case "quit", "q" ->
                    {
                        System.out.println("Exiting debugger.");
                        this.stepIn = false;
                        return; // Exit the debugger
                    }
                    default ->
                    {
                        System.out.println("Unknown command: " + command);
                        System.out.println("Commands: ");
                        System.out.println("  show (z) - Show current frame information");
                        System.out.println("  next (x) - Continue to the next instruction");
                        System.out.println("  quit (q) - Exit the debugger");
                    }
                }
            }
        }

        @VMEventHandler
        public void onStepIn(@NotNull VMStepInEvent event)
        {
            System.out.println("Executing instruction: " + EOpcodes.getName(event.getInstruction().getOpcode()) + " in frame: " + event.getFrame());
            if (!this.stepIn)
                return; // Debugging is disabled

            this.debugOptions(event);
        }

        // @VMEventHandler
        public void onThreadDestroy(@NotNull VMThreadDeathEvent event)
        {
            VMEngine engine = event.getVm().getEngine();
            System.out.printf("Thread %s has terminated.%n", event.getThread().getName());
            VMThreadTracer threadTracer = engine.getTracer();
            List<ThreadTracingEntry> history = threadTracer.getHistory(event.getThread());

            System.out.printf("TRACED THREAD MANIPULATIONS: %d%n", history.size());
            System.out.printf("--- BEGIN OF THREAD MANIPULATION HISTORIES ---%n");
            for (int i = 0; i < history.size(); i++)
            {
                ThreadTracingEntry entry = history.get(i);
                VMThread thread = entry.thread();
                System.out.printf("[t%d] %s: %s%n", i, thread.getName(), entry.type().name());
                if (entry.type() == ThreadManipulationType.CREATION)
                    this.dumpThreadHistory(thread);
            }

            System.out.println("--- END OF THREAD MANIPULATION HISTORIES ---");
        }
        private void dumpThreadHistory(@NotNull VMThread thread)
        {
            VMFrameTracer frameTracer = thread.getTracer();
            List<FrameTracingEntry> frames = frameTracer.getHistory();

            System.out.printf("  TRACED FRAMES: %d%n", frames.size());

            for (int i = 0; i < frames.size(); i++)
            {
                FrameTracingEntry entry = frames.get(i);
                VMFrame frame = entry.frame();

                System.out.printf("  [f%d] %s: %s%n",
                                  i, frame.getMethod(), entry.type().name());

                if (entry.type() == FrameManipulationType.FRAME_OUT)
                    dumpValueHistory(frame);
            }
        }

        private void dumpValueHistory(@NotNull VMFrame frame)
        {
            VMValueTracer frameTracer = frame.getTracer();
            List<ValueTracingEntry> history = frameTracer.getHistory();

            System.out.printf("    TRACED MANIPULATIONS: %d%n", history.size());

            for (int i = 0; i < history.size(); i++)
            {
                ValueTracingEntry entry = history.get(i);
                String value = safeValue(entry.value());
                String comb1 = safeValue(entry.combinationValue());
                String comb2 = safeValue(entry.combinationValue2());
                String instr = safeInstr(entry.manipulatingInstruction());

                switch (entry.type())
                {
                    case GENERATION ->
                            print(i, "GENERATION", value, instr);

                    case MANIPULATION ->
                            print(i, "MANIPULATION", "%s -> %s".formatted(value, comb1), instr);

                    case DESTRUCTION ->
                            print(i, "DESTRUCTION", value, instr);

                    case FIELD_GET ->
                            print(i, "FIELD_GET", value, instr);

                    case FIELD_SET ->
                            print(i, "FIELD_SET", value, instr);

                    case PASSING_AS_ARGUMENT ->
                            print(i, "PASSING_AS_ARGUMENT", value, instr);

                    case RETURNING_FROM ->
                            print(i, "RETURNING_FROM", value, instr);

                    case COMBINATION ->
                            print(i, "COMBINATION",
                                  "%s + %s -> %s".formatted(comb1, comb2, value), instr);

                    case FROM_LOCAL ->
                            print(i, "FROM_LOCAL", "%s -> local".formatted(value), instr);

                    case TO_LOCAL ->
                            print(i, "TO_LOCAL", "local -> %s".formatted(value), instr);
                }
            }
        }

        private String safeValue(Object v)
        {
            return v == null ? "null" : v.toString().replace("\n", "\n    ");
        }

        private String safeInstr(AbstractInsnNode instr)
        {
            return instr == null ? "unknown instruction" : getInstructionText(instr);
        }

        private void print(int index, String type, String value, String instr)
        {
            System.out.printf("    [v%d] %s: %s, by %s%n", index, type, value, instr);
        }

        // @VMEventHandler
        public void onFrameIn(@NotNull VMFrameInEvent e)
        {
            System.out.printf("--[FRAME  IN]->: %s%n", e.getFrame().toString());
        }
        // @VMEventHandler
        public void onFrameOut(@NotNull VMFrameOutEvent e)
        {
            System.out.printf("<-[FRAME OUT]--: %s%n", e.getFrame().toString());
        }
        @VMEventHandler
        public void onClassDefine(@NotNull VMDefineClassEvent e)
        {
            System.out.printf("Defining class: %s%n", e.getReference().getFullQualifiedName());
        }
    }
}
