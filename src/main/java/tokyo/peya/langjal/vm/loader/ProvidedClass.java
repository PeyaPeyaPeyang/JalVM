package tokyo.peya.langjal.vm.loader;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public record ProvidedClass(
        byte @NotNull [] bytecode,
        @NotNull Path containingPath,
        @NotNull Path internalPath,  // ZIP とかに入っていた場合は，そのパス
        @NotNull ClassProvider provider  // どのプロバイダから提供されたか
)
{
}
