package tokyo.peya.langjal.vm.engine;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.vm.VMClassLoader;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMType;

@Getter
public class VMClass {
    private final ClassNode clazz;
    private final VMMethod[] methods;
    private final VMField[] fields;

    public VMClass(@NotNull ClassNode clazz) {
        this.clazz = clazz;
        this.methods = extractMethods(clazz);
        this.fields = extractFields(clazz);
    }

    public void linkMembers(@NotNull VMClassLoader cl) {
        for (VMMethod method : methods)
            method.linkTypes(cl);
        for (VMField field : fields)
            field.linkType(cl);
    }

    private VMField[] extractFields(@NotNull ClassNode classNode) {
        FieldNode[] fields = classNode.fields.toArray(new FieldNode[0]);
        VMField[] vmFields = new VMField[fields.length];
        for (int i = 0; i < fields.length; i++) {
            FieldNode fieldNode = fields[i];
            String descString = fieldNode.desc;
            vmFields[i] = new VMField(
                    this,
                    new VMType(descString),
                    fieldNode
            );
        }

        return vmFields;
    }

    private VMMethod[] extractMethods(@NotNull ClassNode classNode) {
        return classNode.methods.stream()
                .map(methodNode -> new VMMethod(this, methodNode))
                .toArray(VMMethod[]::new);
    }

    public VMMethod findMainMethod() {
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
