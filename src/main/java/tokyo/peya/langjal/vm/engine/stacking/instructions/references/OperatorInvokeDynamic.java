package tokyo.peya.langjal.vm.engine.stacking.instructions.references;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.MethodDescriptor;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.stacking.VMStack;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMClassObject;

public class OperatorInvokeDynamic extends AbstractInstructionOperator<InvokeDynamicInsnNode>
{
    private final DynamicInvocationCallSiteResolver resolver;

    public OperatorInvokeDynamic()
    {
        super(EOpcodes.INVOKEDYNAMIC, "invokedynamic");

        this.resolver = new DynamicInvocationCallSiteResolver();
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InvokeDynamicInsnNode operand)
    {
        String name = operand.name;
        MethodDescriptor descriptor = MethodDescriptor.parse(operand.desc);
        VMObject callSite = this.resolver.resolveCallSite(frame, name, descriptor, operand);
        if (callSite == null)
        {
            // まだ解決されていない場合は，この命令を再実行する
            frame.rerunInstruction();
            return;
        }

        // CallSite から MethodHandle を取得して実行する
        VMObject methodHandle = (VMObject) callSite.getField("target");
        VMObject methodType = (VMObject) methodHandle.getField("type");

        // 引数を集めるために，MethodType からパラメータ型を取得する
        VMArray pTypes = (VMArray) methodType.getField("ptypes");
        VMClass[] paramTypes = new VMClass[pTypes.length()];
        for (int i = 0; i < pTypes.length(); i++)
            paramTypes[i] = ((VMClassObject) pTypes.get(i)).getRepresentingClass();

        // 引数をスタックから取得する
        VMStack stack = frame.getStack();
        VMValue[] args = new VMValue[paramTypes.length];
        for (int i = paramTypes.length - 1; i >= 0; i--)
            args[i] = stack.popType(paramTypes[i]);

        // メソッドを呼び出す
        VMClass methodHandleClass = frame.getClassLoader().findClass(ClassReference.of("java/lang/invoke/MethodHandle"));
        VMMethod invokeExact = methodHandleClass.findMethod("invokeExact");
        if (invokeExact == null)
            throw new VMPanic("MethodHandle.invokeExact not found");

        VMClass caller = frame.getMethod().getClazz();
        invokeExact.invokeInstanceMethod(
                operand,
                frame,
                caller,
                methodHandle,
                false,
                args
        );
    }

}
