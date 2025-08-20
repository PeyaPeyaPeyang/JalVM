package tokyo.peya.langjal.vm.engine;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.AccessAttribute;
import tokyo.peya.langjal.compiler.jvm.AccessAttributeSet;
import tokyo.peya.langjal.compiler.jvm.AccessLevel;
import tokyo.peya.langjal.compiler.jvm.MethodDescriptor;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.injections.InjectedField;
import tokyo.peya.langjal.vm.engine.injections.InjectedMethod;
import tokyo.peya.langjal.vm.engine.members.RestrictedAccessor;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMReferenceValue;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMClassObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class VMClass extends VMType<VMReferenceValue> implements RestrictedAccessor
{
    private final ClassReference reference;
    private final ClassNode clazz;

    private final AccessAttributeSet accessAttributes;
    private final AccessLevel accessLevel;

    @Getter(lombok.AccessLevel.NONE)
    private final List<VMClass> interfaceLinks;
    @Getter(lombok.AccessLevel.NONE)
    private final List<VMMethod> methods;
    @Getter(lombok.AccessLevel.NONE)
    private final List<VMField> fields;

    @Getter(lombok.AccessLevel.NONE)
    private final Map<VMField, VMValue> staticFields;

    private boolean isInitialised;
    @Getter(lombok.AccessLevel.NONE)
    private VMClassObject classObject;
    private VMClass superLink;

    // Primitive は static final で持っておきたい。
    @Setter
    private VMSystemClassLoader classLoader;

    public VMClass(@Nullable VMSystemClassLoader classLoader, @NotNull ClassNode clazz)
    {
        super(ClassReference.of(clazz));
        this.classLoader = classLoader;
        this.reference = ClassReference.of(clazz);
        this.clazz = clazz;

        this.accessAttributes = AccessAttributeSet.fromAccess(clazz.access);
        this.accessLevel = AccessLevel.fromAccess(clazz.access);

        this.methods = extractMethods(clazz);
        this.fields = extractFields(clazz);
        this.interfaceLinks = new ArrayList<>();
        this.staticFields = new HashMap<>();
    }

    public VMClassObject getClassObject(@NotNull VMSystemClassLoader cl)
    {
        // 遅延評価しないと，ロード時に StackOverflowError が発生する可能性がある
        if (this.classObject == null)
            this.classObject = new VMClassObject(cl, this, this);
        return this.classObject;
    }

    public VMObject createInstance(@NotNull VMObject owner)
    {
        if (this.superLink == null)
            throw new IllegalStateException("Cannot create instance of class without super class link: " + this.reference.getFullQualifiedName());

        return new VMObject(this, owner);
    }
    public VMObject createInstance()
    {
        if (this.superLink == null)
            throw new IllegalStateException("Cannot create instance of class without super class link: " + this.reference.getFullQualifiedName());

        return new VMObject(this, null);
    }

    public void injectMethod(@NotNull VMSystemClassLoader cl, @NotNull InjectedMethod method)
    {
        if (!this.reference.equals(method.getOwningClass().getReference()))
            throw new IllegalArgumentException("Injected method does not belong to this class: " + method.getOwningClass()
                                                                                                         .getReference());

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
            throw new VMPanic("Injected field does not belong to this class: " + field.getOwningClass().getReference());

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
        if (this.equals(maySuper))
            return true;

        // --- クラス継承をたどる ---
        VMClass current = this;
        while (current != null)
        {
            if (current.equals(maySuper))
                return true;

            // このクラスが実装しているインタフェースをチェック
            if (implementsInterface(current, maySuper))
                return true;

            if (current.superLink == current)
                break;
            current = current.superLink;
        }

        return false;
    }

    private boolean implementsInterface(@NotNull VMClass clazz, @NotNull VMClass maySuper)
    {
        for (VMClass iface : clazz.interfaceLinks)
        {
            if (iface.equals(maySuper))
                return true;

            // インタフェースの継承もたどる
            if (implementsInterface(iface, maySuper))
                return true;
        }
        return false;
    }
    public void link(@NotNull VMSystemClassLoader cl)
    {
        // リンク処理 -> クラスのスーパクラスやメンバの参照を解決
        this.linkClass(cl);
        this.linkSuper(cl);
        this.linkMembers(cl);
        this.linkInterfaces(cl);
    }

    public void initialise(@NotNull VMThread callerThread)
    {
        if (this.isInitialised)
            return; // 既に初期化済みなら何もしない

        this.isInitialised = true; // 初期化済みフラグを立てる
        if (this.superLink != null && this.superLink != this)
            this.superLink.initialise(callerThread); // スーパークラスがある場合は再帰的に初期化

        this.initialiseStaticFields();
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

    private void linkInterfaces(@NotNull VMSystemClassLoader cl)
    {
        for (String interfaceName : this.clazz.interfaces)
        {
            VMClass interfaceClass = cl.findClass(ClassReference.of(interfaceName));
            this.interfaceLinks.add(interfaceClass); // インターフェースクラスをリンク
        }
    }

    private List<VMField> extractFields(@NotNull ClassNode classNode)
    {
        FieldNode[] fields = classNode.fields.toArray(new FieldNode[0]);
        List<VMField> vmFields = new ArrayList<>();
        long id = 0; // フィールドIDの初期化
        for (FieldNode fieldNode : fields)
        {
            String descString = fieldNode.desc;
            vmFields.add(new VMField(
                    id += 16, // フィールドIDをインクリメント
                    this.classLoader,
                    this,
                    VMType.of(descString),
                    fieldNode
            ));
        }

        return vmFields;
    }

    private void initialiseStaticFields()
    {
        for (VMField field : this.fields)
            if (field.getAccessAttributes().has(AccessAttribute.STATIC))
                this.staticFields.put(field, field.defaultValue());
    }

    private List<VMMethod> extractMethods(@NotNull ClassNode classNode)
    {
        return classNode.methods.stream()
                                .map(methodNode -> new VMMethod(this, methodNode))
                                .collect(Collectors.toList());
    }

    @Nullable
    public VMMethod findConstructor(@Nullable VMClass caller, @NotNull VMClass owner, @NotNull VMType<?>... args)
    {
        return this.findSuitableMethod(caller, owner, "<init>", VMType.VOID, args);
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

    private VMMethod findStaticInitialiser()
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
                                       @Nullable VMType<?> returnType, @NotNull VMType<?>... args)
    {
        return this.findSuitableMethod(caller, this, methodName, returnType, args);
    }
    @Nullable
    public VMMethod findSuitableMethod(@Nullable VMClass caller, @Nullable VMClass owner, @NotNull String methodName,
                                       @Nullable VMType<?> returnType, @NotNull VMType<?>... args)
    {
        for (VMMethod method : this.methods)
        {
            if (!(owner == null || owner.equals(method.getOwningClass())))
                continue;  // オーナークラスが一致しない場合はスキップ
            if (!method.getName().equals(methodName))
                continue;  // メソッド名が一致しない場合はスキップ
            if (!(returnType == null || returnType.equals(method.getReturnType())))
                continue;  // 戻り値の型が一致しない場合はスキップ

            // アクセスレベルが一致しない場合はスキップ
            if (!method.canAccessFrom(caller))
                continue; // アクセスできないメソッドはスキップ

            // 引数の型が一致しない場合はスキップ
            VMType<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != args.length)
                continue; // 引数の数が違う場合はスキップ

            boolean allMatch = true;
            for (int i = 0; i < parameterTypes.length; i++)
            {
                if (!parameterTypes[i].equals(args[i]))
                {
                    allMatch = false; // 引数の型が一致しない場合はスキップ
                    break;
                }
            }

            if (allMatch)
                return method; // 一致するメソッドを返す
        }

        if (!(this.superLink == null || this.superLink == this)) // スーパークラスが存在し、かつ自身ではない場合
            // スーパークラスに同名のメソッドがあるか再帰的に探す
            return this.superLink.findSuitableMethod(caller, owner, methodName, returnType, args);

        return null; // 一致するメソッドが見つからなかった場合はnullを返す
    }

    @Nullable
    public VMMethod findMethod(@NotNull String methodName, @Nullable MethodDescriptor desc)
    {
        for (VMMethod method : this.methods)
        {
            if (method.getName().equals(methodName) && (desc == null || method.getDescriptor().equals(desc)))
                return method; // 一致するメソッドを返す
        }
        return null; // 見つからなかった場合はnullを返す
    }

    public void setStaticField(@NotNull VMField field, @NotNull VMValue value)
    {
        VMValue conformedValue = value.conformValue(field.getType()); // 値をフィールドの型に適合させる
        if (!field.getType().isAssignableFrom(conformedValue.type()))
            throw new VMPanic("Cannot assign value of type " + value.type() + " to field " + field.getName() + " of type " + field.getType());

        if (field instanceof InjectedField)
            ((InjectedField) field).set(this, null, conformedValue); // InjectedFieldの場合は特別な処理を行う
        else
            this.staticFields.put(field, conformedValue); // 静的フィールドに値を設定
    }
    public void setStaticField(@NotNull String fieldName, @NotNull VMValue value)
    {
        VMField field = this.findField(fieldName);
        this.setStaticField(field, value); // フィールド名からフィールドを取得し、値を設定
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
    public VMField findField(@NotNull String fieldName)
    {
        for (VMField field : this.fields)
            if (field.getName().equals(fieldName))
                return field; // 一致するフィールドを返す

        throw new VMPanic("Field not found: " + fieldName + " in class " + this.reference.getFullQualifiedName());
    }

    @NotNull
    public VMField findField(long id)
    {
        for (VMField field : this.fields)
            if (field.getFieldID() == id)
                return field; // 一致するフィールドを返す

        throw new VMPanic("Field with ID " + id + " not found in class " + this.reference.getFullQualifiedName());
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

    public List<VMField> getFields()
    {
        return Collections.unmodifiableList(this.fields);
    }

    public List<VMMethod> getMethods()
    {
        return Collections.unmodifiableList(this.methods);
    }
}
