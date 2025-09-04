package tokyo.peya.langjal.vm.values.metaobjects;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.VMThreadGroup;
import tokyo.peya.langjal.vm.engine.scheduler.VMRunnable;
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
    private VMThreadGroup group;  // VM が作成する場合，情報を収集してからセットする。

    public VMThreadGroupObject(@NotNull VMComponent com, @NotNull VMThreadGroup group)
    {
        super(com.getClassLoader().findClass(ClassReference.of("java/lang/ThreadGroup")));
        this.vm = com.getVM();
        this.group = group;

        if (group.getParent() != null)
            this.setField("parent", group.getParent().getObject());
        this.setField("name", VMStringObject.createString(com, group.getName()));

        this.syncFields();
        this.forceInitialise(com.getClassLoader());
    }

    public VMThreadGroupObject(@NotNull VMComponent com, @NotNull VMObject owner)
    {
        super(com.getClassLoader().findClass(ClassReference.of("java/lang/ThreadGroup")), owner);
        this.vm = com.getVM();
    }

    public void syncFields()
    {
        if (this.group == null)
            return;

        this.setField("maxPriority", new VMInteger(this.vm, this.group.getMaxPriority()));
        this.setField("daemon", VMBoolean.of(this.vm, this.group.isDaemon()));
    }

    public void syncChildren()
    {
        if (this.group == null)
            return;

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

    @Override
    protected void beforeInitialise(@Nullable VMFrame frame)
    {
        if (frame == null)
            return;  // これは VM が作成する場合
        VMFrame threadInitialisingFrame = frame.getPrevFrame();
        if (threadInitialisingFrame == null)
            return;

        VMRunnable.of(frame, frame, () -> {
            VMThreadGroupObject parent = (VMThreadGroupObject) VMThreadGroupObject.this.getField("parent");
            String name = ((VMStringObject) VMThreadGroupObject.this.getField("name")).getString();
            int maxPriority = ((VMInteger) VMThreadGroupObject.this.getField("maxPriority")). asNumber().intValue();
            boolean daemon = ((VMBoolean) VMThreadGroupObject.this.getField("daemon")).asBoolean();

            VMThreadGroup parentGroup = parent.getGroup();
            VMThreadGroup group = parentGroup.createChild(this.vm, name, maxPriority);
            group.setDaemon(daemon);
            VMThreadGroupObject.this.group = group;

        }).runTaskLater(1);  // 初期化が終わり，コントロールが戻ってきたあと
    }
}
