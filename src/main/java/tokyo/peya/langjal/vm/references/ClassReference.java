package tokyo.peya.langjal.vm.references;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import tokyo.peya.langjal.compiler.jvm.ClassReferenceType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ClassReference {
    private static final ClassReference EMPTY = new ClassReference(new String[0], "");

    private final String[] packages;
    private final String className;

    private ClassReference(String[] packages, String className) {
        this.packages = normalizePackages(packages);
        this.className = className;
    }

    public static ClassReference of(String className) {
        if (className == null || className.isEmpty())
            return EMPTY;

        String[] parts = className.replace('/', '.').split("\\.");
        String[] packages = new String[parts.length - 1];
        System.arraycopy(parts, 0, packages, 0, parts.length - 1);
        return new ClassReference(packages, parts[parts.length - 1]);
    }

    public static ClassReference of(String packageName, String className) {
        if (className == null || className.isEmpty())
            return EMPTY;

        String[] packages = packageName == null ? new String[0] : packageName.replace('/', '.').split("\\.");
        return new ClassReference(packages, className);
    }

    public static ClassReference of(ClassNode classNode) {
        if (classNode == null || classNode.name == null || classNode.name.isEmpty())
            return EMPTY;

        return of(classNode.name);
    }

    public static ClassReference of(@NotNull ClassReferenceType type) {
        return of(type.getInternalName());
    }

    private static String[] normalizePackages(String[] packages) {
        if (packages == null || packages.length == 0)
            return new String[0];

        List<String> result = new ArrayList<>(packages.length);
        for (String pkg : packages)
            if (!(pkg == null || pkg.isEmpty()))
                result.add(pkg);

        return result.toArray(new String[0]);
    }

    private static String concat(String a, String b, String delimiter) {
        if (a == null || a.isEmpty())
            return b;
        else if (b == null || b.isEmpty())
            return a;
        else
            return a + delimiter + b;
    }

    public String getPackageDotName() {
        return String.join(".", this.packages);
    }

    public String getPackage() {
        return String.join("/", this.packages);
    }

    public String getFullQualifiedName() {
        return concat(this.getPackage(), this.className, "/");
    }

    public String getFullQualifiedDotName() {
        return concat(this.getPackageDotName(), this.className, ".");
    }

    public String getFileName() {
        return this.className + ".class";
    }

    public String getFileNameFull() {
        return concat(this.getPackage(), this.getFileName(), "/");
    }

    public Path getFilePath() {
        return Path.of(this.getFileNameFull());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ClassReference that))
            return false;
        else if (this == that)
            return true;
        return Objects.deepEquals(this.packages, that.packages)
                && Objects.equals(this.className, that.className);
    }

    public boolean isEqualClassName(@NotNull String className) {
        return Objects.equals(this.className, className);
    }

    public boolean isEqualPackage(@Nullable String packageName) {
        return Objects.equals(this.getPackage(), packageName);
    }
    public boolean isEqualPackage(@NotNull ClassReference otherClass) {
        return Objects.equals(this.getPackage(), otherClass.getPackage());
    }

    public boolean isEqualClass(@NotNull String fullQualifiedName) {
        return Objects.equals(this.getFullQualifiedName(), fullQualifiedName.replace('.', '/'));
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(this.packages), this.className);
    }

    @Override
    public String toString() {
        return this.getFullQualifiedDotName();
    }
}
