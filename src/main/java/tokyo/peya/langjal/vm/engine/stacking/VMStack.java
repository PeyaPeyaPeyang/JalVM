package tokyo.peya.langjal.vm.engine.stacking;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.exceptions.IllegalOperandPanic;
import tokyo.peya.langjal.vm.exceptions.StackOverflowPanic;
import tokyo.peya.langjal.vm.exceptions.StackUnderflowPanic;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

import java.util.Stack;

public class VMStack
{
    private final int maxSize;
    private final Stack<VMValue> stack;

    public VMStack(int maxSize)
    {
        if (maxSize < 0)
            this.maxSize = Integer.MAX_VALUE;
        else
            this.maxSize = maxSize;
        this.stack = new Stack<>();
    }

    public boolean isEmpty()
    {
        return this.stack.isEmpty();
    }

    public boolean isFull()
    {
        return this.stack.size() >= this.maxSize;
    }

    public void push(@NotNull VMValue value)
    {
        // if (this.isFull())
        //     throw new StackOverflowPanic("Stack is full");
        this.stack.push(value);
    }

    public VMValue pop()
    {
        if (this.isEmpty())
            throw new StackUnderflowPanic("Stack underflow.");
        return this.stack.pop();
    }

    public VMValue peek()
    {
        if (this.isEmpty())
            throw new StackUnderflowPanic("Stack is empty.");
        return this.stack.peek();
    }


    @SuppressWarnings("unchecked")  // 型キャストのため
    public <T extends VMValue> T peekType(@NotNull VMType<T> type)
    {
        if (this.isEmpty())
            throw new StackUnderflowPanic("Stack is empty.");
        VMValue value = this.stack.peek();

        VMValue conformedValue = value.conformValue(type); // 値をフィールドの型に適合させる
        if (conformedValue == null)
            throw new IllegalOperandPanic("Expected type: " + type + ", but got: " + value.type());

        return (T) conformedValue;
    }

    public <T extends VMValue> T popType(@NotNull VMType<T> type)
    {
        if (this.isEmpty())
            throw new StackUnderflowPanic("Stack underflow.");
        T value = this.peekType(type);
        this.stack.pop();  // Peekした後にポップする

        return value;
    }

    public int size()
    {
        return this.stack.size();
    }

    @Override
    public String toString()
    {
        return "[" + this.stack.stream()
                               .map(VMValue::toString)
                               .reduce((a, b) -> a + ", " + b)
                               .orElse("") + "]";
    }
}
