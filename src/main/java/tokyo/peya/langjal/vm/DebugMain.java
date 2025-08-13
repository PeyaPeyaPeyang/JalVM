package tokyo.peya.langjal.vm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMClass;

public class DebugMain {
    public static void main(String[] args) {
        JalVM jalVM = new JalVM();

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
        comparisons(methodNode);
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
}
