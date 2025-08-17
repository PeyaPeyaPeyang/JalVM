package tokyo.peya.langjal.vm.tracing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.values.VMValue;

public record ValueTracingEntry(
        @NotNull VMValue value,
        @NotNull ValueManipulationType type,
        @NotNull VMMethod inMethod,
        @Nullable AbstractInsnNode manipulatingInstruction,
        @Nullable VMValue combinationValue,
        @Nullable VMValue combinationValue2,

        @Nullable VMField field
)
{
    public static ValueTracingEntry generation(@NotNull VMValue value,
                                               @NotNull VMMethod method, @NotNull AbstractInsnNode instruction)
    {
        return new ValueTracingEntry(
                value,
                ValueManipulationType.GENERATION,
                method,
                instruction,
                null,
                null,
                null
        );
    }

    public static ValueTracingEntry manipulation(@NotNull VMValue result,
                                                 @NotNull VMValue before,
                                                 @NotNull VMMethod method,
                                                 @Nullable AbstractInsnNode instruction)
    {
        return new ValueTracingEntry(
                result,
                ValueManipulationType.MANIPULATION,
                method,
                instruction,
                null,
                before,
                null
        );
    }

    public static ValueTracingEntry combination(@NotNull VMValue value,
                                                @NotNull VMMethod method,
                                                @NotNull AbstractInsnNode instruction,
                                                @NotNull VMValue combinationValue,
                                                @Nullable VMValue combinationValue2)
    {
        return new ValueTracingEntry(
                value,
                ValueManipulationType.COMBINATION,
                method,
                instruction,
                combinationValue,
                combinationValue2,
                null
        );
    }

    public static ValueTracingEntry fieldAccess(@NotNull VMValue value,
                                                @NotNull VMMethod method,
                                                @Nullable VMValue object,
                                                @NotNull AbstractInsnNode instruction,
                                                @NotNull VMField field)
    {
        return new ValueTracingEntry(
                value,
                ValueManipulationType.FIELD_GET,
                method,
                instruction,
                object,
                null,
                field
        );
    }

    public static ValueTracingEntry fieldSet(@NotNull VMValue value,
                                             @NotNull VMMethod method,
                                             @NotNull AbstractInsnNode instruction,
                                             @NotNull VMField field)
    {
        return new ValueTracingEntry(
                value,
                ValueManipulationType.MANIPULATION,
                method,
                instruction,
                null,
                null,
                field
        );
    }

    public static ValueTracingEntry passing(@NotNull VMValue value,
                                            @NotNull VMMethod method)
    {
        return new ValueTracingEntry(
                value,
                ValueManipulationType.PASSING_AS_ARGUMENT,
                method,
                null,
                null,
                null,
                null
        );
    }

    public static ValueTracingEntry returning(@NotNull VMValue value,
                                              @NotNull VMMethod method,
                                              @Nullable AbstractInsnNode instruction)
    {
        return new ValueTracingEntry(
                value,
                ValueManipulationType.RETURNING_FROM,
                method,
                instruction,
                null,
                null,
                null
        );
    }

    public static ValueTracingEntry destruction(@NotNull VMValue value,
                                                @NotNull VMMethod method,
                                                @Nullable AbstractInsnNode instruction)
    {
        return new ValueTracingEntry(
                value,
                ValueManipulationType.DESTRUCTION,
                method,
                instruction,
                null,
                null,
                null
        );
    }

    public static ValueTracingEntry localSet(@NotNull VMValue value,
                                             @NotNull VMMethod method,
                                             @Nullable AbstractInsnNode instruction)
    {
        return new ValueTracingEntry(
                value,
                ValueManipulationType.TO_LOCAL,
                method,
                instruction,
                null,
                null,
                null
        );
    }

    public static ValueTracingEntry localGet(@NotNull VMValue value,
                                             @NotNull VMMethod method,
                                             @Nullable AbstractInsnNode instruction)
    {
        return new ValueTracingEntry(
                value,
                ValueManipulationType.TO_LOCAL,
                method,
                instruction,
                null,
                null,
                null
        );
    }
}
