package tokyo.peya.langjal.vm.values.metaobjects;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMBoolean;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMNull;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;

@Getter
public class VMThreadFieldHolderObject extends VMObject
{
    private final JalVM vm;
    private VMThread thread;  // VM が作成する場合， start0() まで null の可能性がある。

    public VMThreadFieldHolderObject(@NotNull VMComponent com, @NotNull VMThread thread)
    {
        super(com.getClassLoader().findClass(ClassReference.of("java/lang/Thread$FieldHolder")));
        this.vm = com.getVM();
        this.thread = thread;

        this.syncFields();
        this.forceInitialise(com.getClassLoader());
    }

    public VMThreadFieldHolderObject(@NotNull VMComponent com, @Nullable VMObject owner)
    {
        super(com.getClassLoader().findClass(ClassReference.of("java/lang/Thread$FieldHolder")), owner);
        this.vm = com.getVM();
    }

    public void setVMCreatedThread(@NotNull VMThread thread)
    {
        if (this.thread != null)
            throw new IllegalStateException("Thread already set");
        this.thread = thread;
    }

    public void updateState()
    {
        this.setField("threadStatus", new VMInteger(this.vm, this.thread.getState().getMask()));
    }

    public void syncFields()
    {
        this.setField("group", this.thread.getGroup().getObject());
        this.setField("task", new VMNull<>(VMType.ofClassName(this.vm, "java/lang/Runnable")));
        this.setField("stackSize", new VMLong(this.vm, 0));
        this.setField("priority", new VMInteger(this.vm, this.thread.getPriority()));
        this.setField("daemon", VMBoolean.of(this.vm, this.thread.isDaemon()));
    }
}
