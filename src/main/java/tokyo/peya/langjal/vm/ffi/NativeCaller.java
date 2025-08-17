package tokyo.peya.langjal.vm.ffi;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.exceptions.LinkagePanic;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
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

    private final Arena arena = Arena.ofShared();

    private final Map<ClassReference, NativeLibrary> libraries;

    public NativeCaller()
    {
        this.libraries = new HashMap<>();
    }

    public SymbolLookup registerLibrary(@NotNull ClassReference caller, @NotNull String name)
    {
        Map<String, SymbolLookup> symbols;
        NativeLibrary nativeLibrary = this.libraries.get(caller);
        if (nativeLibrary == null)
            this.libraries.put(caller, new NativeLibrary(name, symbols = new HashMap<>(), new HashMap<>()));
        else
            symbols = nativeLibrary.symbols;

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
        symbols.put(name, lookup);

        return lookup;
    }

    private MethodHandle createCachedMethodHandle(@NotNull NativeLibrary nativeLibrary,
                                                  @NotNull String name,
                                                  @NotNull VMType returningType,
                                                  @NotNull VMValue... args)
    {
        VMType[] argTypes = Arrays.stream(args)
                                  .map(VMValue::type)
                                  .toArray(VMType[]::new);
        List<MethodHandleCache> caches = nativeLibrary.handles.get(name);
        if (caches == null)
            nativeLibrary.handles.put(name, caches = new ArrayList<>());
        else
        {
            for (MethodHandleCache cached : caches)
                if (cached.returningType.equals(returningType)
                        && Arrays.equals(cached.args, argTypes))
                {
                    return cached.handle;
                }
        }

        SymbolLookup lookup = nativeLibrary.symbols.get(name);
        if (lookup == null)
            throw new LinkagePanic("No symbol found for name: " + name);

        MemorySegment seg = lookup.find(name)
                                  .orElseThrow(() -> new LinkagePanic("Failed to find symbol: " + name));

        MemoryLayout[] arguments = createFunctionLayouts(returningType, args);
        FunctionDescriptor layout;
        if (returningType.equals(VMType.VOID))
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

    public VMValue callFFI(@NotNull ClassReference caller, @NotNull String name,
                           @NotNull VMType returningType, @NotNull VMValue... args)
    {
        List<VMValue> results = new ArrayList<>();
        NativeLibrary nativeLibrary = this.libraries.get(caller);
        if (nativeLibrary == null)
            throw new LinkagePanic("No native library registered for the current class.");

        MethodHandle methodHandle = this.createCachedMethodHandle(nativeLibrary, name, returningType, args);
        Object[] convertedArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++)
            convertedArgs[i] = args[i].toJavaObject();
        try
        {
            Object result = methodHandle.invokeExact(convertedArgs);
            return VMValue.fromJavaObject(result);  // TODO: cast
        }
        catch (Throwable e)
        {
            throw new VMPanic("Failed to invoke native function: " + name, e);
        }
    }

    private static MemoryLayout[] createFunctionLayouts(VMType type, VMValue[] args)
    {
        MemoryLayout[] layouts = new MemoryLayout[args.length];
        for (int i = 0; i < args.length; i++)
        {
            VMValue arg = args[i];
            if (arg instanceof VMPrimitive<?> primitive)
                layouts[i] = createFunctionLayout(primitive.type());
            else if (arg instanceof VMReferenceValue)
                layouts[i] = ValueLayout.ADDRESS; // 参照型はアドレスとして扱う
        }
        return layouts;
    }

    private static MemoryLayout createFunctionLayout(@NotNull VMType type)
    {
        if (type == VMType.BOOLEAN)
            return ValueLayout.JAVA_BOOLEAN;
        else if (type == VMType.BYTE)
            return ValueLayout.JAVA_BYTE;
        else if (type == VMType.SHORT)
            return ValueLayout.JAVA_SHORT;
        else if (type == VMType.CHAR)
            return ValueLayout.JAVA_CHAR;
        else if (type == VMType.INTEGER)
            return ValueLayout.JAVA_INT;
        else if (type == VMType.LONG)
            return ValueLayout.JAVA_LONG;
        else if (type == VMType.FLOAT)
            return ValueLayout.JAVA_FLOAT;
        else if (type == VMType.DOUBLE)
            return ValueLayout.JAVA_DOUBLE;
        else
            throw new VMPanic("Unsupported type for native function: " + type);
    }

    private record NativeLibrary(
            @NotNull
            String name,
            @NotNull
            Map<String, SymbolLookup> symbols,
            @NotNull
            Map<String, List<MethodHandleCache>> handles
    ) {}

    private record MethodHandleCache(
            @NotNull
            String name,
            @NotNull
            VMType returningType,
            @NotNull
            VMType[] args,
            @NotNull
            MethodHandle handle
    ) {}
}
