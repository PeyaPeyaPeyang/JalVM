package tokyo.peya.langjal.vm.values.metaobjects;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMObject;

@Getter
public class VMConstructorObject extends VMMethodObject
{
    private final VMSystemClassLoader classLoader;
    private final VMMethod method;

    public VMConstructorObject(@NotNull VMSystemClassLoader classLoader, @NotNull VMMethod method)
    {
        super(classLoader, method, classLoader.findClass(ClassReference.of("java/lang/reflect/Constructor")));
        this.classLoader = classLoader;
        this.method = method;
    }
}
