package tokyo.peya.langjal.vm;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.api.VMEventManager;
import tokyo.peya.langjal.vm.api.VMPluginLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMEngine;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMType;

@Getter
public class JalVM {
    private final VMHeap heap;
    private final ClassPaths classPaths;
    private final VMSystemClassLoader classLoader;
    private final VMEngine engine;
    private final VMEventManager eventManager;
    private final VMPluginLoader pluginLoader;
    private final boolean debugging = true;


    public JalVM() {
        System.out.println("Initialising J(al)VM...");

        this.eventManager = new VMEventManager();
        this.pluginLoader = new VMPluginLoader();

        this.heap = new VMHeap();
        this.classPaths = new ClassPaths();
        this.classLoader = new VMSystemClassLoader(this, this.heap);
        this.engine = new VMEngine(this);

        this.pluginLoader.loadPlugins();
        initialiseWellKnownClasses(this.classLoader);
    }

    private static void initialiseWellKnownClasses(@NotNull VMSystemClassLoader cl) {
        VMType.STRING.linkClass(cl);
    }

    public void startJVM() {
        System.out.println("Starting J(al)VM...");
        this.engine.startEngine();
        System.out.println("J(al)VM has stopped successfully.");
    }

    public void executeMain(@NotNull ClassReference clazz, @NotNull String[] args) {
        VMClass vmClass = this.heap.getLoadedClass(clazz);
        if (vmClass == null)
            throw new IllegalStateException("Unable to load class: " + clazz.getFullQualifiedName()
                    + ", please define it with VMHeap#defineClass() first!");

        this.executeMain(vmClass, args);
    }

    public void executeMain(@NotNull VMClass clazz, @NotNull String[] args) {
        VMMethod mainMethod = clazz.findEntryPoint();
        if (mainMethod == null)
            throw new IllegalStateException("There is no main method in class:  "
                    + clazz.getReference().getFullQualifiedName());

        this.engine.getMainThread().startMainThread(mainMethod, args);

        this.startJVM();
    }
}
