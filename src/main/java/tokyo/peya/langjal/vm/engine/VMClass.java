package tokyo.peya.langjal.vm.engine;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.vm.references.ClassReference;

public class VMClass {
    private final ClassNode clazz;
    private final VMMethod[] methods;

    public VMClass(ClassNode clazz) {
        this.clazz = clazz;
        this.methods = this.extractMethods(clazz);
    }

    private VMMethod[] extractMethods(@NotNull ClassNode classNode) {
        return classNode.methods.stream()
                .map(methodNode -> new VMMethod(this, methodNode))
                .toArray(VMMethod[]::new);
    }

    public VMMethod findMainMethod()
    {
        for (VMMethod method : methods) {
            MethodNode node = method.getMethodNode();
            if (node.name.equals("main")
                && node.desc.equals("([Ljava/lang/String;)V")
                // アクセスフラグ: public static, not native
                && (node.access & Opcodes.ACC_STATIC) != 0
                && (node.access & Opcodes.ACC_PUBLIC) != 0
                && (node.access & Opcodes.ACC_NATIVE) == 0)
                return method;
        }
        return null;
    }

    public ClassReference getReference() {
        return ClassReference.of(clazz);
    }
}
