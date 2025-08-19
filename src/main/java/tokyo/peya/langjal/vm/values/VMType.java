package tokyo.peya.langjal.vm.values;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.ClassReferenceType;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.compiler.jvm.Type;
import tokyo.peya.langjal.compiler.jvm.TypeDescriptor;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMPrimitiveClass;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;

import java.util.Objects;

@Getter
public class VMType<T extends VMValue>
{
    public static final VMType<VMVoid> VOID = new VMType<>(PrimitiveTypes.VOID);
    public static final VMType<VMBoolean> BOOLEAN = new VMType<>(PrimitiveTypes.BOOLEAN);
    public static final VMType<VMByte> BYTE = new VMType<>(PrimitiveTypes.BYTE);
    public static final VMType<VMChar> CHAR = new VMType<>(PrimitiveTypes.CHAR);
    public static final VMType<VMShort> SHORT = new VMType<>(PrimitiveTypes.SHORT);
    public static final VMType<VMInteger> INTEGER = new VMType<>(PrimitiveTypes.INT);
    public static final VMType<VMLong> LONG = new VMType<>(PrimitiveTypes.LONG);
    public static final VMType<VMFloat> FLOAT = new VMType<>(PrimitiveTypes.FLOAT);
    public static final VMType<VMDouble> DOUBLE = new VMType<>(PrimitiveTypes.DOUBLE);

    // プリミティブではないが，よく使うため，
    public static final VMType<VMReferenceValue> GENERIC_OBJECT = VMType.of(TypeDescriptor.className("java/lang/Object"));
    public static final VMType<VMArray> GENERIC_ARRAY = VMType.of(TypeDescriptor.parse("[Ljava/lang/Object;"));
    public static final VMType<VMObject> STRING = VMType.of(TypeDescriptor.className("java/lang/String"));

    public static void initialiseWellKnownClasses(@NotNull VMSystemClassLoader classLoader)
    {
        // ここでよく使うクラスをリンクしておく
        GENERIC_OBJECT.linkClass(classLoader);
        STRING.linkClass(classLoader);
        GENERIC_ARRAY.linkClass(classLoader);
    }


    private final Type type;
    private final int arrayDimensions;
    private final boolean isPrimitive;  // 配列でも基底タイプがプリミティブなら true

    private VMClass linkedClass;

    public VMType(@NotNull Type type, int arrayDimensions)
    {
        if (type instanceof PrimitiveTypes && arrayDimensions == 0)
            throw new VMPanic("type can not be a PrimitiveTypes instance directly. Use VMType.PRIMITIVE_NAME instead.");

        this.type = type;
        this.arrayDimensions = arrayDimensions;
        this.isPrimitive = type.isPrimitive();
        if (type instanceof PrimitiveTypes primitive)
            this.linkedClass = new VMPrimitiveClass(this, primitive.getName());
    }

    private VMType(@NotNull PrimitiveTypes type)
    {
        this.type = type;
        this.arrayDimensions = 0;
        this.isPrimitive = true;
        this.linkedClass = new VMPrimitiveClass(this, type.getName());
    }

    public VMType(@NotNull ClassReference reference)
    {
        this.type = ClassReferenceType.parse(reference.getFullQualifiedName());
        this.arrayDimensions = 0;
        this.isPrimitive = false;
    }

    @SuppressWarnings("unchecked")
    public static <T extends VMValue> VMType<T> of(@NotNull TypeDescriptor desc)
    {
        if (desc.getBaseType().isPrimitive() && !desc.isArray())
            return switch (desc.getBaseType())
            {
                case PrimitiveTypes.BOOLEAN -> (VMType<T>) BOOLEAN;
                case PrimitiveTypes.BYTE -> (VMType<T>) BYTE;
                case PrimitiveTypes.CHAR -> (VMType<T>) CHAR;
                case PrimitiveTypes.SHORT -> (VMType<T>) SHORT;
                case PrimitiveTypes.INT -> (VMType<T>) INTEGER;
                case PrimitiveTypes.LONG -> (VMType<T>) LONG;
                case PrimitiveTypes.FLOAT -> (VMType<T>) FLOAT;
                case PrimitiveTypes.DOUBLE -> (VMType<T>) DOUBLE;
                case PrimitiveTypes.VOID -> (VMType<T>) VOID;
                default -> throw new VMPanic("Unsupported primitive type: " + desc.getBaseType());
            };

        return new VMType<>(desc.getBaseType(), desc.getArrayDimensions());
    }

    public VMValue defaultValue()
    {
        // 非プリミティブまたは配列は「参照型」であるから， null
        if (this.type instanceof ClassReferenceType || this.arrayDimensions > 0)
            return new VMNull<>(this);

        // プリミティブ型のデフォルト値を返す
        return switch ((PrimitiveTypes) this.type)
        {
            case BOOLEAN -> VMBoolean.FALSE;
            case BYTE -> VMByte.ZERO;
            case CHAR -> VMChar.ZERO;
            case SHORT -> VMShort.ZERO;
            case INT -> VMInteger.ZERO;
            case LONG -> VMLong.ZERO;
            case FLOAT -> VMFloat.ZERO;
            case DOUBLE -> VMDouble.ZERO;
            case VOID -> VMVoid.INSTANCE;
        };
    }

    public void linkClass(@NotNull VMSystemClassLoader cl)
    {
        if (this.isPrimitive)
            return;  // プリミティブ型はリンク不要

        if (this.linkedClass == null)
        {
            ClassReferenceType classRefType = (ClassReferenceType) this.type;
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
                this.arrayDimensions == that.arrayDimensions;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.type, this.arrayDimensions);
    }

    public boolean isAssignableFrom(@NotNull VMType<?> other)
    {
        // 同じ型なら代入可能
        if (this.type.equals(other.type))
            return true;
        else if (other.equals(VMType.VOID))
            return true;  // Void型はどんな型からも代入可能

        // プリミティブ型同士の互換性チェック
        boolean isBothPrimitive = this.isPrimitive && other.isPrimitive;
        if (isBothPrimitive)
        {
            // 配列型は同じ次元数であれば互換性あり
            if (this.arrayDimensions != other.arrayDimensions)
                return false;
            return this.isAssignableFromPrimitive((PrimitiveTypes) other.type);
        }

        // 参照型同士の互換性チェック
        if (this.linkedClass == null || other.linkedClass == null)
        {
            // どちらかがリンクされていない場合はリンクを試みる
            this.linkOthers(other);
            if (this.linkedClass == null || other.linkedClass == null)
                return false;  // リンクに失敗した場合は互換性なし
        }

        // 参照型の互換性チェック
        return other.linkedClass.isSubclassOf(this.linkedClass);
    }

    private void linkOthers(@NotNull VMType<?> other)
    {
        VMClass linkedClass = this.linkedClass == null ? other.linkedClass : this.linkedClass;
        if (linkedClass == null)
            return;

        VMSystemClassLoader cl = linkedClass.getClassLoader();
        if (cl == null)
            return;

        this.linkClass(cl);
        other.linkClass(cl);
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
        return "[".repeat(Math.max(0, this.arrayDimensions)) +
                this.type.getDescriptor();
    }

    public VMObject createInstance()
    {
        if (this.linkedClass == null)
            throw new VMPanic("Class must be linked before creating an instance.");
        return this.linkedClass.createInstance();
    }

    public static <T extends VMValue> VMType<T> of(@NotNull String typeDescriptor)
    {
        return of(TypeDescriptor.parse(typeDescriptor));
    }

    public static VMType<VMObject> ofClassName(@NotNull String className)
    {
        return of(TypeDescriptor.className(className));
    }

    public static VMType<? extends VMPrimitive> getPrimitiveType(@NotNull String name)
    {
        return switch (name)
        {
            case "void" -> VOID;
            case "boolean" -> BOOLEAN;
            case "byte" -> BYTE;
            case "char" -> CHAR;
            case "short" -> SHORT;
            case "int" -> INTEGER;
            case "long" -> LONG;
            case "float" -> FLOAT;
            case "double" -> DOUBLE;
            default -> throw new VMPanic("Unknown primitive type: " + name);
        };
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(this.type.getDescriptor());
        if (this.arrayDimensions > 0)
            sb.append("[]".repeat(this.arrayDimensions));
        return sb.toString();
    }
}
