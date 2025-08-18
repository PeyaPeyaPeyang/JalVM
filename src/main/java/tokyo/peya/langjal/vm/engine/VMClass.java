package tokyo.peya.langjal.vm.engine;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.JALClassCompiler;
import tokyo.peya.langjal.compiler.jvm.AccessAttribute;
import tokyo.peya.langjal.compiler.jvm.AccessAttributeSet;
import tokyo.peya.langjal.compiler.jvm.AccessLevel;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.injections.InjectedField;
import tokyo.peya.langjal.vm.engine.injections.InjectedMethod;
import tokyo.peya.langjal.vm.engine.members.RestrictedAccessor;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threads.VMThread;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMClassObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class VMClass extends VMType implements RestrictedAccessor
{
    private final VMSystemClassLoader cl;
    private final ClassReference reference;
    private final ClassNode clazz;

    private final AccessAttributeSet accessAttributes;
    private final AccessLevel accessLevel;

    private final List<VMMethod> methods;
    private final List<VMField> fields;

    @Getter(lombok.AccessLevel.NONE)
    private final Map<VMField, VMValue> staticFields;

    private VMClassObject classObject;
    private VMClass superLink;

    public VMClass(@NotNull VMSystemClassLoader cl, @NotNull ClassNode clazz)
    {
        super(ClassReference.of(clazz));
        this.cl = cl;
        this.reference = ClassReference.of(clazz);
        this.clazz = clazz;

        this.accessAttributes = AccessAttributeSet.fromAccess(clazz.access);
        this.accessLevel = AccessLevel.fromAccess(clazz.access);

        this.methods = extractMethods(clazz);
        this.fields = extractFields(clazz);

        this.staticFields = this.initialiseStaticFields();
    }

    public VMClassObject getClassObject()
    {
        // 遅延評価しないと，ロード時に StackOverflowError が発生する可能性がある
        if (this.classObject == null)
            this.classObject = new VMClassObject(this.cl, this, this);
        return this.classObject;
    }

    public VMObject createInstance()
    {
        if (this.superLink == null)
            throw new IllegalStateException("Cannot create instance of class without super class link: " + this.reference.getFullQualifiedName());

        return new VMObject(this);
    }

    public void injectMethod(@NotNull VMSystemClassLoader cl, @NotNull InjectedMethod method)
    {
        if (!this.reference.equals(method.getOwningClass().getReference()))
        {
            throw new IllegalArgumentException("Injected method does not belong to this class: " + method.getOwningClass()
                                                                                                         .getReference());
        }

        String injectingMethodName = method.getName();
        for (VMMethod existingMethod : this.methods)
        {
            if (!existingMethod.getName().equals(injectingMethodName))
                continue; // メソッド名が一致しない場合はスキップ

            MethodNode existingNode = existingMethod.getMethodNode();
            MethodNode injectingNode = method.getMethodNode();
            if (existingNode.desc.equals(injectingNode.desc))
            {
                // 既存のメソッドと同じシグネチャのメソッドが存在する場合は上書き
                this.methods.remove(existingMethod); // 既存のメソッドを削除
                break;
            }
        }

        method.linkTypes(cl);

        // 新しいメソッドを追加
        this.methods.add(method);
    }

    public void injectField(@NotNull VMSystemClassLoader cl, @NotNull VMField field)
    {
        if (!this.reference.equals(field.getOwningClass().getReference()))
        {
            throw new VMPanic("Injected field does not belong to this class: " + field.getOwningClass().getReference());
        }

        String injectingFieldName = field.getName();
        for (VMField existingField : this.fields)
        {
            if (existingField.getName().equals(injectingFieldName))
            {
                this.fields.remove(existingField); // 既存のフィールドを削除
                break;
            }
        }

        field.linkType(cl); // フィールドの型をリンク

        // 既存のフィールドを削除
        this.fields.removeIf(existingField -> existingField.getName().equals(field.getName()));
        // 新しいフィールドを追加
        this.fields.add(field);
    }

    public boolean isSubclassOf(@NotNull VMClass maySuper)
    {
        if (this == maySuper)
            return true; // 同じクラスならtrue
        if (this.superLink == null)
            return false; // スーパークラスがない場合はfalse

        // 親をたどりながら探索
        VMClass current = this.superLink;
        while (current != null)
        {
            if (current == maySuper)
                return true;
            current = current.superLink;
        }

        // 一致せず
        return false;
    }

    public void initialiseClass(@NotNull VMSystemClassLoader cl)
    {
        // リンク処理 -> クラスのスーパクラスやメンバの参照を解決
        this.linkSuper(cl);
        this.linkMembers(cl);
        // 静的初期化メソッドを呼び出す
        VMThread currentThread = cl.getVm().getEngine().getCurrentThread();
        this.invokeStaticInitaliser(currentThread);
    }

    private void invokeStaticInitaliser(@NotNull VMThread callerThread)
    {
        VMMethod staticInitMethod = this.findStaticInitialiser();
        if (staticInitMethod == null)
            return;  // 静的初期化メソッドがない場合は何もしない

        // 静的初期化メソッドを呼び出す
        staticInitMethod.invokeBypassAccess(callerThread, null);
    }

    private void linkSuper(@NotNull VMSystemClassLoader cl)
    {
        String superName = this.clazz.superName;
        if (superName == null || superName.isEmpty())
            superName = "java/lang/Object"; // デフォルトは java.lang.Object

        this.superLink = cl.findClass(ClassReference.of(superName));
    }

    private void linkMembers(@NotNull VMSystemClassLoader cl)
    {
        for (VMMethod method : this.methods)
            method.linkTypes(cl);
        for (VMField field : this.fields)
            field.linkType(cl);
    }

    private List<VMField> extractFields(@NotNull ClassNode classNode)
    {
        FieldNode[] fields = classNode.fields.toArray(new FieldNode[0]);
        List<VMField> vmFields = new ArrayList<>();
        for (FieldNode fieldNode : fields)
        {
            String descString = fieldNode.desc;
            vmFields.add(new VMField(
                    this,
                    VMType.ofTypeDescriptor(descString),
                    fieldNode
            ));
        }

        return vmFields;
    }

    private Map<VMField, VMValue> initialiseStaticFields()
    {
        Map<VMField, VMValue> staticFields = new HashMap<>();
        for (VMField field : this.fields)
            if (field.getAccessAttributes().has(AccessAttribute.STATIC))
                staticFields.put(field, field.getType().defaultValue());

        return staticFields;
    }

    private List<VMMethod> extractMethods(@NotNull ClassNode classNode)
    {
        return classNode.methods.stream()
                                .map(methodNode -> new VMMethod(this, methodNode))
                                .collect(Collectors.toList());
    }

    @Nullable
    public VMMethod findConstructor(@Nullable VMClass caller, @NotNull VMType... args)
    {
        return this.findSuitableMethod(caller, "<init>", VMType.VOID, args);
    }

    public VMMethod findEntryPoint()
    {
        for (VMMethod method : this.methods)
        {
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

    public ClassReference getReference()
    {
        return ClassReference.of(this.clazz);
    }

    public VMMethod findStaticInitialiser()
    {
        for (VMMethod method : this.methods)
        {
            if (method.getName().equals("<clinit>")
                    && method.getAccessAttributes().has(AccessAttribute.STATIC)
                    && method.getReturnType().equals(VMType.VOID)
                    && method.getParameterTypes().length == 0)
                return method; // 静的初期化メソッドを見つけたら返す
        }
        return null; // 見つからなかった場合はnullを返す
    }

    @Nullable
    public VMMethod findSuitableMethod(@Nullable VMClass caller, @NotNull String methodName,
                                       @Nullable VMType returnType, @NotNull VMType... args)
    {
        for (VMMethod method : this.methods)
        {
            // メソッド名が一致しない場合はスキップ
            if (!method.getName().equals(methodName))
                continue;
            // 戻り値の型が一致しない場合はスキップ
            if (!(returnType == null || returnType.isAssignableFrom(method.getReturnType())))
                continue;

            // アクセスレベルが一致しない場合はスキップ
            if (!method.canAccessFrom(caller))
                continue; // アクセスできないメソッドはスキップ

            // 引数の型が一致しない場合はスキップ
            VMType[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != args.length)
                continue; // 引数の数が違う場合はスキップ

            boolean allMatch = true;
            for (int i = 0; i < parameterTypes.length; i++)
            {
                if (!parameterTypes[i].isAssignableFrom(args[i]))
                {
                    allMatch = false; // 引数の型が一致しない場合はスキップ
                    break;
                }
            }

            if (allMatch)
                return method; // 一致するメソッドを返す
        }
        return null; // 一致するメソッドが見つからなかった場合はnullを返す
    }

    public void setStaticField(@NotNull String fieldName, @NotNull VMValue value)
    {
        VMField field = this.findStaticField(fieldName);
        if (!field.getType().isAssignableFrom(value.type()))
            throw new VMPanic("Cannot assign value of type " + value.type() + " to field " + fieldName + " of type " + field.getType());

        if (field instanceof InjectedField)
            ((InjectedField) field).set(this, null, value); // InjectedFieldの場合は特別な処理を行う

        this.staticFields.put(field, value); // 静的フィールドに値を設定
    }

    @NotNull
    public VMValue getStaticFieldValue(@NotNull VMField field)
    {
        if (field instanceof InjectedField)
            return ((InjectedField) field).get(this, null);

        VMValue value = this.staticFields.get(field);
        if (value == null)
            throw new VMPanic("Static field " + field.getName() + " is not initialized in class " + this.reference.getFullQualifiedName());

        return value; // 静的フィールドの値を返す
    }

    @NotNull
    public VMField findStaticField(@NotNull String fieldName)
    {
        for (VMField field : this.fields)
            if (field.getName().equals(fieldName))
                return field; // 一致するフィールドを返す

        throw new VMPanic("Field not found: " + fieldName + " in class " + this.reference.getFullQualifiedName());
    }

    @Override
    public int hashCode()
    {
        return this.reference.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!(obj instanceof VMClass other))
            return false;

        return this.reference.equals(other.reference);
    }

    @Override
    public VMClass getOwningClass()
    {
        return this; // VMClassは常に自身がオーナクラス
    }
}
