package tokyo.peya.langjal.vm.engine;

import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.events.VMThreadDeathEvent;
import tokyo.peya.langjal.vm.api.events.VMThreadGroupHeartbeatEvent;
import tokyo.peya.langjal.vm.api.events.VMThreadHeartbeatEvent;
import tokyo.peya.langjal.vm.api.events.VMThreadStartEvent;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.engine.threading.VMThreadState;
import tokyo.peya.langjal.vm.tracing.ThreadManipulationType;
import tokyo.peya.langjal.vm.tracing.ThreadTracingEntry;
import tokyo.peya.langjal.vm.tracing.VMThreadTracer;
import tokyo.peya.langjal.vm.values.metaobjects.VMThreadGroupObject;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@Getter
public class VMThreadGroup implements VMComponent
{
    @Getter(AccessLevel.NONE)
    private final JalVM vm;
    private final VMThreadTracer tracer;

    private final String name;
    private final VMThreadGroup parent;
    @Getter(AccessLevel.NONE)
    private final LinkedList<VMThreadGroup> children;
    @Getter(AccessLevel.NONE)
    private final LinkedList<VMThread> threads;

    private final VMThreadGroupObject object;

    private int maxPriority;
    private boolean daemon;

    private Iterator<VMThreadGroup> childIterator;
    private Iterator<VMThread> threadIterator;

    private VMThreadGroup currentChildGroup;
    private VMThread currentThread;

    private VMThreadGroup(@NotNull JalVM vm, @NotNull String name, int maxPriority, @Nullable VMThreadGroup parent)
    {
        if (parent == null && !name.equals("system"))
            throw new IllegalArgumentException("Only the system thread group can have no parent");

        this.vm = vm;
        this.tracer = parent == null ? new VMThreadTracer() : parent.tracer;

        this.name = name;
        this.maxPriority = maxPriority;
        this.parent = parent;
        this.daemon = false;
        this.children = new LinkedList<>();
        this.threads = new LinkedList<>();
        this.childIterator = Collections.emptyIterator();
        this.threadIterator = Collections.emptyIterator();

        this.object = new VMThreadGroupObject(vm, this);
    }

    public VMThreadGroup(@NotNull JalVM vm)
    {
        this(vm, "system", Thread.MAX_PRIORITY, null);
    }

    public boolean isThreadsRunning()
    {
        return !(this.threads.isEmpty() || this.threads.stream().allMatch(VMThread::isDaemon));
    }

    public boolean isChildrenRunning()
    {
        for (VMThreadGroup child : this.children)
        {
            // 子が daemon グループで、かつ中のスレッドも全部 daemon なら無視
            if (!child.daemon && child.isRunning())
                return true;

            // 再帰的に確認
            if (child.isChildrenRunning())
                return true;
        }

        return false;
    }

    public boolean isRunning()
    {
        return this.isThreadsRunning() || this.isChildrenRunning();
    }

    public static VMThreadGroup createChild(@NotNull JalVM vm, @NotNull String name, int maxPriority, @NotNull VMThreadGroup parent)
    {
        VMThreadGroup group = new VMThreadGroup(vm, name, maxPriority, parent);
        parent.children.add(group);
        parent.object.syncFields();
        return group;
    }

    private static void removeChild(@NotNull VMThreadGroup group, @NotNull VMThreadGroup child)
    {
        group.children.remove(child);
        group.object.syncFields();
    }

    private void renewIterators()
    {
        if (!this.threads.isEmpty())
            this.threadIterator = this.threads.iterator();
        if (!this.children.isEmpty())
            this.childIterator = this.children.iterator();
    }

    public void heartbeat()
    {
        // 永遠に回し続ける
        if (!(this.threadIterator.hasNext() || this.childIterator.hasNext()))
            this.renewIterators();

        if (this.threadIterator.hasNext())
        {
            // スレッドを優先的に回す
            this.heartbeatThread(this.currentThread = this.threadIterator.next());
            this.currentThread = null;
        }
        else if (this.childIterator.hasNext())
        {
            // 子スレッドがなければ，子スレッドグループを回す
            this.heartbeatGroup(this.currentChildGroup = this.childIterator.next());
            this.currentChildGroup = null;
        }

        // このあと，再度子スレッドグループとスレッドを回すためにイテレータがリセットされる
    }

    private void heartbeatThread(@NotNull VMThread current)
    {
        if (current.getState() == VMThreadState.TERMINATED)
        {
            System.out.println("Thread " + current.getName() + " has terminated. Cleaning up...");
            this.killThread(current);
            this.currentThread = null;
            return;
        }

        this.vm.getEventManager().dispatchEvent(new VMThreadHeartbeatEvent(this.vm, current));
        try
        {
            current.heartbeat();
        }
        catch (Throwable e)
        {
            System.err.println("Error in thread " + current.getName() + ": " + e.getMessage());
            e.printStackTrace();
            this.killThread(current);
        }
    }

    private void heartbeatGroup(@NotNull VMThreadGroup current)
    {
        if (current.isThreadsRunning())
        {
            // 子スレッドグループにスレッドが一つもない場合は削除する
            this.killThreadGroup(current);
            return;
        }

        this.vm.getEventManager().dispatchEvent(new VMThreadGroupHeartbeatEvent(this.vm, current));
        current.heartbeat();
    }

    public void addThread(@NotNull VMThread thread)
    {
        if (this.threads.contains(thread))
            throw new IllegalStateException("Thread already exists in the engine.");

        this.vm.getEventManager().dispatchEvent(new VMThreadStartEvent(this.vm, thread));

        this.threads.add(thread);
        this.tracer.pushHistory(
                new ThreadTracingEntry(
                        ThreadManipulationType.CREATION,
                        thread
                )
        );
    }

    public void killThread(@NotNull VMThread thread)
    {
        if (!this.threads.contains(thread))
            throw new IllegalStateException("Thread does not exist in the engine.");
        this.threads.remove(thread);
        thread.kill();
        this.tracer.pushHistory(
                new ThreadTracingEntry(
                        ThreadManipulationType.DESTRUCTION,
                        thread
                )
        );

        this.vm.getEventManager().dispatchEvent(new VMThreadDeathEvent(this.vm, thread));
    }

    public void killThreadGroup(@NotNull VMThreadGroup group)
    {
        if (!this.children.contains(group))
            throw new IllegalStateException("Thread does not exist in the engine.");

        this.children.remove(group);
    }

    public List<VMThreadGroup> getChildren()
    {
        return Collections.unmodifiableList(this.children);
    }

    public List<VMThread> getThreads()
    {
        return Collections.unmodifiableList(this.threads);
    }

    @Override
    public @NotNull JalVM getVM()
    {
        return this.vm;
    }

    @Nullable
    public VMThread getCurrentHeartBeatingThread()
    {
        if (this.currentThread != null)  // このスレッドグループでハートビート中のスレッドがいる場合
            return this.currentThread;
        if (this.currentChildGroup != null)  // このスレッドグループでハートビート中の子スレッドグループがいる場合
            return this.currentChildGroup.getCurrentHeartBeatingThread();

        return null;  // どちらでもない場合
    }

    public void setMaxPriority(int maxPriority)
    {
        this.maxPriority = Math.min(maxPriority, Thread.MAX_PRIORITY);
        this.object.syncFields();
    }

    public void setDaemon(boolean daemon)
    {
        this.daemon = daemon;
        this.object.syncFields();
    }
}
