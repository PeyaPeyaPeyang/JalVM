package tokyo.peya.langjal.vm.values.metaobjects;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;

@Getter
public class VMClassObject extends VMObject
{
    @NotNull
    private final VMClass representingClass;
    @NotNull
    private final VMType<?> typeOf;

    public VMClassObject(@NotNull VMSystemClassLoader cl, @NotNull VMClass representingClass, @NotNull VMType<?> typeOf)
    {
        super(cl.findClass(ClassReference.of("java/lang/Class")));
        this.representingClass = representingClass;
        this.typeOf = typeOf;

        typeOf.linkClass(cl);
        this.forceInitialise(cl);
    }

    public VMClassObject(@NotNull JalVM vm, @NotNull VMType<?> typeOf)
    {
        this(vm.getClassLoader(), typeOf.getLinkedClass(), typeOf);
    }

    public VMClassObject(@NotNull VMSystemClassLoader cl, @NotNull VMType<?> typeOf)
    {
        this(cl, typeOf.getLinkedClass(), typeOf);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof VMClassObject other))
            return false;
        return this.representingClass.equals(other.representingClass);
    }

    @Override
    public @NotNull String toString()
    {
        return "class " + this.representingClass.getReference();
    }

    public boolean isPrimitive()
    {
        return this.representingClass.isPrimitive();
    }
}
