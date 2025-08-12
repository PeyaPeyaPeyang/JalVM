package tokyo.peya.langjal.vm.engine.stacking;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.values.VMValue;

import java.util.Stack;

public class VMStack {
    private final int maxSize;
    private final Stack<VMValue> stack;

    public VMStack(int maxSize) {
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
            throw new StackOverflowError("Stack is full");
        }
        this.stack.push(value);
    }

    public VMValue pop() {
        if (isEmpty())
            throw new IllegalArgumentException("Stack underflow.");
        return this.stack.pop();
    }

    public VMValue peek() {
        if (isEmpty())
            throw new IllegalArgumentException("Stack is empty.");
        return this.stack.peek();
    }

    public int size() {
        return this.stack.size();
    }
}
