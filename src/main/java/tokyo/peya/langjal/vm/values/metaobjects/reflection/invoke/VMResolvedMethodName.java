package tokyo.peya.langjal.vm.values.metaobjects.reflection.invoke;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMObject;

@Getter
public class VMResolvedMethodName extends VMObject
{
    private final VMMethod method;
    private final VMClass declaringClass;

    public VMResolvedMethodName(@NotNull VMSystemClassLoader cl, @NotNull VMMethod method)
    {
        super(cl.findClass(ClassReference.of("java/lang/invoke/ResolvedMethodName")));

        this.method = method;
        this.declaringClass = method.getClazz();

        this.setField("declaringClass", this.declaringClass.getClassObject());

        this.forceInitialise(cl);
    }
}
