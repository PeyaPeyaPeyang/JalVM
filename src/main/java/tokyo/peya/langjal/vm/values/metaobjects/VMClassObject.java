package tokyo.peya.langjal.vm.values.metaobjects;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;

@Getter
public class VMClassObject extends VMObject
{
    @Nullable
    private final VMClass clazz;
    @NotNull
    private final VMType typeOf;

    public VMClassObject(@NotNull VMSystemClassLoader cl, @Nullable VMClass clazz, @NotNull VMType typeOf)
    {
        super(cl.findClass(ClassReference.of("java/lang/Class")));
        this.clazz = clazz;
        this.typeOf = typeOf;

        this.forceInitialise();
    }

    public VMClassObject(@NotNull JalVM vm, @NotNull VMType typeOf)
    {
        this(vm.getClassLoader(), null, typeOf);
    }

    public VMClassObject(@NotNull VMSystemClassLoader cl, @NotNull VMType typeOf)
    {
        this(cl, null, typeOf);
    }

    public boolean isPrimitive()
    {
        return this.clazz != null && this.clazz.isPrimitive();
    }
}
