package tokyo.peya.langjal.vm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
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
        methodNode.visitInsn(Opcodes.RETURN); // Return instruction
        methodNode.visitMaxs(0, 0); // Max stack and local variables
        methodNode.visitEnd();
        classNode.methods.add(methodNode);

        VMClass clazz = jalVM.getClassLoader().defineClass(classNode);

        jalVM.executeMain(clazz, new String[]{});
    }
}
