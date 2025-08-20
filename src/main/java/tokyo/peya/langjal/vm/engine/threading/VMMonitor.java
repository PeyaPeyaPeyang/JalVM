package tokyo.peya.langjal.vm.engine.threading;

import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.api.events.VMThreadMonitorAcquireListedEvent;
import tokyo.peya.langjal.vm.api.events.VMThreadMonitorAcquiredEvent;
import tokyo.peya.langjal.vm.api.events.VMThreadNotifiedEvent;
import tokyo.peya.langjal.vm.api.events.VMThreadWaitingEvent;
import tokyo.peya.langjal.vm.values.VMObject;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Getter
public class VMMonitor
{
    private final VMObject associated;
    @Getter(AccessLevel.NONE)
    private final List<VMThreadWaitingEntry> acquireWaitlist;
    @Getter(AccessLevel.NONE)
    private final List<VMThreadWaitingEntry> notifyWaitlist;

    private VMThread owner;

    public VMMonitor(@NotNull VMObject associated)
    {
        this.associated = associated;
        this.acquireWaitlist = new ArrayList<>();
        this.notifyWaitlist = new ArrayList<>();
        this.owner = null;
    }

    public boolean acquire(@NotNull VMThread thread)
    {
        if (this.owner == null)
        {
            this.setOwner(thread); // モニタの所有者を設定
            return true;
        }

        VMThreadMonitorAcquireListedEvent listedEvent =
                new VMThreadMonitorAcquireListedEvent(thread.getVm(), thread, this, this.acquireWaitlist.size());
        thread.getVm().getEventManager().dispatchEvent(listedEvent);

        this.acquireWaitlist.add(VMThreadWaitingEntry.acquire(thread));

        return false; // モニタはすでに他のスレッドによって所有されている
    }

    public void waitFor(@NotNull VMThread thread)
    {
        VMThreadWaitingEvent waitingEvent =
                new VMThreadWaitingEvent(thread.getVm(), thread, this, Duration.ZERO);
        thread.getVm().getEventManager().dispatchEvent(waitingEvent);

        thread.setState(VMThreadState.WAITING);
        this.notifyWaitlist.add(VMThreadWaitingEntry.wait(thread));
    }

    public void waitForTimed(@NotNull VMThread thread, @NotNull Duration duration)
    {
        VMThreadWaitingEvent waitingEvent =
                new VMThreadWaitingEvent(thread.getVm(), thread, this, duration);
        thread.getVm().getEventManager().dispatchEvent(waitingEvent);

        thread.setState(VMThreadState.TIMED_WAITING);
        this.notifyWaitlist.add(VMThreadWaitingEntry.waitTimed(thread, duration));
    }

    public void notifyOne()
    {
        if (this.notifyWaitlist.isEmpty())
            return; // 待機リストが空なら何もしない

        VMThreadWaitingEntry entry = this.notifyWaitlist.removeFirst(); // 最初の待機エントリを取得
        this.notifyFor0(entry.thread());

        // 待機リストから削除
        this.notifyWaitlist.remove(entry);
    }

    private void notifyFor0(@NotNull VMThread thread)
    {
        // スレッドがモニタの所有者でない場合は何もしない
        if (!this.isOwner(thread))
            return;

        VMThreadNotifiedEvent notifiedEvent =
                new VMThreadNotifiedEvent(thread.getVm(), thread, this);
        thread.getVm().getEventManager().dispatchEvent(notifiedEvent);

        thread.setState(VMThreadState.RUNNABLE); // スレッドを実行可能状態に設定
    }

    public void notifyForAll()
    {
        // モニタの待機リストから全てのスレッドを削除
        for (VMThreadWaitingEntry entry : this.notifyWaitlist)
            this.notifyFor0(entry.thread());

        this.notifyWaitlist.clear(); // 待機リストを空にする
    }

    private static void removeFromWaitlist(@NotNull VMThread thread, @NotNull List<VMThreadWaitingEntry> waitList)
    {
        for (int i = 0; i < waitList.size(); i++)
        {
            VMThreadWaitingEntry entry = waitList.get(i);
            if (entry.thread() == thread)
            {
                waitList.remove(i); // スレッドを待機リストから削除
                return;
            }
        }
    }

    public boolean release(@NotNull VMThread thread)
    {
        // モニタの待ちリストから削除
        removeFromWaitlist(thread, this.acquireWaitlist);

        boolean isOwner = this.owner == thread;
        if (isOwner)
        {
            if (this.acquireWaitlist.isEmpty())
                this.owner = null; // 待機リストが空なら所有者をnullに設定
            else
            {
                VMThreadWaitingEntry nextWait = this.acquireWaitlist.removeFirst(); // 待機リストの最初のスレッドを所有者に設定
                this.setOwner(nextWait.thread());
            }
        }

        return isOwner; // スレッドがモニタの所有者であれば解放成功
    }

    public boolean isAcquireWaiting(@NotNull VMThread thread)
    {
       return this.acquireWaitlist.stream().anyMatch(entry -> entry.thread() == thread);
    }

    public boolean isNotifyWaiting(@NotNull VMThread thread)
    {
        return this.notifyWaitlist.stream().anyMatch(entry -> entry.thread() == thread);
    }

    public VMThreadWaitingEntry getNotifyWaitingRecord(@NotNull VMThread thread)
    {
        return this.notifyWaitlist.stream()
                .filter(entry -> entry.thread() == thread)
                .findFirst()
                .orElse(null); // スレッドが待機していない場合はnullを返す
    }

    private void setOwner(@NotNull VMThread thread)
    {
        VMThreadMonitorAcquiredEvent acquiredEvent =
                new VMThreadMonitorAcquiredEvent(thread.getVm(), thread, this);
        thread.getVm().getEventManager().dispatchEvent(acquiredEvent);
        this.owner = thread; // スレッドをモニタの所有者として設定
    }

    public boolean isOwner(@NotNull VMThread thread)
    {
        return this.owner == thread; // スレッドがモニタの所有者かどうかを確認
    }
}
