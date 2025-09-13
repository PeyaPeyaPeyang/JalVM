package tokyo.peya.langjal.vm.engine;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMType;

@Getter
public class VMArrayClass extends VMClass
{
    private final VMType<?> arrayType;
    private final VMClass componentClass;

    public VMArrayClass(@NotNull VMComponent component, @NotNull VMType<?> arrayType, @NotNull VMClass componentClass)
    {
        super(component, componentClass.getClazz(), arrayType.getComponentType());
        this.arrayType = arrayType;
        this.componentClass = componentClass;
        this.linkedClass = this;

        component.getClassLoader().linkType(this);
    }

    @Override
    public void link(@NotNull JalVM vm)
    {
        if (this.isLinked)
            return;

        VMSystemClassLoader cl = vm.getClassLoader();
        this.linkedClass = this;
        this.interfaceLinks.add(cl.findClass(ClassReference.of("java/util/Collection")));
        this.superLink = cl.findClass(ClassReference.of("java/lang/Object"));

        this.isLinked = true;
        this.isInitialised = true;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!(obj instanceof VMArrayClass other))
            return false;

        return this.arrayType.equals(other.arrayType);
    }



    @Override
    public @Nullable VMMethod findSuitableMethod(@Nullable VMClass caller, @Nullable VMClass owner,
                                                 @NotNull String methodName, @Nullable VMType<?> returnType,
                                                 @NotNull VMType<?>... args)
    {
        // Object のメソッドを返す
        return this.superLink.findSuitableMethod(caller, owner, methodName, returnType, args);
    }

    @Override
    public boolean isAssignableFrom(@NotNull VMType<?> other)
    {
        if (other.getComponentType() == null)
            return false;

        return this.componentClass.isAssignableFrom(other.getComponentType());
    }

    @Override
    public VMType<?> getComponentType()
    {
        return this.componentClass;
    }

    @Override
    public String toString()
    {
        return "[" + this.componentClass.toString();
    }
}
