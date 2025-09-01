package tokyo.peya.langjal.vm.values.metaobjects;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMObject;

import java.lang.reflect.Field;

@Getter
public class VMThreadObject extends VMObject
{
    private final JalVM vm;
    private final VMThread thread;
    private final VMThreadFieldHolderObject fieldHolder;

    public VMThreadObject(@NotNull JalVM vm, @NotNull VMThread thread)
    {
        super(vm.getClassLoader().findClass(ClassReference.of("java/lang/Thread")));
        this.vm = vm;
        this.thread = thread;
        this.fieldHolder = new VMThreadFieldHolderObject(vm, thread);

        this.setField("holder", this.fieldHolder);
        this.setField("name", VMStringObject.createString(vm, thread.getName()));
        this.setField("tid", new VMLong(vm, thread.getId()));

        this.setField("scopedValueBindings", this.getObjectType().getClassObject());

        this.forceInitialise(vm.getClassLoader());
    }

    public void syncStateField()
    {
        this.fieldHolder.updateState();
    }

    public void syncFields()
    {
        this.fieldHolder.syncFields();
    }
}
