package tokyo.peya.langjal.vm.engine;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMInterpreter;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.stacking.VMStack;
import tokyo.peya.langjal.vm.engine.threads.VMThread;

@Getter
public class VMFrame {
    private final JalVM vm;
    private final VMThread engine;
    private final VMMethod method;

    private final VMStack stack;
    private final VMLocals locals;

    private final VMFrame prevFrame;
    private VMFrame nextFrame;

    private VMInterpreter interpreter;
    private boolean isRunning;

    public VMFrame(
            @NotNull JalVM vm,
            @NotNull VMThread engine,
            @Nullable VMFrame prevFrame,
            @NotNull VMMethod method) {
        this.vm = vm;
        this.engine = engine;
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
        this.interpreter = this.method.createInterpreter(this.vm, this.engine, this);
    }

    public void heartbeat() {
        if (!this.isRunning) {
            throw new IllegalStateException("Frame is not running.");
        }

        if (this.interpreter.hasNextInstruction()) {
            this.interpreter.feedNextInstruction();
        } else {
            this.isRunning = false;
            this.engine.restoreFrame();
        }
    }
}
