package tokyo.peya.langjal.vm.values.metaobjects;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMThreadGroup;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMBoolean;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;

@Getter
public class VMThreadGroupObject extends VMObject
{
    private final JalVM vm;
    private final VMThreadGroup group;

    public VMThreadGroupObject(@NotNull JalVM vm, @NotNull VMThreadGroup group)
    {
        super(vm.getClassLoader().findClass(ClassReference.of("java/lang/ThreadGroup")));
        this.vm = vm;
        this.group = group;

        if (group.getParent() != null)
            this.setField("parent", group.getParent().getObject());
        this.setField("name", VMStringObject.createString(vm, group.getName()));

        this.updateFields();
        this.forceInitialise(vm.getClassLoader());
    }

    public void updateFields()
    {
        this.setField("maxPriority", new VMInteger(this.vm, this.group.getMaxPriority()));
        this.setField("daemon", VMBoolean.of(this.vm, this.group.isDaemon()));
    }

    public void updateChildren()
    {
        int childrenCount = this.group.getChildren().size();
        VMArray children = new VMArray(
                this.vm,
                VMType.ofClassName(this.vm, "java/lang/ThreadGroup"),
                childrenCount
        );

        for (int i = 0; i < childrenCount; i++)
            children.set(i, this.group.getChildren().get(i).getObject());

        this.setField("ngroups", new VMInteger(this.vm, childrenCount));
        this.setField("groups", children);
    }
}
