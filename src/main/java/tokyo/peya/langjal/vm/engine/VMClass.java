package tokyo.peya.langjal.vm.engine;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.*;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.members.RestrictedAccessor;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threads.VMThread;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

import java.util.Arrays;

@Getter
public class VMClass implements RestrictedAccessor {
    private final ClassReference reference;
    private final ClassNode clazz;

    private final AccessAttributeSet accessAttributes;
    private final AccessLevel accessLevel;

    private final VMMethod[] methods;
    private final VMField[] fields;

    private VMClass superLink;

    public VMClass(@NotNull ClassNode clazz) {
        this.reference = ClassReference.of(clazz);
        this.clazz = clazz;

        this.accessAttributes = AccessAttributeSet.fromAccess(clazz.access);
        this.accessLevel = AccessLevel.fromAccess(clazz.access);

        this.methods = extractMethods(clazz);
        this.fields = extractFields(clazz);

    }

    public VMObject createInstance() {
        if (this.superLink == null)
            throw new IllegalStateException("Cannot create instance of class without super class link: " + this.reference.getFullQualifiedName());

        return new VMObject(this);
    }


    public boolean isSubclassOf(@NotNull VMClass maySuper) {
        if (this == maySuper)
            return true; // 同じクラスならtrue
        if (this.superLink == null)
            return false; // スーパークラスがない場合はfalse

        // 親をたどりながら探索
        VMClass current = this.superLink;
        while (current != null) {
            if (current == maySuper)
                return true;
            current = current.superLink;
        }

        // 一致せず
        return false;
    }

    public void initialiseClass(@NotNull VMSystemClassLoader cl) {
        // リンク処理 -> クラスのスーパクラスやメンバの参照を解決
        this.linkSuper(cl);
        this.linkMembers(cl);
        // 静的初期化メソッドを呼び出す
        VMThread currentThread = cl.getVm().getEngine().getCurrentThread();
        this.invokeStaticInitaliser(currentThread);
    }

    private void invokeStaticInitaliser(@NotNull VMThread callerThread)
    {
        VMMethod staticInitMethod = this.findSuitableMethod(
                null,
                "<clinit>",
                VMType.VOID
        );
        if (staticInitMethod == null)
            return;  // 静的初期化メソッドがない場合は何もしない

        // 静的初期化メソッドを呼び出す
        staticInitMethod.invokeStatic(callerThread, null);
    }


    private void linkSuper(@NotNull VMSystemClassLoader cl) {
        String superName = clazz.superName;
        if (superName == null || superName.isEmpty())
            superName = "java/lang/Object"; // デフォルトは java.lang.Object

        this.superLink = cl.findClass(ClassReference.of(superName));
    }

    private void linkMembers(@NotNull VMSystemClassLoader cl) {
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
                    VMType.ofTypeDescriptor(descString),
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

    @Nullable
    public VMMethod findConstructor(@Nullable VMClass caller, @NotNull VMType... args) {
        return this.findSuitableMethod(caller, "<init>", VMType.VOID, args);
    }

    public VMMethod findEntryPoint() {
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

    @Nullable
    public VMMethod findSuitableMethod(@Nullable VMClass caller, @NotNull String methodName, @Nullable VMType returnType, @NotNull VMType... args) {
        for (VMMethod method : this.getMethods()) {
            // メソッド名が一致しない場合はスキップ
            if (!method.getName().equals(methodName))
                continue;
            // 戻り値の型が一致しない場合はスキップ
            if (returnType != null && !returnType.isAssignableFrom(method.getReturnType()))
                continue;

            // アクセスレベルが一致しない場合はスキップ
            if (!method.canAccessFrom(caller))
                continue; // アクセスできないメソッドはスキップ

            // 引数の型が一致しない場合はスキップ
            VMType[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != args.length)
                continue; // 引数の数が違う場合はスキップ

            boolean allMatch = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                if (!parameterTypes[i].isAssignableFrom(args[i])) {
                    allMatch = false; // 引数の型が一致しない場合はスキップ
                    break;
                }
            }

            if (allMatch)
                return method; // 一致するメソッドを返す
        }
        return null; // 一致するメソッドが見つからなかった場合はnullを返す
    }

    @NotNull
    public VMType getType() {
        return new VMType(this.reference);
    }

    @Override
    public int hashCode() {
        return this.reference.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof VMClass other))
            return false;

        return this.reference.equals(other.reference);
    }

    @Override
    public VMClass getOwningClass() {
        return this; // VMClassは常に自身がオーナクラス
    }
}
