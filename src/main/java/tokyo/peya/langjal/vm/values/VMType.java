package tokyo.peya.langjal.vm.values;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.ClassReferenceType;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.compiler.jvm.Type;
import tokyo.peya.langjal.compiler.jvm.TypeDescriptor;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;

import java.util.Objects;

@Getter
public class VMType
{
    public static final VMType VOID = new VMType(PrimitiveTypes.VOID);
    public static final VMType BOOLEAN = new VMType(PrimitiveTypes.BOOLEAN);
    public static final VMType BYTE = new VMType(PrimitiveTypes.BYTE);
    public static final VMType CHAR = new VMType(PrimitiveTypes.CHAR);
    public static final VMType SHORT = new VMType(PrimitiveTypes.SHORT);
    public static final VMType INTEGER = new VMType(PrimitiveTypes.INT);
    public static final VMType LONG = new VMType(PrimitiveTypes.LONG);
    public static final VMType FLOAT = new VMType(PrimitiveTypes.FLOAT);
    public static final VMType DOUBLE = new VMType(PrimitiveTypes.DOUBLE);

    // プリミティブではないが，よく使うため，
    public static final VMType STRING = new VMType(TypeDescriptor.className("java/lang/String"));

    private final Type type;
    private final int arrayDimensions;
    private final boolean isPrimitive;  // 配列でも基底タイプがプリミティブなら true

    private VMClass linkedClass;

    public VMType(@NotNull Type type, int arrayDimensions)
    {
        if (type instanceof PrimitiveTypes)
            throw new VMPanic("type can not be a PrimitiveTypes instance directly. Use VMType.PRIMITIVE_NAME instead.");

        this.type = type;
        this.arrayDimensions = arrayDimensions;
        this.isPrimitive = type.isPrimitive();
    }

    private VMType(@NotNull PrimitiveTypes type)
    {
        this.type = type;
        this.arrayDimensions = 0;
        this.isPrimitive = true;
    }

    public VMType(@NotNull ClassReference reference)
    {
        this(TypeDescriptor.className(reference.getFullQualifiedName()));
    }

    public VMType(@NotNull TypeDescriptor desc)
    {
        this.type = desc.getBaseType();
        this.arrayDimensions = desc.getArrayDimensions();
        this.isPrimitive = this.type.isPrimitive();
    }

    public VMValue defaultValue()
    {
        // 非プリミティブまたは配列は「参照型」であるから， null
        if (this.type instanceof ClassReferenceType || this.arrayDimensions > 0)
            return new VMNull(this);

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
        if (!(obj instanceof VMType that))
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

    public boolean isAssignableFrom(@NotNull VMType other)
    {
        // 同じ型なら代入可能
        if (this.equals(other))
            return true;

        // プリミティブ型同士の互換性チェック
        boolean isBothPrimitive = this.isPrimitive && other.isPrimitive;
        if (isBothPrimitive)
        {
            // 配列型は同じ次元数であれば互換性あり
            if (this.arrayDimensions != other.arrayDimensions)
                return false;
            return this.isAssignableFromPrimitive((PrimitiveTypes) other.type);
        }

        // 片方がプリミティブ型で他方が参照型の場合は互換性なし
        if (this.isPrimitive || other.isPrimitive)
            return false;

        // 参照型同士の互換性チェック
        if (this.linkedClass == null || other.linkedClass == null)
            return false;

        // 参照型の互換性チェック
        return this.linkedClass.isSubclassOf(other.linkedClass);
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

    public static VMType ofTypeDescriptor(@NotNull String typeDescriptor)
    {
        return new VMType(TypeDescriptor.parse(typeDescriptor));
    }

    public static VMType ofClassName(@NotNull String className)
    {
        return new VMType(TypeDescriptor.className(className));
    }
}
