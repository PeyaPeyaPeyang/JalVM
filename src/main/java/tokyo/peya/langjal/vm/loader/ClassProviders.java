package tokyo.peya.langjal.vm.loader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassProviders
{
    private final List<Path> classPaths;
    private final Map<Path, ClassProvider> handles;

    public ClassProviders()
    {
        this.classPaths = new ArrayList<>();
        this.handles = new HashMap<>();

        this.addDefaultClassPaths();
    }

    private void addDefaultClassPaths()
    {
        this.addClassPath(Path.of(System.getProperty("java.home"), "lib"));
        this.addClassPath(Path.of(System.getProperty("java.home"), "jmods"));
    }

    private void addClassPath(@NotNull Path classpath)
    {
        if (!Files.exists(classpath))
            return;

        // ディレクトリなら，すべての JAR/JMOD を追加する
        if (Files.isDirectory(classpath))
            try (var stream = Files.list(classpath))
            {
                stream.forEach(this::addClassPathFile);
            }
            catch (Exception e)
            {
                throw new VMPanic("Failed to list classpath directory: " + classpath, e);
            }
        else
            this.addClassPathFile(classpath);
    }

    private void addClassPathFile(@NotNull Path classpathFile)
    {
        if (!Files.isRegularFile(classpathFile))
            return;

        String filename = classpathFile.getFileName().toString().toLowerCase();
        if (filename.endsWith(".jar"))
            this.handles.put(classpathFile, new JARFileClassProvider(classpathFile));
        else if (filename.endsWith(".jmod"))
            this.handles.put(classpathFile, new ModuleFileClassProvider(classpathFile));
        // ...
    }

    @Nullable
    public ProvidedClass findClass(@NotNull ClassReference reference)
    {
        for (ClassProvider handle : this.handles.values())
        {
            ProvidedClass found = handle.findClass(reference);
            if (found != null)
                return found;
        }

        return null;
    }
}
