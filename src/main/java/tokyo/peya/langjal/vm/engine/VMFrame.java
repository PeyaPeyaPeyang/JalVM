package tokyo.peya.langjal.vm.engine;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMInterpreter;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.stacking.VMStack;
import tokyo.peya.langjal.vm.engine.stacking.VMStackMachine;
import tokyo.peya.langjal.vm.engine.threads.VMThread;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

@Getter
public class VMFrame {
    private final JalVM vm;
    private final VMThread thread;
    private final VMMethod method;

    private final VMStack stack;
    private final VMLocals locals;

    private final VMFrame prevFrame;
    private VMFrame nextFrame;

    private VMInterpreter interpreter;
    private boolean isRunning;

    public VMFrame(
            @NotNull JalVM vm,
            @NotNull VMThread thread,
            @Nullable VMFrame prevFrame,
            @NotNull VMMethod method) {
        this.vm = vm;
        this.thread = thread;
        this.prevFrame = prevFrame;
        this.method = method;
        this.stack = new VMStack(method.getMaxStackSize());
        this.locals = new VMLocals(method.getMaxLocals());
    }

    public void setNextFrame(@NotNull VMFrame nextFrame) {
        this.nextFrame = nextFrame;
    }

    public void startRunning() {
        if (this.isRunning)
            throw new IllegalStateException("Frame is already running.");
        else if (this.interpreter != null)
            throw new IllegalStateException("Frame has already started by other interpreter.");

        this.isRunning = true;
        this.interpreter = this.method.createInterpreter(this.vm, this.thread, this);
    }

    public void heartbeat() {
        if (!this.isRunning) {
            throw new VMPanic("Frame is not running.");
        }

        if (this.interpreter.hasNextInstruction()) {
            try {
                AbstractInsnNode next = this.interpreter.feedNextInstruction();
                if (next == null)
                    return;  // そういうこともある。

                VMStackMachine.executeInstruction(this, next);
            } catch (Throwable e) {
                System.err.println("Error while executing instruction in frame: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            this.isRunning = false;
            this.thread.restoreFrame();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        VMFrame current = this.thread.getFirstFrame();
        while (current != null) {
            sb.append(current.getMethod().getClazz().getReference())
                    .append("->")
                    .append(current.getMethod().getMethodNode().name)
                    .append(current.getMethod().getMethodNode().desc);
            if (current.nextFrame != null) {
                sb.append(" -> ");
                current = current.nextFrame;
            }

            if (current == this) {
                sb.append(" (me!)");
                break;
            }
        }

        return sb.toString();
    }
}
