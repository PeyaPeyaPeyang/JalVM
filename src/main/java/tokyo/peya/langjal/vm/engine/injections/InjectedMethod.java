package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMValue;

public abstract class InjectedMethod extends VMMethod
{
    public InjectedMethod(@NotNull VMClass clazz, @NotNull MethodNode methodNode)
    {
        super(
                clazz.getVM(),
                clazz,
                retrieveOriginalSlot(clazz, methodNode),
                methodNode
        );
    }

    private static int retrieveOriginalSlot(@NotNull VMClass clazz, @NotNull MethodNode node)
    {
        return clazz.getMethods().stream()
                    .filter(f -> f.getName().equals(node.name) && f.getDescriptor().equals(node.desc))
                    .findFirst()
                    .map(VMMethod::getSlot)
                    .orElseThrow(() -> new IllegalStateException("Original method ID not found for " + node.name));
    }

    @Override
    public void invokeInstanceMethod(@Nullable MethodInsnNode operand, @NotNull VMThread thread, @Nullable VMClass caller, @NotNull VMObject instance,
                                     boolean isVMDecree, @NotNull VMValue... args)
    {
        // インジェクションされたメソド内で違うフレームを作る可能性があるため，ここで保持しておく。
        VMFrame currentFrame = thread.getCurrentFrame();
        VMValue returning = this.invoke(thread, caller, instance, args);
        if (returning != null)
        {
            if (operand != null)
                currentFrame.getTracer().pushHistory(
                        ValueTracingEntry.returning(
                                returning,
                                this,
                                operand
                        )
                );
            currentFrame.getStack().push(returning);
        }
    }

    @Override
    public void invokeStatic(@Nullable MethodInsnNode operand, @NotNull VMThread thread, @Nullable VMClass caller, boolean isVMDecree,
                             @NotNull VMValue... args)
    {
        // インジェクションされたメソド内で違うフレームを作る可能性があるため，ここで保持しておく。
        VMFrame currentFrame = thread.getCurrentFrame();
        VMValue returning = this.invoke(thread, caller, null, args);
        if (returning != null)
        {
            if (operand != null)
                currentFrame.getTracer().pushHistory(
                        ValueTracingEntry.returning(
                                returning,
                                this,
                                operand
                        )
                );
            currentFrame.getStack().push(returning);
        }
    }

    @Nullable
    abstract VMValue invoke(
            @NotNull VMThread thread,
            @Nullable VMClass caller,
            @Nullable VMObject instance,
            @NotNull VMValue[] args);
}
