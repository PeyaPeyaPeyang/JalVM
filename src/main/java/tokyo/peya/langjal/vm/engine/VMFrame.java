package tokyo.peya.langjal.vm.engine;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;
import tokyo.peya.langjal.compiler.jvm.AccessAttribute;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMInterpreter;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.stacking.VMStack;
import tokyo.peya.langjal.vm.engine.stacking.VMStackMachine;
import tokyo.peya.langjal.vm.engine.threads.VMThread;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

@Getter
public class VMFrame {
    private final JalVM vm;
    private final VMThread thread;
    private final boolean isVMDecree;
    private final VMMethod method;
    private final VMValue[] args;

    private final VMStack stack;
    private final VMLocals locals;

    private final VMFrame prevFrame;
    private VMFrame nextFrame;

    private VMInterpreter interpreter;
    private boolean isRunning;

    @Setter
    private VMValue returnValue;

    public VMFrame(
            @NotNull JalVM vm,
            @NotNull VMThread thread,
            boolean isVMDecree,
            @NotNull VMMethod method,
            @NotNull VMValue[] args,
            @Nullable VMFrame prevFrame) {
        this.vm = vm;
        this.thread = thread;
        this.isVMDecree = isVMDecree;  // VMが決めたフレームかどうか
        this.method = method;
        this.args = args;
        this.prevFrame = prevFrame;

        checkArgumentTypes(method, args);
        this.stack = new VMStack(method.getMaxStackSize());
        this.locals = new VMLocals(method.getMaxLocals(), args);
    }

    private static void checkArgumentTypes(@NotNull VMMethod method, @NotNull VMValue[] args) {
        VMType[] parameterTypes = method.getParameterTypes();
        int expectedArgs = parameterTypes.length;
        int actualArgs = args.length;

        boolean needThis = !method.getAccessAttributes().has(AccessAttribute.STATIC);
        if (needThis)
            expectedArgs++;  // インスタンスメソッドの場合、this引数があるので1つ多い
        boolean canOmitLastArray = method.getAccessAttributes().has(AccessAttribute.VARARGS);
        if (canOmitLastArray && expectedArgs > 0) {
            VMType lastType = parameterTypes[expectedArgs - 1];
            if (lastType.getArrayDimensions() > 0 && actualArgs + 1 == expectedArgs) {
                // 最後の引数が配列である場合、最後の引数を省略できる
                expectedArgs--;
            }
        }

        if (expectedArgs != actualArgs)
            throw new VMPanic("Method " + method.getMethodNode().name + " expects " + expectedArgs +
                    " arguments, but got " + actualArgs + (needThis ? ", missing 'this' at first?" : "."));

        for (int i = 0; i < parameterTypes.length; i++)
            if (!args[i].getType().isAssignableFrom(parameterTypes[i]))
                throw new VMPanic("Argument " + i + " of method " + method.getMethodNode().name +
                        " is of type " + args[i].getType() + ", but expected " + parameterTypes[i] + ".");
    }

    public void setNextFrame(@NotNull VMFrame nextFrame) {
        this.nextFrame = nextFrame;
    }

    public void activate() {
        if (this.isRunning)
            throw new VMPanic("Frame is already running.");
        else if (this.interpreter != null)
            throw new VMPanic("Frame has already started by other interpreter.");

        this.interpreter = this.method.createInterpreter(this.vm, this.thread, this);
        this.isRunning = true;
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
            // 実行が終わったら，フレームを戻す
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
