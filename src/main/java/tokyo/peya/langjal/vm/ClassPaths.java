package tokyo.peya.langjal.vm;

import com.sun.jdi.VoidType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.references.ClassReference;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ClassPaths
{
    private final List<Path> classPaths;
    private final Map<Path, ZipFile> zipFileHandles;

    public ClassPaths() {
        this.classPaths = new ArrayList<>();
        this.zipFileHandles = new HashMap<>();

        this.addDefaultPackages();
    }

    private void addDefaultPackages() {
        // 今の Java の標準ライブラリのパッケージを追加
        this.addClassPath(Path.of(System.getProperty("java.home"), "lib"));
        this.addClassPath(Path.of(System.getProperty("java.home"), "jmods"));
    }

    public void addClassPath(Path classPath) {
        if (!(classPath == null || classPath.toString().isEmpty()))
            this.classPaths.add(classPath);
    }

    public void removeClassPath(Path classPath) {
        this.classPaths.remove(classPath);
    }

    public List<Path> getClassPaths() {
        return Collections.unmodifiableList(this.classPaths);
    }

    public byte @Nullable [] findClassBytes(@NotNull ClassReference reference) {
        byte[] classBytes = findClassBytesFromClassPaths(reference);
        if (classBytes != null) {
            return classBytes;
        }

        if (this.zipFileHandles.isEmpty())
            this.collectZipFiles();

        for (ZipFile zipFile : this.zipFileHandles.values()) {
            try {
                Path entryPath = reference.getFilePath();
                boolean isJMod = zipFile.getName().endsWith(".jmod");
                if (isJMod)
                    entryPath = Path.of("classes", entryPath.toString());

                ZipEntry entry;
                if ((entry = zipFile.getEntry(entryPath.toString())) != null) {
                    try (InputStream inputStream = zipFile.getInputStream(entry)) {
                        return inputStream.readAllBytes();
                    }
                }

            } catch (Exception e) {
                throw new IllegalStateException("Unable to read class from zip file: " + reference, e);
            }
        }

        return null;
    }

    private byte[] findClassBytesFromClassPaths(@NotNull ClassReference reference) {
        for (Path path : this.classPaths) {
            Path classFile = path.resolve(reference.getFilePath());
            if (Files.exists(classFile)) {
                try {
                    return Files.readAllBytes(classFile);
                } catch (Exception e) {
                    throw new IllegalStateException("Unable to read class file: " + classFile, e);
                }
            }
        }

        return null;
    }

    public void collectZipFiles() {
        this.zipFileHandles.clear();

        for (Path path : this.classPaths) {
            try (Stream<Path> files = Files.walk(path)) {
                files.filter(file -> {
                    if (!Files.isRegularFile(file))
                        return false;

                    String fileName = file.getFileName().toString();
                    return fileName.endsWith(".jar") || fileName.endsWith(".zip")
                            || fileName.endsWith(".jmod");
                }).forEach(file -> {
                    ZipFile zipFile = getZipFileHandle(file);
                    this.zipFileHandles.put(file, zipFile);
                });
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Failed to collect zip files from class path: " + path
                         + ", ignoring.");
            }
        }
    }

    private @Nullable ZipFile getZipFileHandle(@NotNull Path path) {
        if (this.zipFileHandles.containsKey(path)) {
            return this.zipFileHandles.get(path);
        }

        try {
            ZipFile zipFile = new JarFile(path.toFile());
            this.zipFileHandles.put(path, zipFile);
            return zipFile;
        } catch (Exception e) {
            System.out.println("Failed to open zip file: " + path + ", ignoring.");
            e.printStackTrace();
            return null;
        }
    }
}
