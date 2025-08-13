package tokyo.peya.langjal.vm;

import com.ibm.icu.text.RbnfLenientScanner;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.api.VMEventHandler;
import tokyo.peya.langjal.vm.api.VMListener;
import tokyo.peya.langjal.vm.api.events.StepInEvent;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMEngine;
import tokyo.peya.langjal.vm.engine.VMFrame;

import java.util.Scanner;

public class DebugMain {
    public static void main(String[] args) {
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
        helloWorld(methodNode);
        // comparisons(methodNode);
        methodNode.visitInsn(Opcodes.RETURN); // Return instruction
        methodNode.visitMaxs(-1, -1); // Max stack and local variables
        methodNode.visitEnd();
        classNode.methods.add(methodNode);

        VMClass clazz = jalVM.getClassLoader().defineClass(classNode);
        System.out.println(jalVM.getHeap().getLoadedClasses().size());

        jalVM.executeMain(clazz, new String[]{});
    }

    private static void comparisons(MethodNode node) {
        node.visitIntInsn(EOpcodes.SIPUSH, 20);
        node.visitIntInsn(EOpcodes.SIPUSH, 30);
        node.visitInsn(EOpcodes.IADD);
        node.visitIntInsn(EOpcodes.SIPUSH, 50);
        node.visitInsn(EOpcodes.ISUB);
    }

    private static void helloWorld(MethodNode node) {
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

    private static class EventListeners implements VMListener {
        private static final Scanner scanner = new Scanner(System.in);

        private boolean stepIn = true;

        private void printFrame(VMFrame frame, VMEngine engine) {
            System.out.printf("Current frame: %s%n", frame.toString());
            System.out.printf("Current thread: %s%n", engine.getCurrentThread().getName());
            System.out.printf("Current method: %s%n", frame.getMethod().getMethodNode().name);
            System.out.printf("Stack: %s%n", frame.getStack());
            System.out.printf("Locals: %s%n", frame.getLocals());
        }

        private void debugOptions(StepInEvent event) {
            Printer printer = new Textifier();
            TraceMethodVisitor tmv = new TraceMethodVisitor(printer);
            event.getInstruction().accept(tmv);
            String instructionText = printer.getText().toString();
            // 末尾の \n を削除
            instructionText = instructionText.endsWith("\n]") ? instructionText.substring(1, instructionText.length() - 2) : instructionText;

            System.out.println("STEP: " + instructionText);
            while(true) {
                String input = scanner.nextLine();
                String[] parts = input.split(" ");
                if (parts.length == 0) {
                    System.out.println("No command entered. Please try again.");
                    continue;
                }
                String command = parts[0].toLowerCase();
                switch (command) {
                    case "show", "z" -> {
                        this.printFrame(event.getFrame(), event.getFrame().getVm().getEngine());
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

        @VMEventHandler
        public void onStepIn(@NotNull StepInEvent event) {
            if (!this.stepIn) {
                return; // Debugging is disabled
            }

            this.debugOptions(event);
        }
    }
}
