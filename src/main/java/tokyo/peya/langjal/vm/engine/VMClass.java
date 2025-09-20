package tokyo.peya.langjal.vm.engine;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.AccessAttribute;
import tokyo.peya.langjal.compiler.jvm.AccessAttributeSet;
import tokyo.peya.langjal.compiler.jvm.AccessLevel;
import tokyo.peya.langjal.compiler.jvm.MethodDescriptor;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.injections.InjectedField;
import tokyo.peya.langjal.vm.engine.injections.InjectedMethod;
import tokyo.peya.langjal.vm.engine.members.AccessibleObject;
import tokyo.peya.langjal.vm.engine.members.VMAnnotationElementVirtualMethod;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMAnnotation;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMReferenceValue;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMClassObject;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Getter
public class VMClass extends VMType<VMReferenceValue> implements AccessibleObject, VMComponent
{
    @Getter(lombok.AccessLevel.NONE)
    private final JalVM vm;

    protected final ClassReference reference;
    protected final ClassNode clazz;

    protected final AccessAttributeSet accessAttributes;
    protected final AccessLevel accessLevel;

    @Getter(lombok.AccessLevel.NONE)
    protected final List<VMClass> innerLinks;
    @Getter(lombok.AccessLevel.NONE)
    protected final List<VMClass> interfaceLinks;
    @Getter(lombok.AccessLevel.NONE)
    protected final List<VMMethod> methods;
    @Getter(lombok.AccessLevel.NONE)
    protected final List<VMField> fields;
    @Getter(lombok.AccessLevel.NONE)
    protected final List<VMAnnotation> annotations;

    @Getter(lombok.AccessLevel.NONE)
    protected final Map<VMField, VMValue> staticFields;

    private long lastFieldID;

    protected boolean isLinked;
    protected boolean isInitialised;

    private Function</* owner: */ VMObject, /* target: */ VMObject> instanceCreator;
    @Getter(lombok.AccessLevel.NONE)
    private VMClassObject classObject;

    protected VMClass superLink;
    protected VMClass outerLink;

    private VMValue classData;

    public VMClass(@NotNull VMComponent component, @NotNull ClassNode clazz, @Nullable VMType<?> componentType)
    {
        super(component, ClassReference.of(clazz), componentType);
        this.vm = component.getVM();
        this.reference = ClassReference.of(clazz);
        this.clazz = clazz;

        this.accessAttributes = AccessAttributeSet.fromAccess(clazz.access);
        this.accessLevel = AccessLevel.fromAccess(clazz.access);

        this.methods = new ArrayList<>();
        this.fields = new ArrayList<>();
        this.interfaceLinks = new ArrayList<>();
        this.innerLinks = new ArrayList<>();
        this.staticFields = new HashMap<>();
        this.annotations = new ArrayList<>();
    }

    public VMClass(@NotNull JalVM vm, @NotNull ClassNode clazz)
    {
        this(vm, clazz, null);
    }

    public VMClass(@NotNull JalVM vm, @NotNull VMType<?> componentType)
    {
        this(vm, componentType.getLinkedClass().clazz, componentType);
    }

    public void setClassData(@NotNull VMValue classData)
    {
        this.classData = classData;
        if (this.classObject != null)
            this.classObject.updateClassData();
    }

    public VMClassObject getClassObject()
    {
        // 遅延評価しないと，ロード時に StackOverflowError が発生する可能性がある
        if (this.classObject == null)
            this.classObject = new VMClassObject(this.vm, this, this);

        this.classObject.updateClassData();

        return this.classObject;
    }

    public VMObject createInstance(@NotNull VMObject owner)
    {
        if (this.instanceCreator == null)
            return new VMObject(this, owner);
        else
            return this.instanceCreator.apply(owner);
    }

    public VMObject createInstance()
    {
        if (this.superLink == null)
            throw new IllegalStateException("Cannot create instance of class without super class link: " + this.reference.getFullQualifiedName());

        if (this.instanceCreator == null)
            return new VMObject(this);
        else
            return this.instanceCreator.apply(null);
    }

    public void injectMethod(@NotNull InjectedMethod method)
    {
        VMMethod existing = this.findMethod(method.getName(), method.getDescriptor());
        if (existing != null)
            this.methods.remove(existing); // 既存のメソッドを削除

        // 新しいメソッドを追加
        this.methods.add(method);
    }

    public void injectField(@NotNull VMField field)
    {
        // 既存のフィールドを削除
        this.fields.removeIf(existingField -> existingField.getName().equals(field.getName()));
        // 新しいフィールドを追加
        this.fields.add(field);

        if (field.getAccessAttributes().has(AccessAttribute.STATIC))
            this.vm.getHeap().recognizeStaticField(field); // 静的フィールドをヒープに認識させる
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

    @Override
    public void link(@NotNull JalVM vm)
    {
        if (this.isLinked)
            return; // 既にリンク済みなら何もしない

        VMSystemClassLoader cl = vm.getClassLoader();
        // リンク処理 -> クラスのスーパクラスやメンバの参照を解決
        super.link(vm);
        this.linkHierarchy(cl);
        this.linkSuper(cl);
        this.linkInterfaces(cl);
        this.linkFields();
        this.linkMethods();
        this.linkAnnotations();
        if (this.accessAttributes.has(AccessAttribute.ANNOTATION))
            this.linkAnnotationMethods();

        this.isLinked = true; // リンク済みフラグを立てる
    }

    private void linkHierarchy(@NotNull VMSystemClassLoader cl)
    {
        List<String> innerClassNames = this.clazz.innerClasses.stream()
                // .filter(inner -> inner.outerName != null && inner.outerName.equals(this.clazz.name))
                .map(inner -> inner.name)
                .toList();

        for (String innerName : innerClassNames)
        {
            VMClass innerClass = cl.findClass(ClassReference.of(innerName));
            this.innerLinks.add(innerClass); // インナークラスをリンク
        }

        if (!(this.clazz.outerClass == null || this.clazz.outerClass.isEmpty()))
            this.outerLink = cl.findClass(ClassReference.of(this.clazz.outerClass));
    }


    private void linkSuper(@NotNull VMSystemClassLoader cl)
    {
        String superName = this.clazz.superName;
        if (superName == null || superName.isEmpty())
            superName = "java/lang/Object"; // デフォルトは java.lang.Object

        this.superLink = cl.findClass(ClassReference.of(superName));
    }

    private void linkInterfaces(@NotNull VMSystemClassLoader cl)
    {
        for (String interfaceName : this.clazz.interfaces)
        {
            VMClass interfaceClass = cl.findClass(ClassReference.of(interfaceName));
            this.interfaceLinks.add(interfaceClass); // インターフェースクラスをリンク
        }
    }

    private void linkFields()
    {
        FieldNode[] fields = this.clazz.fields.toArray(new FieldNode[0]);
        for (int i = 0; i < fields.length; i++)
        {
            FieldNode fieldNode = fields[i];
            String descString = fieldNode.desc;
            boolean isStatic = (fieldNode.access & Opcodes.ACC_STATIC) != 0;
            long id;
            if (isStatic)
                id = this.vm.getHeap().assignStaticFieldID(); // 静的フィールドでデフォルト値がある場合は新しいIDを割り当て
            else
                id = this.lastFieldID += 16; // それ以外は次のフィールドIDを取得

            VMField field = new VMField(
                    this.vm,
                    this,
                    i,
                    id,
                    VMType.of(this.vm, descString),
                    fieldNode
            );
            this.fields.add(field);
            if (isStatic)
                this.vm.getHeap().recognizeStaticField(field); // 静的フィールドをヒープに認識させる
        }
    }

    private void initialiseStaticFields()
    {
        for (VMField field : this.fields)
            if (field.getAccessAttributes().has(AccessAttribute.STATIC))
                this.staticFields.put(field, field.defaultValue());
    }

    private void linkMethods()
    {
        int methodsCount = this.clazz.methods.size();
        for (int i = 0; i < methodsCount; i++)
        {
            MethodNode methodNode = this.clazz.methods.get(i);
            this.methods.add(new VMMethod(this.vm, this, i, methodNode));
        }
    }

    private void linkAnnotations()
    {
        this.annotations.addAll(VMAnnotation.of(this, this.clazz.visibleAnnotations));
    }

    private void linkAnnotationMethods()
    {
        List<VMMethod> toRemove = new ArrayList<>();
        List<VMMethod> toAdd = new ArrayList<>();
        for (VMMethod method: this.methods)
        {
            if (!method.getAccessAttributes().has(AccessAttribute.ABSTRACT))
                continue; // 抽象メソッドでない場合はスキップ
            if (method.getParameterTypes().length != 0)
                continue; // 引数がある場合はスキップ

            // アノテーション要素を表すメソッドに対応するVMAnnotationElementVirtualMethodを生成して置き換え
            VMAnnotationElementVirtualMethod annoMethod = new VMAnnotationElementVirtualMethod(
                    this,
                    method.getName(),
                    method.getReturnType()
            );
            toRemove.add(method); // 既存の抽象メソッドを削除リストに追加
            toAdd.add(annoMethod); // 新しいアノテーション要素メソッドを追加リストに追加
        }

        this.methods.addAll(toAdd);
        toRemove.forEach(this.methods::remove);
    }

    @Override
    public boolean isAssignableFrom(@NotNull VMType<?> other)
    {
        VMClass otherClass = other.getLinkedClass();

        boolean isSubclass = otherClass.isSubclassOf(this);
        if (isSubclass)
            return true; // 他のクラスがこのクラスのサブクラスである場合はtrue

        for (VMClass iface : otherClass.interfaceLinks)
        {
            if (this.isAssignableFrom(iface))
                return true; // 他のクラスがこのクラスのインターフェースを実装している場合はtrue
        }

        return false; // それ以外の場合はfalse
    }

    public void initialise(@NotNull VMThread callerThread)
    {
        if (this.isInitialised)
            return; // 既に初期化済みなら何もしない

        if (!(this.superLink == null || this.superLink == this || this.superLink.isInitialised))
        {
            this.superLink.initialise(callerThread); // スーパークラスがある場合は再帰的に初期化
            return; // スーパークラスの初期化が完了するまで待つ
        }

        this.isInitialised = true; // 初期化済みフラグを立てる
        this.initialiseStaticFields();
        VMMethod staticInitMethod = this.findStaticInitialiser();
        if (staticInitMethod == null)
            return;  // 静的初期化メソッドがない場合は何もしない

        // 静的初期化メソッドを呼び出す
        staticInitMethod.invokeBypassAccess(callerThread.getCurrentFrame(), null);
    }

    @Nullable
    public VMMethod findConstructor(@Nullable VMClass caller, @NotNull VMClass owner, @NotNull VMType<?>... args)
    {
        return this.findSuitableMethod(caller, owner, "<init>", VMType.of(this.vm, PrimitiveTypes.VOID), args);
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

    private VMMethod findStaticInitialiser()
    {
        for (VMMethod method : this.methods)
        {
            if (method.getName().equals("<clinit>")
                    && method.getAccessAttributes().has(AccessAttribute.STATIC)
                    && method.getReturnType().equals(VMType.of(this.vm, PrimitiveTypes.VOID))
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
            if (method.isSuitableToCall(caller, owner, methodName, returnType, args))
                return method; // 一致するメソッドを返す

        if (!(this.superLink == null || this.superLink == this)) // スーパークラスが存在し、かつ自身ではない場合
            // スーパークラスに同名のメソッドがあるか再帰的に探す
        {
            VMMethod method = this.superLink.findSuitableMethod(caller, owner, methodName, returnType, args);
            if (method != null)
                return method; // 一致するメソッドを返す
        }
        if (!this.interfaceLinks.isEmpty())
            // インターフェースに同名のメソッドがあるか再帰的に探す
            for (VMClass iface : this.interfaceLinks)
            {
                VMMethod method = iface.findSuitableMethod(caller, owner, methodName, returnType, args);
                if (method != null)
                    return method; // 一致するメソッドを返す
            }

        return null; // 一致するメソッドが見つからなかった場合はnullを返す
    }

    @Nullable
    public VMMethod findMethod(@NotNull String methodName)
    {
        return this.findMethod(methodName, null);
    }

    @Nullable
    public VMMethod findMethod(@NotNull String methodName, @Nullable MethodDescriptor desc)
    {
        for (VMMethod method : this.methods)
        {
            if (method.getName().equals(methodName) && (desc == null || method.getDescriptor().equals(desc) || method.isSignaturePolymorphic()))
                return method; // 一致するメソッドを返す
        }
        return null; // 見つからなかった場合はnullを返す
    }

    public void setStaticField(@NotNull VMField field, @NotNull VMValue value)
    {
        VMClass owner = findFieldOwner(field, this);

        VMValue conformedValue = value.conformValue(field.getType()); // 値をフィールドの型に適合させる
        if (!field.getType().isAssignableFrom(conformedValue.type()))
            throw new VMPanic("Cannot assign value of type " + value.type() + " to field " + field.getName() + " of type " + field.getType());

        if (field instanceof InjectedField)
            ((InjectedField) field).set(owner, null, conformedValue); // InjectedFieldの場合は特別な処理を行う
        else
            owner.staticFields.put(field, conformedValue); // 静的フィールドに値を設定
    }
    public void setStaticField(@NotNull String fieldName, @NotNull VMValue value)
    {
        VMField field = this.findField(fieldName);
        this.setStaticField(field, value); // フィールド名からフィールドを取得し、値を設定
    }

    @NotNull
    public VMValue getStaticFieldValue(@NotNull VMField field)
    {
        VMClass owner = findFieldOwner(field, this);

        if (field instanceof InjectedField)
            return ((InjectedField) field).get(owner, null);

        VMValue value = owner.staticFields.get(field);
        if (value == null)
            throw new VMPanic("Static field " + field.getName() + " is not initialized in class " + this.reference.getFullQualifiedName());

        return value; // 静的フィールドの値を返す
    }

    @NotNull
    public VMField findField(@NotNull String fieldName, @Nullable ClassReference owner)
    {
        VMField field = this.findFieldSafe(fieldName, owner);
        if (field != null)
            return field;

        throw new VMPanic("Field not found: " + fieldName + " in class " + this.reference.getFullQualifiedName());
    }

    @NotNull
    public VMField findField(@NotNull String fieldName)
    {
        return this.findField(fieldName, null);
        }

    @NotNull
    public VMField findField(long id)
    {
        VMClass current = this;
        do
        {
            for (VMField field : current.fields)
                if (field.getFieldID() == id)
                    return field; // 一致するフィールドを返す

            if (current == current.superLink)
                break;

            current = current.superLink
            ; // スーパークラスに移動
        } while (!(current == null || current == this)); // スーパークラスが存在し、かつ自身ではない場合

        throw new VMPanic("Field with ID " + id + " not found in class " + this.reference.getFullQualifiedName());
    }

    @Nullable
    public VMField findFieldSafe(@NotNull String fieldName)
    {
        return this.findFieldSafe(fieldName, null);
    }

    @Nullable
    public VMField findFieldSafe(@NotNull String fieldName, @Nullable ClassReference owner)
    {
        boolean ownerFound = owner == null;  // オーナーが指定されていない場合は最初から true
        VMClass current = this;
        do
        {
            // オーナーが指定されている場合はオーナーと一致するか確認
            if (!ownerFound)
                ownerFound = current.reference.equals(owner);

            // オーナーが見つかっている場合のみフィールドを検索
            if (ownerFound)
                for (VMField field : current.fields)
                    if (field.getName().equals(fieldName))
                        return field; // 一致するフィールドを返す

            if (current == current.superLink)
                break;

            current = current.superLink; // スーパークラスに移動
        } while (!(current == null || current == this)); // スーパークラスが存在し，かつ自身ではない場合

        return null;
    }

    public boolean isInstance(@Nullable VMValue instance)
    {
        if (!(instance instanceof VMObject obj))
            return false;

        return this.isAssignableFrom(obj.getObjectType());
    }

    private static VMClass findFieldOwner(@NotNull VMField field, @NotNull VMClass apex)
    {
        VMClass current = apex;
        do
        {
            for (VMField f : current.fields)
            {
                if (f.equals(field))
                    return current; // フィールドが見つかったらそのクラスを返す
            }
            current = current.superLink; // スーパークラスに移動
        } while (!(current == null || current == apex)); // スーパークラスが存在し、かつ自身ではない場合

        throw new VMPanic("Field " + field.getName() + " not found in class hierarchy of " + apex.reference.getFullQualifiedName());
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
        {
            if (obj instanceof VMType<?> otherType)
                return this.getTypeDescriptor().equals(otherType.getTypeDescriptor());
            return false;
        }

        return this.reference.equals(other.getReference());
    }

    @Override
    public String toString()
    {
        return this.reference.getFullQualifiedName();
    }

    public int getNextSlot()
    {
        int maxSlot = -1;
        for (VMField field : this.fields)
        {
            if (field.getSlot() > maxSlot)
                maxSlot = field.getSlot();
        }
        return maxSlot + 1; // 次のスロット番号を返す
    }

    public long getNextFieldID()
    {
        long maxID = -16;
        for (VMField field : this.fields)
        {
            if (field.getFieldID() > maxID)
                maxID = field.getFieldID();
        }
        return maxID + 16; // 次のフィールドIDを返す
    }

    public void injectInstanceCreator(@NotNull Function<VMObject, VMObject> creator)
    {
        this.instanceCreator = creator;
    }

    public void addNestedClassDynamically(@NotNull VMClass nestedClass)
    {
        if (nestedClass.outerLink != null)
            throw new VMPanic("Nested class " + nestedClass.reference.getFullQualifiedName() + " already has an outer link to " + nestedClass.outerLink.reference.getFullQualifiedName());

        if (!this.innerLinks.contains(nestedClass))
            this.innerLinks.add(nestedClass);

        nestedClass.outerLink = this;
    }

    @Override
    public VMClass getOwningClass()
    {
        return this; // VMClassは常に自身がオーナクラス
    }

    public List<VMClass> getInnerLinks()
    {
        return Collections.unmodifiableList(this.innerLinks);
    }

    public List<VMField> getFields()
    {
        return Collections.unmodifiableList(this.fields);
    }

    public List<VMMethod> getMethods()
    {
        return Collections.unmodifiableList(this.methods);
    }

    public @NotNull String getTypeDescriptor()
    {
        return "L" + this.reference.getFullQualifiedName() + ";";
    }

    public String base64()
    {
        ClassWriter writer = new ClassWriter(0);
        this.clazz.accept(writer);
        byte[] classBytes = writer.toByteArray();
        return Base64.getEncoder().encodeToString(classBytes);
    }
}
