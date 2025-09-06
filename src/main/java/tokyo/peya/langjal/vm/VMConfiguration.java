package tokyo.peya.langjal.vm;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.nio.file.Path;
import java.util.List;

@Value
@Builder
public class VMConfiguration
{
    @Singular
    List<Path> classPaths;

    boolean enableAssertions;
}
