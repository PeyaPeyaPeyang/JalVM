package tokyo.peya.langjal.vm.engine.stacking;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.exceptions.IllegalOperandPanic;
import tokyo.peya.langjal.vm.exceptions.StackOverflowPanic;
import tokyo.peya.langjal.vm.exceptions.StackUnderflowPanic;
import tokyo.peya.langjal.vm.values.VMValue;

import java.util.Stack;

public class VMStack {
    private final int maxSize;
    private final Stack<VMValue> stack;

    public VMStack(int maxSize) {
        if (maxSize < 0)
            this.maxSize = Integer.MAX_VALUE;
        else
            this.maxSize = maxSize;
        this.stack = new Stack<>();
    }

    public boolean isEmpty() {
        return this.stack.isEmpty();
    }

    public boolean isFull() {
        return this.stack.size() >= this.maxSize;
    }

    public void push(@NotNull VMValue value) {
        if (isFull()) {
            throw new StackOverflowPanic("Stack is full");
        }
        this.stack.push(value);
    }


    public VMValue pop() {
        if (this.isEmpty())
            throw new StackUnderflowPanic("Stack underflow.");
        return this.stack.pop();
    }

    public VMValue peek() {
        if (this.isEmpty())
            throw new StackUnderflowPanic("Stack is empty.");
        return this.stack.peek();
    }

    public <T extends VMValue> T popType(@NotNull Class<? extends T> type) {
        if (this.isEmpty())
            throw new StackUnderflowPanic("Stack underflow.");
        VMValue value = this.stack.pop();
        if (!type.isInstance(value)) {
            throw new IllegalOperandPanic(
                    "Expected value of type " + type.getSimpleName() + ", but got " + value.getClass().getSimpleName()
            );
        }
        return type.cast(value);
    }

    public int size() {
        return this.stack.size();
    }

    @Override
    public String toString() {
        return "[" + stack.stream()
                .map(VMValue::toString)
                .reduce((a, b) -> a + ", " + b)
                .orElse("") + "]";
    }
}
