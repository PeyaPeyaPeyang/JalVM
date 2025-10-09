package tokyo.peya.langjal.vm.loader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.references.ClassReference;

import java.nio.file.Files;
import java.nio.file.Path;

public class JARFileClassProvider extends AbstractZIPFileClassProvider
{
    public JARFileClassProvider(@NotNull Path archivePath)
    {
        super(archivePath);
    }

    @Override
    protected @NotNull Path getInternalPath(@NotNull ClassReference reference)
    {
        return reference.getFilePath();
    }
}
