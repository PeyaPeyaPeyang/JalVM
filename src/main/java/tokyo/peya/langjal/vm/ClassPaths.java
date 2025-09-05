package tokyo.peya.langjal.vm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ClassPaths
{
    private final List<Path> classPaths;
    private final Map<Path, ZipFile> zipFileHandles;
    private final Map<String, ZipEntryCache> classEntries;

    public ClassPaths()
    {
        this.classPaths = new ArrayList<>();
        this.zipFileHandles = new HashMap<>();
        this.classEntries = new HashMap<>();

        this.addDefaultPackages();
    }

    private void addDefaultPackages()
    {
        // 今の Java の標準ライブラリのパッケージを追加
        this.addClassPath(Path.of(System.getProperty("java.home"), "lib"));
        this.addClassPath(Path.of(System.getProperty("java.home"), "jmods"));
    }

    public void addClassPath(Path classPath)
    {
        if (!(classPath == null || classPath.toString().isEmpty()))
            this.classPaths.add(classPath);
    }

    public void removeClassPath(Path classPath)
    {
        this.classPaths.remove(classPath);
    }

    public List<Path> getClassPaths()
    {
        return Collections.unmodifiableList(this.classPaths);
    }

    public byte @Nullable [] findClassBytes(@NotNull ClassReference reference)
    {
        byte[] classBytes = findClassBytesFromClassPaths(reference);
        if (classBytes != null)
            return classBytes;

        if (this.zipFileHandles.isEmpty())
            this.collectZipFiles();

        String classFilePath = reference.getFileNameFull();
        if (!this.classEntries.containsKey(classFilePath))
            return null;

        ZipEntryCache entry = this.classEntries.get(classFilePath);
        try (InputStream inputStream = entry.zipFile.getInputStream(entry.entry))
        {
            return inputStream.readAllBytes();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] findClassBytesFromClassPaths(@NotNull ClassReference reference)
    {
        for (Path path : this.classPaths)
        {
            Path classFile = path.resolve(reference.getFilePath());
            if (!Files.exists(classFile))
                continue;
            try
            {
                return Files.readAllBytes(classFile);
            }
            catch (Exception e)
            {
                throw new VMPanic("Unable to read class file: " + classFile, e);
            }
        }

        return null;
    }

    public void collectZipFiles()
    {
        this.zipFileHandles.clear();

        for (Path path : this.classPaths)
            try (Stream<Path> files = Files.walk(path))
            {
                files.filter(file -> {
                    if (!Files.isRegularFile(file))
                        return false;

                    String fileName = file.getFileName().toString();
                    return fileName.endsWith(".jar") || fileName.endsWith(".zip")
                            || fileName.endsWith(".jmod");
                }).forEach(file -> {
                    ZipFile zipFile = getZipFileHandle(file);
                    this.zipFileHandles.put(file, zipFile);
                    boolean isJMod = file.toString().endsWith(".jmod");

                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements())
                    {
                        ZipEntry entry = entries.nextElement();
                        // ディレクトリと .class 以外は無視
                        if (entry.getName().endsWith(".class"))
                        {
                            String normalizedPath = normalizeClassFilePath(entry.getName(), isJMod);
                            this.classEntries.put(normalizedPath, new ZipEntryCache(zipFile, entry));
                        }

                    }
                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
                System.out.println("Failed to collect zip files from class path: " + path
                                           + ", ignoring.");
            }
    }

    private static String normalizeClassFilePath(@NotNull String path, boolean isJMod)
    {
        if (path.startsWith("/"))
            path = path.substring(1);
        if (isJMod && path.startsWith("classes/"))
            path = path.substring("classes/".length());
        return path;
    }

    private @Nullable ZipFile getZipFileHandle(@NotNull Path path)
    {
        if (this.zipFileHandles.containsKey(path))
            return this.zipFileHandles.get(path);

        try
        {
            ZipFile zipFile = new JarFile(path.toFile());
            this.zipFileHandles.put(path, zipFile);
            return zipFile;
        }
        catch (Exception e)
        {
            System.out.println("Failed to open zip file: " + path + ", ignoring.");
            e.printStackTrace();
            return null;
        }
    }

    record ZipEntryCache(@NotNull ZipFile zipFile, @NotNull ZipEntry entry)
    {
    }
}
