package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.panics.PanicCreator;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMReferenceValue;
import tokyo.peya.langjal.vm.values.VMValue;

public class InjectorStackTraceElement implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("java/lang/StackTraceElement");

    @Override
    public @NotNull ClassReference suitableClass()
    {
        return CLAZZ;
    }

    @Override
    public void inject(@NotNull VMSystemClassLoader cl, @NotNull VMClass clazz)
    {
        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "initStackTraceElements",
                        "([Ljava/lang/StackTraceElement;Ljava/lang/Object;I)V",
                        null,
                        null
                )
                )
                {
                    @Override
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMArray elements = (VMArray) args[0];
                        VMReferenceValue backTrace = (VMReferenceValue) args[1];
                        int depth = ((VMInteger) args[2]).asNumber().intValue();

                        // この VM では， backTrace は VMStackTraceElement[] 型の配列を持つ。
                        // そのため， elements.length と depth の小さい方を取得し，その数だけ要素を backTrace から elements にコピーするだけ。

                        if (!(backTrace instanceof VMArray backTraceArray))
                            throw PanicCreator.createInternalPanic(
                                    frame,
                                    "Expected VMArray but got " + backTrace.getClass().getSimpleName(),
                                    null
                            );

                        for (int i = 0; i < depth; i++)
                            elements.set(i, backTraceArray.get(i));

                        return null;
                    }
                }
        );
    }

}
