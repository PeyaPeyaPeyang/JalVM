package tokyo.peya.langjal.vm.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class VMPluginClassLoader extends URLClassLoader {

    private final Path pluginPath;
    private final URL pluginURL;

    private VMPlugin plugin;

    public VMPluginClassLoader(@NotNull Path pluginPath, @NotNull URL pluginUrl) {
        super(new URL[]{pluginUrl}, VMPluginClassLoader.class.getClassLoader());
        this.pluginPath = pluginPath;
        this.pluginURL = pluginUrl;
    }

    @Nullable
    public VMPlugin loadPlugin() {
        try (JarFile jarFile = new JarFile(this.pluginPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class"))
                    continue;

                Class<?> clazz = findClass(entry.getName().replace('/', '.').replace(".class", ""));
                if (VMPlugin.class.isAssignableFrom(clazz))
                    return this.plugin = (VMPlugin) clazz.getDeclaredConstructor().newInstance();
            }
        } catch (ClassNotFoundException | IOException | InvocationTargetException | InstantiationException |
                 IllegalAccessException | NoSuchMethodException e) {
            System.err.println("Failed to load plugin class from " + this.pluginPath + ": " + e.getMessage());
            e.printStackTrace();
        }

        System.err.println("No plugin class found in " + this.pluginPath);
        return null;
    }


    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }
}
