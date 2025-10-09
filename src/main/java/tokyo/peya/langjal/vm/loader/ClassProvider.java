package tokyo.peya.langjal.vm.loader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.references.ClassReference;

import java.nio.file.Path;
import java.util.Enumeration;

public interface ClassProvider
{
    @Nullable
    ProvidedClass findClass(@NotNull ClassReference reference);
}
