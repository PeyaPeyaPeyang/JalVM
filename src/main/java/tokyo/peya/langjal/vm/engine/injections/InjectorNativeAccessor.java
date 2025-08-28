package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.reflection.VMConstructorObject;

public class InjectorNativeAccessor implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("jdk/internal/reflect/DirectConstructorHandleAccessor$NativeAccessor");

    @Override
    public ClassReference suitableClass()
    {
        return CLAZZ;
    }

    @Override
    public void inject(@NotNull VMSystemClassLoader cl, @NotNull VMClass clazz)
    {
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "newInstance0",
                        "(Ljava/lang/reflect/Constructor;[Ljava/lang/Object;)Ljava/lang/Object;",
                        null,
                        null
                )
                )
                {
                    @Override
                    VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                   @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        assert caller != null;
                        VMConstructorObject constructor = (VMConstructorObject) args[0];
                        VMObject obj = constructor.getMethod().getClazz().createInstance();
                        VMValue[] callingArgs;
                        if (args[1] instanceof VMArray arr)
                            callingArgs = arr.getElements();
                        else
                            callingArgs = new VMValue[0];

                        constructor.call(thread, caller, obj, callingArgs, false);
                        return obj;
                    }
                }
        );
    }
}
