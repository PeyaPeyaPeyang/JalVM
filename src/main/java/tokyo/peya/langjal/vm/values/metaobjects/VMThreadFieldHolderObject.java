package tokyo.peya.langjal.vm.values.metaobjects;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
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
    private final VMThread thread;

    public VMThreadFieldHolderObject(@NotNull JalVM vm, @NotNull VMThread thread)
    {
        super(vm.getClassLoader().findClass(ClassReference.of("java/lang/Thread$FieldHolder")));
        this.vm = vm;
        this.thread = thread;

        this.syncFields();
        this.forceInitialise(vm.getClassLoader());
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
