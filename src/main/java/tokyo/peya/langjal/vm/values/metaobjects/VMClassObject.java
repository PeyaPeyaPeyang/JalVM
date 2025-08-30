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

    public VMClassObject(@NotNull JalVM vm, @NotNull VMClass representingClass, @NotNull VMType<?> typeOf)
    {
        super(vm.getClassLoader().findClass(ClassReference.of("java/lang/Class")));
        this.representingClass = representingClass;
        this.typeOf = typeOf;

        if (typeOf.getComponentType() != null)
            this.setField("componentType", new VMClassObject(typeOf.getComponentType()));

        this.forceInitialise(vm.getClassLoader());
    }

    public VMClassObject(@NotNull VMType<?> typeOf)
    {
        this(typeOf.getVM(), typeOf.getLinkedClass(), typeOf);
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
