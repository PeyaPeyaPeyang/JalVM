package tokyo.peya.langjal.vm.values.metaobjects;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.compiler.jvm.MethodDescriptor;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threading.VMMainThread;
import tokyo.peya.langjal.vm.engine.threading.VMThreadGroup;
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
        VMObject owner = this.getOwner();
        // VM 側でスレッドを新規作成する場合，情報を収集して VMThread を生成する。
        if (this.thread != null)
            throw new IllegalStateException("Thread already started or created by VM, not by user.");
        else if (owner == null)
            throw new IllegalStateException("Thread created by VM must have an owner.");

        VMThread newThread = this.createNewThread();
        this.fieldHolder.setVMCreatedThread(newThread);

        // run() メソッドを呼び出す
        VMMethod runMethod = owner.getObjectType().findMethod("run", MethodDescriptor.parse("()V"));
        if (runMethod == null)
            throw new IllegalStateException("Thread owner does not have run() method.");

        newThread.createFrame(runMethod, false);
    }

    private VMThread createNewThread()
    {
        VMThreadFieldHolderObject holder = (VMThreadFieldHolderObject) this.getField("holder");
        String name = ((VMStringObject) this.getField("name")).getString();
        boolean daemon = ((VMBoolean) holder.getField("daemon")).asBoolean();
        int priority = ((VMInteger) holder.getField("priority")).asNumber().intValue();
        VMThreadGroup group = ((VMThreadGroupObject) holder.getField("group")).getGroup();
        // long stackSize = ((VMLong) holder.getField("stackSize")).asNumber().longValue();

        this.fieldHolder = holder;
        VMThread thread = this.thread = group.createNewThread(name);
        thread.setDaemon(daemon);
        thread.setPriority(priority);
        // stackSize は無視する。JalVM ではスタックサイズを制御しない。

        // 作ったスレッドと，既存のスレッドの整合性を検査する
        long tid = ((VMLong) this.getField("tid")).asNumber().longValue();
        if (tid != thread.getId())
            throw new IllegalStateException("Thread ID mismatch, collapsed system integrity: expected " + tid + ", got " + thread.getId());

        return thread;
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
