package tokyo.peya.langjal.vm;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.MethodDescriptor;
import tokyo.peya.langjal.vm.api.VMEventManager;
import tokyo.peya.langjal.vm.api.VMPluginLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.engine.VMEngine;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.ffi.NativeCaller;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMType;

import java.time.Instant;

@Getter
public class JalVM implements VMComponent
{
    private final VMConfiguration config;

    private final VMHeap heap;
    private final ClassPaths classPaths;
    private final VMSystemClassLoader classLoader;
    private final VMEngine engine;
    private final NativeCaller nativeCaller;
    private final VMEventManager eventManager;
    private final VMPluginLoader pluginLoader;

    private Instant initialisationStartedAt;
    private Instant initialisationFinishedAt;
    private Instant startedAt;
    private Instant finishedAt;

    private final boolean debugging = true;

    private boolean isRunning;

    public JalVM(@NotNull VMConfiguration config)
    {
        this.config = config;

        System.out.println("Starting J(al)VM...");
        this.eventManager = new VMEventManager();
        this.pluginLoader = new VMPluginLoader();

        this.heap = new VMHeap();
        this.classLoader = new VMSystemClassLoader(this, this.heap);
        this.classPaths = new ClassPaths();
        VMType.initVM(this);
        this.engine = new VMEngine(this);
        this.nativeCaller = new NativeCaller(this);

        this.pluginLoader.loadPlugins();
    }


    public void startJVM(@NotNull VMMethod mainMethod, @NotNull String[] args)
    {
        this.isRunning = true;
        this.classLoader.resumeLinking();

        System.out.println("Starting J(al)VM, please wait...");

        // 初期化開始
        System.out.println("Initialising J(al)VM...");
        this.initialisationStartedAt = Instant.now();
        this.initialiseVM();
        this.initialisationFinishedAt = Instant.now();
        System.out.println("VM Initialisation SUCCESS, took " + (this.initialisationFinishedAt.toEpochMilli() - this.initialisationStartedAt.toEpochMilli()) + " ms");

        // メインメソッド開始
        System.out.println("Launching main method: " + mainMethod.getOwningClass() + "->" + mainMethod.getName() + mainMethod.getDescriptor());
        this.engine.getMainThread().startMainThread(mainMethod, args);
        this.engine.startEngine();
        System.out.println("J(al)VM has stopped successfully.");
        this.isRunning = false;
    }

    public void initialiseVM()
    {
        VMClass systemClass = this.classLoader.findClass(ClassReference.of("java/lang/System"));
        VMMethod initPhase1 = systemClass.findMethod("initPhase1", MethodDescriptor.parse("()V"));
        if (initPhase1 == null)
            throw new VMPanic("System class does not have initPhase1 method");
        this.engine.getMainThread().invokeVMInitialisation(initPhase1);
        this.engine.startEngine();
    }

    public void executeMain(@NotNull ClassReference clazz, @NotNull String[] args)
    {
        VMClass vmClass = this.heap.getLoadedClass(clazz);
        if (vmClass == null)
            throw new IllegalStateException("Unable to load class: " + clazz.getFullQualifiedName()
                                                    + ", please define it with VMHeap#defineClass() first!");

        this.executeMain(vmClass, args);
    }

    public void executeMain(@NotNull VMClass clazz, @NotNull String[] args)
    {
        VMMethod mainMethod = clazz.findEntryPoint();
        if (mainMethod == null)
            throw new IllegalStateException("There is no main method in class:  "
                                                    + clazz.getReference().getFullQualifiedName());

        this.startJVM(mainMethod, args);
    }

    @Override
    public @NotNull JalVM getVM()
    {
        return this;
    }
}
