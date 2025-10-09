package tokyo.peya.langjal.vm.loader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public abstract class AbstractZIPFileClassProvider implements ClassProvider
{
    private final Path archivePath;
    private final List<ZipEntry> entryCache;
    protected ZipFile archiveHandle;

    public AbstractZIPFileClassProvider(@NotNull Path archivePath)
    {
        this.archivePath = archivePath;
        this.entryCache = new ArrayList<>();
    }

    private void cacheHandle()
    {
        try
        {
            this.archiveHandle = new ZipFile(this.archivePath.toFile());
            this.entryCache.addAll(this.archiveHandle.stream().toList());
        }
        catch (IOException e)
        {
            throw new VMPanic("Failed to open archive: " + this.archivePath, e);
        }
    }

    @NotNull
    protected abstract Path getInternalPath(@NotNull ClassReference reference);

    @Override
    @Nullable
    public ProvidedClass findClass(@NotNull ClassReference reference)
    {
        if (this.archiveHandle == null)
            this.cacheHandle();

        // ZIP 内のパスを取得
        Path internalPath = this.getInternalPath(reference);
        String internalPathStr = internalPath.toString()
                .replace('\\', '/'); // ZIP 内は常に '/' で区切られる

        // エントリを絞り込み
        ZipEntry targetEntry = this.entryCache.stream()
                                              .filter(entry -> entry.getName().equals(internalPathStr))
                                              .findFirst()
                                              .orElse(null);

        if (targetEntry == null)
            return null;

        try
        {
            byte[] bytecode;
            try (InputStream inputStream = this.archiveHandle.getInputStream(targetEntry))
            {
                bytecode = inputStream.readAllBytes();
            }

            return new ProvidedClass(
                    bytecode,
                    this.archivePath,
                    internalPath,
                    this
            );
        }
        catch (IOException e)
        {
            throw new VMPanic("Failed to read class from archive: " + this.archivePath + " at " + internalPath, e);
        }
    }
}
