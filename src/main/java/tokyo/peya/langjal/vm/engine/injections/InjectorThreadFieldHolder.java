package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.metaobjects.VMThreadFieldHolderObject;

public class InjectorThreadFieldHolder implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("java/lang/Thread$FieldHolder");

    @Override
    public @NotNull ClassReference suitableClass()
    {
        return CLAZZ;
    }

    @Override
    public void inject(@NotNull VMSystemClassLoader cl, @NotNull VMClass clazz)
    {
        // new 命令のとき，汎用的な VMObject の代わりに， VMThreadFieldHolderObject を生成するようにする。
        clazz.injectInstanceCreator((o) -> new VMThreadFieldHolderObject(cl, o));
    }
}
