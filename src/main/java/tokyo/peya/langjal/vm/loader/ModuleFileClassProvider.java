package tokyo.peya.langjal.vm.loader;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.references.ClassReference;

import java.nio.file.Path;

public class ModuleFileClassProvider extends AbstractZIPFileClassProvider
{
    private static final Path BASE_PATH = Path.of("classes");

    public ModuleFileClassProvider(@NotNull Path archivePath)
    {
        super(archivePath);
    }

    @Override
    protected @NotNull Path getInternalPath(@NotNull ClassReference reference)
    {
        return BASE_PATH.resolve(reference.getFilePath());
    }
}
