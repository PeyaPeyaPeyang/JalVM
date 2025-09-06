package tokyo.peya.langjal.vm;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.references.ClassReference;

import java.nio.file.Path;
import java.util.List;

@Value
@Builder
public class VMConfiguration
{
    @NotNull
    ClassReference mainClass;
    @Singular
    List<String> mainArgs;

    @Singular
    List<Path> classPaths;

    boolean enableAssertions;

    boolean debugVM;

    public static VMConfigurationBuilder builder(@NotNull ClassReference mainClass)
    {
        return new VMConfigurationBuilder().mainClass(mainClass);
    }
}
