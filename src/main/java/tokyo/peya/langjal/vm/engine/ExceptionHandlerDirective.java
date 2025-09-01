package tokyo.peya.langjal.vm.engine;

import org.jetbrains.annotations.Nullable;

public record ExceptionHandlerDirective(
    int startInstructionIndex,
    int endInstructionIndex,
    int handlerInstructionIndex,
    @Nullable
    VMClass exceptionType
)
{
}
