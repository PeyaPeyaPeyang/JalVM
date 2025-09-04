package tokyo.peya.langjal.vm.values.metaobjects;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.VMThreadGroup;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMBoolean;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMObject;

@Getter
public class VMThreadObject extends VMObject
{
    private final JalVM vm;

    private VMThreadFieldHolderObject fieldHolder;
    private VMThread thread;

    @Setter
    private int insideVMPriority;  // setPriority0 で設定された値を**一時的に**保持する。

    public VMThreadObject(@NotNull VMComponent com, @NotNull VMThread thread)
    {
        super(com.getClassLoader().findClass(ClassReference.of("java/lang/Thread")));
        this.vm = com.getVM();
        this.thread = thread;
        this.fieldHolder = new VMThreadFieldHolderObject(com, thread);

        this.setField("holder", this.fieldHolder);
        this.setField("name", VMStringObject.createString(com, thread.getName()));
        this.setField("tid", new VMLong(com, thread.getId()));

        this.setField("scopedValueBindings", this.getObjectType().getClassObject());

        this.forceInitialise(com.getClassLoader());
    }

    public VMThreadObject(@NotNull VMComponent com, @Nullable VMObject owner)  // VM が作成する場合
    {
        super(com.getClassLoader().findClass(ClassReference.of("java/lang/Thread")), owner);
        this.vm = com.getVM();
    }

    public void startNewThreadByVM()
    {
        // VM 側でスレッドを新規作成する場合，情報を収集して VMThread を生成する。
        if (this.thread != null)
            throw new IllegalStateException("Thread already started");

        VMThreadFieldHolderObject holder = (VMThreadFieldHolderObject) this.getField("holder");
        String name = ((VMStringObject) this.getField("name")).getString();
        boolean daemon = ((VMBoolean) holder.getField("daemon")).asBoolean();
        int priority = ((VMInteger) holder.getField("priority")).asNumber().intValue();
        VMThreadGroup group = ((VMThreadGroupObject) holder.getField("group")).getGroup();
        long stackSize = ((VMLong) holder.getField("stackSize")).asNumber().longValue();

        this.fieldHolder = holder;
        VMThread thread = this.thread = new VMThread(
                this.vm,
                group,
                name
        );
        thread.setDaemon(daemon);
        thread.setPriority(priority);
        // stackSize は無視する。JalVM ではスタックサイズを制御しない。

        this.fieldHolder.setVMCreatedThread(thread);
        group.addThread(thread);
    }

    @Override
    protected void beforeInitialise(@Nullable VMFrame frame)
    {
        if (frame == null)
            return;  // これは VM が作成する場合
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
