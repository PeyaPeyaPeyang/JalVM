package tokyo.peya.langjal.vm.references;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import tokyo.peya.langjal.compiler.jvm.ClassReferenceType;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ClassReference
{
    public static final ClassReference EMPTY = new ClassReference(new String[0], "");
    public static final ClassReference OBJECT = new ClassReference(new String[]{"java", "lang"}, "Object");

    private static final Map<String, ClassReference> CACHES;

    private final String[] packages;
    private final String className;

    static {
        CACHES = new LinkedHashMap<>(512, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ClassReference> eldest) {
                return this.size() >= 512;
            }
        };
    }

    private ClassReference(String[] packages, String className)
    {
        this.packages = normalizePackages(packages);
        this.className = className;
    }

    public String getPackageDotName()
    {
        return String.join(".", this.packages);
    }

    public String getPackage()
    {
        return String.join("/", this.packages);
    }

    public String getFullQualifiedName()
    {
        return concat(this.getPackage(), this.className, "/");
    }

    public String getFullQualifiedDotName()
    {
        return concat(this.getPackageDotName(), this.className, ".");
    }

    public String getFileName()
    {
        return this.className + ".class";
    }

    public String getFileNameFull()
    {
        return concat(this.getPackage(), this.getFileName(), "/");
    }

    public Path getFilePath()
    {
        return Path.of(this.getFileNameFull());
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof ClassReference that))
            return false;
        else if (this == that)
            return true;
        return Objects.deepEquals(this.packages, that.packages)
                && Objects.equals(this.className, that.className);
    }

    public boolean isEqualClassName(@NotNull String className)
    {
        return Objects.equals(this.className, className);
    }

    public boolean isEqualPackage(@Nullable String packageName)
    {
        return Objects.equals(this.getPackage(), packageName);
    }

    public boolean isEqualPackage(@NotNull ClassReference otherClass)
    {
        return Objects.equals(this.getPackage(), otherClass.getPackage());
    }

    public boolean isEqualClass(@NotNull ClassReference otherClass)
    {
        return Objects.equals(this.getFullQualifiedName(), otherClass.getFullQualifiedName());
    }

    public boolean isEqualClass(@NotNull String fullQualifiedName)
    {
        return Objects.equals(this.getFullQualifiedName(), fullQualifiedName.replace('.', '/'));
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(Arrays.hashCode(this.packages), this.className);
    }

    @Override
    public String toString()
    {
        return this.getFullQualifiedDotName();
    }

    private static String[] splitByChar(String s, char delimiter)
    {
        // 事前にカウントしてサイズ確定
        int count = 1;
        for (int i = 0; i < s.length(); i++)
            if (s.charAt(i) == delimiter)
                count++;

        String[] result = new String[count];
        int start = 0, idx = 0;
        for (int i = 0; i < s.length(); i++)
        {
            if (s.charAt(i) == delimiter)
            {
                result[idx++] = s.substring(start, i);
                start = i + 1;
            }
        }
        result[idx] = s.substring(start);
        return result;
    }

    public static ClassReference of(String className)
    {
        if (className == null || className.isEmpty())
            return EMPTY;

        if (CACHES.containsKey(className))
            return CACHES.get(className);

        String normalisedClassName = className.replace('/', '.');
        String[] parts = splitByChar(normalisedClassName, '.');
        if (parts.length == 1)
            return new ClassReference(new String[0], parts[0]);

        String[] packages = Arrays.copyOf(parts, parts.length - 1);
        ClassReference ref = new ClassReference(packages, parts[parts.length - 1]);
        CACHES.put(className, ref);
        return ref;
    }

    public static ClassReference of(String packageName, String className)
    {
        if (className == null || className.isEmpty())
            return EMPTY;

        String normalisedPackageName = className.replace('/', '.');
        String[] packages = packageName == null ? new String[0]: splitByChar(normalisedPackageName.replace('/', '.'), '.');
        return new ClassReference(packages, className);
    }

    public static ClassReference of(ClassNode classNode)
    {
        if (classNode == null || classNode.name == null || classNode.name.isEmpty())
            return EMPTY;

        return of(classNode.name);
    }

    public static ClassReference of(@NotNull ClassReferenceType type)
    {
        return of(type.getInternalName());
    }


    private static String[] normalizePackages(String[] packages)
    {
        if (packages == null || packages.length == 0)
            return new String[0];

        return Arrays.stream(packages)
                     .filter(pkg -> pkg != null && !pkg.isEmpty())
                     .toArray(String[]::new);
    }

    private static String concat(String a, String b, String delimiter)
    {
        if (a == null || a.isEmpty())
            return b;
        else if (b == null || b.isEmpty())
            return a;
        else
            return a + delimiter + b;
    }
}
