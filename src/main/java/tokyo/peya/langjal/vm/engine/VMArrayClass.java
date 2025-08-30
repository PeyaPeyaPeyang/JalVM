package tokyo.peya.langjal.vm.engine;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.AccessAttribute;
import tokyo.peya.langjal.compiler.jvm.AccessAttributeSet;
import tokyo.peya.langjal.compiler.jvm.AccessLevel;
import tokyo.peya.langjal.compiler.jvm.MethodDescriptor;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.injections.InjectedField;
import tokyo.peya.langjal.vm.engine.injections.InjectedMethod;
import tokyo.peya.langjal.vm.engine.members.AccessibleObject;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMReferenceValue;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMClassObject;
import tokyo.peya.langjal.vm.values.metaobjects.VMStringObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class VMArrayClass extends VMClass
{
    private final VMType<?> arrayType;
    private final VMClass componentClass;

    public VMArrayClass(@NotNull JalVM vm, @NotNull VMType<?> arrayType, @NotNull VMClass componentClass)
    {
        super(vm, componentClass.getClazz(), arrayType.getComponentType());
        this.arrayType = arrayType;
        this.componentClass = componentClass;

        vm.getClassLoader().linkType(this);
    }

    @Override
    public void link(@NotNull JalVM vm)
    {
        if (this.isLinked)
            return;

        VMSystemClassLoader cl = vm.getClassLoader();
        super.link(vm);
        this.interfaceLinks.add(cl.findClass(ClassReference.of("java/util/Collection")));
        this.superLink = cl.findClass(ClassReference.of("java/lang/Object"));

        this.isLinked = true;
        this.isInitialised = true;
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
