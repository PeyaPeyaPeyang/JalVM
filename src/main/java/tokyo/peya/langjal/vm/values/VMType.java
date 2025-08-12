package tokyo.peya.langjal.vm.values;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.ClassReferenceType;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.compiler.jvm.Type;
import tokyo.peya.langjal.compiler.jvm.TypeDescriptor;
import tokyo.peya.langjal.vm.VMClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.references.ClassReference;

@Getter
public class VMType {
    private final Type type;
    private final int arrayDimensions;
    private final boolean isPrimitive;  // 配列でも基底タイプがプリミティブなら true

    private VMClass linkedClass;

    public VMType(@NotNull Type type, int arrayDimensions) {
        this.type = type;
        this.arrayDimensions = arrayDimensions;
        this.isPrimitive = type.isPrimitive();
    }

    public VMType(@NotNull String typeDescriptor) {
        this(TypeDescriptor.parse(typeDescriptor));
    }

    public VMType(@NotNull TypeDescriptor desc) {
        this.type = desc.getBaseType();
        this.arrayDimensions = desc.getArrayDimensions();
        this.isPrimitive = type.isPrimitive();
    }

    public VMValue defaultValue() {
        // 非プリミティブまたは配列は「参照型」であるから， null
        if (!(this.isPrimitive || this.arrayDimensions > 1))
            return new VMNull(this.type);

        // プリミティブ型のデフォルト値を返す
        return switch ((PrimitiveTypes) this.type) {
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

    @NotNull
    public VMClass linkClass(@NotNull VMClassLoader cl) {
        if (this.isPrimitive)
            throw new UnsupportedOperationException("Cannot link a primitive type: " + this.type);

        if (this.linkedClass == null) {
            ClassReferenceType classRefType = (ClassReferenceType) this.type;
            this.linkedClass = cl.findClass(ClassReference.of(classRefType));
        }

        return this.linkedClass;
    }
}
