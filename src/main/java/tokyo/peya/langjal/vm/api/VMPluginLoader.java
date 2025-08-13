package tokyo.peya.langjal.vm.api;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Getter
public class VMPluginLoader {
    private static final Path PLUGINS_PATH = Paths.get("plugins");

    private final List<VMPlugin> plugins;

    public VMPluginLoader() {
        this.plugins = new ArrayList<>();
    }

    public void loadPlugins() {
        System.out.println("Loading plugins from: " + PLUGINS_PATH.toAbsolutePath());

        Path pluginsDir = PLUGINS_PATH.toAbsolutePath();
        if (!Files.exists(pluginsDir))
            return;
        if (!Files.isDirectory(pluginsDir)) {
            System.err.println("Plugins directory is not a directory: " + pluginsDir);
            return;
        }

        try (Stream<Path> paths = Files.walk(pluginsDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".jar"))
                 .forEach(this::loadPlugin);
        } catch (Exception e) {
            System.err.println("Error loading plugins: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Loaded " + plugins.size() + " plugin(s).");
    }

    private void loadPlugin(@NotNull Path path) {
        try {
            System.out.println("Loading file: " + path.toAbsolutePath());
            URL pluginUrl = path.toUri().toURL();
            VMPluginClassLoader classLoader = new VMPluginClassLoader(path, pluginUrl);
            VMPlugin plugin = classLoader.loadPlugin();
            if (plugin == null)
                this.plugins.add(plugin);
            System.out.println("Plugin loaded successfully: " + path.getFileName());
        } catch (Exception e) {
            System.err.println("Failed to load plugin from " + path + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public VMPlugin getPlugin(@NotNull Class<? extends VMPlugin> pluginClass) {
        for (VMPlugin plugin : plugins)
            if (plugin.getClass().equals(pluginClass))
                return plugin;

        return null;
    }

    public List<VMPlugin> getPlugins() {
        return new ArrayList<>(this.plugins);
    }
}
