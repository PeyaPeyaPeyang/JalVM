package tokyo.peya.langjal.vm.values;

import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.compiler.jvm.ClassReferenceType;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.compiler.jvm.Type;
import tokyo.peya.langjal.compiler.jvm.TypeDescriptor;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMHeap;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMArrayClass;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.engine.VMPrimitiveClass;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;

import java.util.Objects;

@Getter
public class VMType<T extends VMValue> implements VMComponent
{
    @Getter(AccessLevel.NONE)
    private final JalVM vm;
    private final Type type;
    private final VMType<?> componentType;  // 配列型の場合，1次元減らした型
    private final boolean isPrimitive;  // 配列でも基底タイプがプリミティブなら true

    protected VMClass linkedClass;

    private VMType(@NotNull JalVM vm, @NotNull Type type, @Nullable VMType<?> componentType)
    {
        this.vm = vm;
        this.type = type;
        this.componentType = componentType;
        this.isPrimitive = type.isPrimitive() && componentType == null;

        if (this.getClass() == VMType.class)
            vm.getClassLoader().linkType(this);
    }

    private VMType(@NotNull VMComponent component, @NotNull PrimitiveTypes type)
    {
        this.vm = component.getVM();
        this.type = type;
        this.componentType = null;
        this.isPrimitive = true;
    }


    public VMType(@NotNull VMComponent component, @NotNull ClassReference reference)
    {
        this(component, reference, null);
    }

    public VMType(@NotNull VMComponent component, @NotNull ClassReference reference, @Nullable VMType<?> componentType)
    {
        this.vm = component.getVM();
        this.type = ClassReferenceType.parse(reference.getFullQualifiedName());
        this.componentType = componentType;
        this.isPrimitive = false;
        if (this.getClass() == VMType.class)
            component.getClassLoader().linkType(this);
    }

    @Override
    public @NotNull JalVM getVM()
    {
        return this.vm;
    }

    public static void initVM(@NotNull JalVM vm)
    {
        addInitClass(vm, new VMType<>(vm, PrimitiveTypes.VOID));
        addInitClass(vm, new VMType<>(vm, PrimitiveTypes.BOOLEAN));
        addInitClass(vm, new VMType<>(vm, PrimitiveTypes.BYTE));
        addInitClass(vm, new VMType<>(vm, PrimitiveTypes.CHAR));
        addInitClass(vm, new VMType<>(vm, PrimitiveTypes.SHORT));
        addInitClass(vm, new VMType<>(vm, PrimitiveTypes.INT));
        addInitClass(vm, new VMType<>(vm, PrimitiveTypes.LONG));
        addInitClass(vm, new VMType<>(vm, PrimitiveTypes.FLOAT));
        addInitClass(vm, new VMType<>(vm, PrimitiveTypes.DOUBLE));
        vm.getClassLoader().resumeLinking();
        addInitClass(vm, new VMType<>(vm, ClassReference.OBJECT));
        addInitClass(vm, VMType.of(vm, TypeDescriptor.parse("[Ljava/lang/Object;")));

    }

    private static void addInitClass(@NotNull JalVM vm, @NotNull VMType<?> type)
    {
        vm.getHeap().addType(type);
        vm.getClassLoader().linkLater(type);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <T extends VMPrimitive> VMType<T> of(@NotNull VMComponent component, @NotNull PrimitiveTypes primitive)
    {
        return (VMType<T>) component.getVM().getHeap().getType(primitive.getDescriptor());
    }

    @NotNull
    public static VMType<VMReferenceValue> ofGenericObject(@NotNull VMComponent component)
    {
        return ofGenericObject(component.getVM().getHeap());
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static VMType<VMReferenceValue> ofGenericObject(@NotNull VMHeap heap)
    {
        return (VMType<VMReferenceValue>) heap.getType("Ljava/lang/Object;");
    }

    @NotNull
    public static VMType<VMArray> ofGenericArray(@NotNull VMComponent component)
    {
        return ofGenericArray(component.getVM().getHeap());
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static VMType<VMArray> ofGenericArray(@NotNull VMHeap heap)
    {
        return (VMType<VMArray>) heap.getType("[Ljava/lang/Object;");
    }

    @SuppressWarnings("unchecked")
    public static <T extends VMValue> VMType<T> of(@NotNull VMComponent component, @NotNull TypeDescriptor desc)
    {
        JalVM vm = component.getVM();
        if (desc.getBaseType().isPrimitive() && !desc.isArray())
            return (VMType<T>) VMType.of(vm, (PrimitiveTypes) desc.getBaseType());

        VMType<?> type;
        if ((type = vm.getHeap().getType(desc.toString())) != null)
            return (VMType<T>) type;

        VMType<?> created = new VMType<>(vm, desc.getBaseType(), null);
        for (int i = 0; i < desc.getArrayDimensions(); i++)
            created = new VMType<>(vm, created.getType(), created);

        vm.getHeap().addType(created);
        return (VMType<T>) created;
    }

    public VMValue defaultValue()
    {
        // 非プリミティブまたは配列は「参照型」であるから， null
        if (this.type instanceof ClassReferenceType)
            return new VMNull<>(this);

        // プリミティブ型のデフォルト値を返す
        return switch ((PrimitiveTypes) this.type)
        {
            case BOOLEAN -> VMBoolean.ofFalse(this);
            case BYTE -> VMByte.ofZero(this);
            case CHAR -> VMChar.ofZero(this);
            case SHORT -> VMShort.ofZero(this);
            case INT -> VMInteger.ofZero(this);
            case LONG -> VMLong.ofZero(this);
            case FLOAT -> VMFloat.ofZero(this);
            case DOUBLE -> VMDouble.ofZero(this);
            case VOID -> VMVoid.instance(this);
        };
    }

    public void link(@NotNull JalVM vm)
    {
        if (this.isPrimitive)
        {
            PrimitiveTypes type = (PrimitiveTypes) this.type;
            this.linkedClass = new VMPrimitiveClass(vm, this, type);
            this.linkedClass.link(vm);
        }
        else if (this.componentType != null)
        {
            VMClass arrayClass;
            VMType<?> arrayType;
            if ((arrayType = vm.getHeap().getType(this.getTypeDescriptor())) == null)
            {
                arrayType = arrayClass = new VMArrayClass(vm, this, this.componentType.getLinkedClass());
                vm.getHeap().addType(arrayClass);
            }
            this.linkedClass = arrayType.getLinkedClass();
            this.componentType.link(vm);
        }
        else if (this.linkedClass == null)
        {
            assert this.type instanceof ClassReferenceType;
            ClassReferenceType classRefType = (ClassReferenceType) this.type;
            VMSystemClassLoader cl = vm.getClassLoader();
            this.linkedClass = cl.findClass(ClassReference.of(classRefType));
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof VMType<?> that))
            return false;
        if (this == that)
            return true;

        // 型と配列次元数が同じなら等価
        return Objects.equals(this.type, that.type) &&
                Objects.equals(this.componentType, that.componentType);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.type, this.componentType);
    }

    public boolean isAssignableFrom(@NotNull VMType<?> other)
    {
        // 同じ型なら代入可能
        if (this.type.equals(other.type))
            return true;
        else if (other.equals(VMType.of(other.vm, PrimitiveTypes.VOID)))
            return true;  // Object/Void型はどんな型からも代入可能

        if (this.getType() instanceof ClassReferenceType refType && refType.equals(ClassReferenceType.OBJECT))
            return true;  // Object型はどんな型からも代入可能

        // プリミティブ型同士の互換性チェック
        boolean isBothPrimitive = this.isPrimitive && other.isPrimitive;
        if (isBothPrimitive)
        {
            // 配列型は同じ次元数であれば互換性あり
            if (Objects.equals(this.componentType, other.componentType))
                return false;
            return this.isAssignableFromPrimitive((PrimitiveTypes) other.type);
        }

        // 参照型の互換性チェック
        return this.linkedClass.isAssignableFrom(other.linkedClass);
    }

    private boolean isAssignableFromPrimitive(@NotNull PrimitiveTypes other)
    {
        if (this.type == other)
            return true;

        return switch ((PrimitiveTypes) this.type)
        {
            case SHORT -> other == PrimitiveTypes.BYTE;
            case INT -> other == PrimitiveTypes.BYTE
                    || other == PrimitiveTypes.SHORT
                    || other == PrimitiveTypes.CHAR;
            case LONG -> other == PrimitiveTypes.BYTE
                    || other == PrimitiveTypes.SHORT
                    || other == PrimitiveTypes.CHAR
                    || other == PrimitiveTypes.INT;
            case FLOAT -> other == PrimitiveTypes.BYTE
                    || other == PrimitiveTypes.SHORT
                    || other == PrimitiveTypes.CHAR
                    || other == PrimitiveTypes.INT
                    || other == PrimitiveTypes.LONG;
            case DOUBLE -> other == PrimitiveTypes.BYTE
                    || other == PrimitiveTypes.SHORT
                    || other == PrimitiveTypes.CHAR
                    || other == PrimitiveTypes.INT
                    || other == PrimitiveTypes.LONG
                    || other == PrimitiveTypes.FLOAT;
            case BOOLEAN, VOID, CHAR, BYTE -> false;
        };
    }

    @NotNull
    public String getTypeDescriptor()
    {
        StringBuilder sb = new StringBuilder();
        VMType<?> current = this.componentType;
        while (current != null)
        {
            sb.append('[');
            current = current.componentType;
        }
        sb.append(this.type.getDescriptor());
        return sb.toString();
    }

    public VMObject createInstance()
    {
        if (this.linkedClass == null)
            throw new VMPanic("Class must be linked before creating an instance.");
        return this.linkedClass.createInstance();
    }

    public static <T extends VMValue> VMType<T> of(@NotNull VMComponent component, @NotNull String typeDescriptor)
    {
        return of(component, TypeDescriptor.parse(typeDescriptor));
    }

    public static VMType<VMObject> ofClassName(@NotNull VMComponent component, @NotNull String className)
    {
        return of(component, TypeDescriptor.className(className));
    }


    public static VMType<? extends VMPrimitive> getPrimitiveType(@NotNull VMComponent component, @NotNull String name)
    {
        return of(component, switch (name)
        {
            case "void" -> PrimitiveTypes.VOID;
            case "boolean" -> PrimitiveTypes.BOOLEAN;
            case "byte" -> PrimitiveTypes.BYTE;
            case "char" -> PrimitiveTypes.CHAR;
            case "short" -> PrimitiveTypes.SHORT;
            case "int" -> PrimitiveTypes.INT;
            case "long" -> PrimitiveTypes.LONG;
            case "float" -> PrimitiveTypes.FLOAT;
            case "double" -> PrimitiveTypes.DOUBLE;
            default -> throw new VMPanic("Unknown primitive type: " + name);
        });
    }

    public static VMType<?> convertASMType(@NotNull VMComponent component, @NotNull org.objectweb.asm.Type type)
    {
        JalVM vm = component.getVM();
        return switch (type.getSort())
        {
            case org.objectweb.asm.Type.VOID -> VMType.of(vm, PrimitiveTypes.VOID);
            case org.objectweb.asm.Type.BOOLEAN -> VMType.of(vm, PrimitiveTypes.BOOLEAN);
            case org.objectweb.asm.Type.CHAR -> VMType.of(vm, PrimitiveTypes.CHAR);
            case org.objectweb.asm.Type.BYTE -> VMType.of(vm, PrimitiveTypes.BYTE);
            case org.objectweb.asm.Type.SHORT -> VMType.of(vm, PrimitiveTypes.SHORT);
            case org.objectweb.asm.Type.INT -> VMType.of(vm, PrimitiveTypes.INT);
            case org.objectweb.asm.Type.FLOAT -> VMType.of(vm, PrimitiveTypes.FLOAT);
            case org.objectweb.asm.Type.LONG -> VMType.of(vm, PrimitiveTypes.LONG);
            case org.objectweb.asm.Type.DOUBLE -> VMType.of(vm, PrimitiveTypes.DOUBLE);
            case org.objectweb.asm.Type.ARRAY -> {
                org.objectweb.asm.Type elemType = type.getElementType();
                VMType<?> arrayType = convertASMType(vm, elemType);
                for (int i = 0; i < type.getDimensions(); i++)
                    arrayType = new VMType<>(vm, arrayType.getType(), arrayType);
                yield arrayType;
            }
            case org.objectweb.asm.Type.OBJECT -> VMType.ofClassName(vm, type.getInternalName());
            default -> throw new VMPanic("Unsupported ASM type: " + type);
        };
    }

    public boolean isLinked()
    {
        return this.linkedClass != null;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        VMType<?> current = this.componentType;
        while (current != null)
        {
            sb.append("[");
            current = current.componentType;
        }
        sb.append(this.type.getDescriptor());
        return sb.toString();
    }
}
