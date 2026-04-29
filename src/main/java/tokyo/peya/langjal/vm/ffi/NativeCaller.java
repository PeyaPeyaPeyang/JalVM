package tokyo.peya.langjal.vm.ffi;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.panics.LinkagePanic;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMPrimitive;
import tokyo.peya.langjal.vm.values.VMReferenceValue;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NativeCaller
{
    private static final Linker LINKER = Linker.nativeLinker();

    private final JalVM vm;
    private final Arena arena = Arena.ofShared();

    private final Map<ClassReference, LoadedClassLibraries> libraries;

    private long lastHandleID = 1L;  // 0 は無効なので，1 から始める

    public NativeCaller(@NotNull JalVM vm)
    {
        this.vm = vm;
        this.libraries = new HashMap<>();
    }

    public NativeLibrary registerLibrary(@NotNull ClassReference caller, @NotNull String name)
    {
        Map<String, NativeLibrary> symbols;
        LoadedClassLibraries loadedClassLibraries = this.libraries.get(caller);
        if (loadedClassLibraries == null)
        {
            LoadedClassLibraries lcl;
            this.libraries.put(caller, lcl = new LoadedClassLibraries(name));
            symbols = lcl.symbols;
        }
        else
            symbols = loadedClassLibraries.symbols;

        if (symbols.containsKey(name))
            return symbols.get(name);
        SymbolLookup lookup;
        try
        {
            lookup = SymbolLookup.libraryLookup(name, this.arena);
        }
        catch (IllegalArgumentException e)
        {
            try
            {
                lookup = SymbolLookup.libraryLookup(name + ".so", this.arena);  // Linux は省略不可
            }
            catch (IllegalArgumentException e2)
            {
                throw new VMPanic("Failed to register native library: " + name, e2);
            }
        }

        NativeLibrary nativeLibrary;
        symbols.put(name, nativeLibrary = new NativeLibrary(
                name,
                lookup,
                this.lastHandleID++
        ));

        return nativeLibrary;
    }

    private MethodHandle createCachedMethodHandle(@NotNull NativeCaller.LoadedClassLibraries loadedClassLibraries,
                                                  @NotNull String name,
                                                  @NotNull VMType<?> returningType,
                                                  @NotNull VMValue... args)
    {
        VMType<?>[] argTypes = Arrays.stream(args)
                                  .map(VMValue::type)
                                  .toArray(VMType<?>[]::new);
        List<MethodHandleCache> caches = loadedClassLibraries.handles.get(name);
        if (caches == null)
            loadedClassLibraries.handles.put(name, caches = new ArrayList<>());
        else
        {
            for (MethodHandleCache cached : caches)
                if (cached.returningType.equals(returningType)
                        && Arrays.equals(cached.args, argTypes))
                {
                    return cached.handle;
                }
        }

        NativeLibrary library = loadedClassLibraries.symbols.get(name);
        if (library == null)
            throw new LinkagePanic("No symbol found for name: " + name);

        SymbolLookup lookup = library.lookup;
        MemorySegment seg = lookup.find(name)
                                  .orElseThrow(() -> new LinkagePanic("Failed to find symbol: " + name));

        MemoryLayout[] arguments = createFunctionLayouts(returningType, args);
        FunctionDescriptor layout;
        if (returningType.equals(VMType.of(this.vm, PrimitiveTypes.VOID)))
            layout = FunctionDescriptor.ofVoid(arguments);
        else
            layout = FunctionDescriptor.of(createFunctionLayout(returningType), arguments);

        MethodHandle methodHandle = LINKER.downcallHandle(seg, layout);
        if (methodHandle == null)
            throw new VMPanic("Failed to create method handle for native function: " + name);
        MethodHandleCache cache = new MethodHandleCache(
                name,
                returningType,
                argTypes,
                methodHandle
        );
        caches.add(cache);
        return methodHandle;
    }

    public VMValue callFFI(@NotNull VMThread order, @NotNull ClassReference owner, @NotNull String name,
                           @NotNull VMType<?> returningType, @NotNull VMValue... args)
    {
        List<VMValue> results = new ArrayList<>();
        LoadedClassLibraries loadedClassLibraries = this.libraries.get(owner);
        if (loadedClassLibraries == null)
            throw new LinkagePanic("No native library registered for the current class, can't call native function: " + name
                                           + " (owner: " + owner + ")");

        MethodHandle methodHandle = this.createCachedMethodHandle(loadedClassLibraries, name, returningType, args);
        Object[] convertedArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++)
            convertedArgs[i] = args[i].toJavaObject();
        try
        {
            Object result = methodHandle.invokeExact(convertedArgs);
            return VMValue.fromJavaObject(order, result);  // TODO: cast
        }
        catch (Throwable e)
        {
            throw new VMPanic("Failed to invoke native function: " + name, e);
        }
    }

    private static MemoryLayout[] createFunctionLayouts(VMType<?> type, VMValue[] args)
    {
        MemoryLayout[] layouts = new MemoryLayout[args.length];
        for (int i = 0; i < args.length; i++)
        {
            VMValue arg = args[i];
            if (arg instanceof VMPrimitive primitive)
                layouts[i] = createFunctionLayout(primitive.type());
            else if (arg instanceof VMReferenceValue)
                layouts[i] = ValueLayout.ADDRESS; // 参照型はアドレスとして扱う
        }
        return layouts;
    }

    private static MemoryLayout createFunctionLayout(@NotNull VMType<?> type)
    {
        return switch (type.getType())
        {
            case PrimitiveTypes.BOOLEAN -> ValueLayout.JAVA_BOOLEAN;
            case PrimitiveTypes.BYTE -> ValueLayout.JAVA_BYTE;
            case PrimitiveTypes.SHORT -> ValueLayout.JAVA_SHORT;
            case PrimitiveTypes.CHAR -> ValueLayout.JAVA_CHAR;
            case PrimitiveTypes.INT -> ValueLayout.JAVA_INT;
            case PrimitiveTypes.LONG -> ValueLayout.JAVA_LONG;
            case PrimitiveTypes.FLOAT -> ValueLayout.JAVA_FLOAT;
            case PrimitiveTypes.DOUBLE -> ValueLayout.JAVA_DOUBLE;
            case null, default -> throw new VMPanic("Unsupported type for native function: " + type);
        };
    }

    private record LoadedClassLibraries(
            @NotNull
            String name,
            @NotNull
            Map<String, NativeLibrary> symbols,
            @NotNull
            Map<String, List<MethodHandleCache>> handles
    ) {
        private LoadedClassLibraries (@NotNull String name)
        {
            this(name, new HashMap<>(), new HashMap<>());
        }
    }

    public record NativeLibrary(
            @NotNull
            String name,
            @NotNull
            SymbolLookup lookup,
            long handle
    ) {}

    private record MethodHandleCache(
            @NotNull
            String name,
            @NotNull
            VMType<?> returningType,
            @NotNull
            VMType<?>[] args,
            @NotNull
            MethodHandle handle
    ) {}
}
