package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMBoolean;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMClassObject;

public class InjectorReflection implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("jdk/internal/reflect/Reflection");

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
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "getCallerClass",
                        "()Ljava/lang/Class;",
                        null,
                        null
                )
                )
                {
                    @Override
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        assert caller != null;
                        // caller を返せば一見良さそうだが，実際はもう一個上。
                        VMFrame prevFrame = frame.getPrevFrame()  // Reflection.getCallerClass() を呼んだフレーム
                                                 .getPrevFrame(); // そのまた呼び出し元
                        assert prevFrame != null;

                        return prevFrame.getMethod().getOwningClass().getClassObject();
                    }
                }
        );
        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "getClassAccessFlags",
                        "(Ljava/lang/Class;)I",
                        null,
                        null
                )
                )
                {
                    @Override
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMClassObject obj = (VMClassObject) args[0];
                        int access = obj.getRepresentingClass().getClazz().access;
                        return new VMInteger(frame, access);
                    }
                }
        );
        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "areNestMates",
                        "(Ljava/lang/Class;Ljava/lang/Class;)Z",
                        null,
                        null
                )
                )
                {
                    @Override
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMClassObject classObj1 = (VMClassObject) args[0];
                        VMClassObject classObj2 = (VMClassObject) args[1];

                        VMClass class1 = classObj1.getRepresentingClass();
                        VMClass class2 = classObj2.getRepresentingClass();

                        VMClass nestHost1 = class1.getOuterLink() == null ? class1 : class1.getOuterLink();
                        VMClass nestHost2 = class2.getOuterLink() == null ? class2 : class2.getOuterLink();

                        return VMBoolean.of(frame, nestHost1.equals(nestHost2));
                    }
                }
        );
    }

}
