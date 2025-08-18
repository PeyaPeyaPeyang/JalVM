package tokyo.peya.langjal.vm.engine.stacking.instructions.references;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import tokyo.peya.langjal.compiler.jvm.AccessAttribute;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.MethodDescriptor;
import tokyo.peya.langjal.compiler.jvm.TypeDescriptor;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.reflect.ReflectUtils;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMMethodHandleLookupObject;

import java.lang.invoke.MethodHandles;

public class OperatorInvokeDynamic extends AbstractInstructionOperator<InvokeDynamicInsnNode>
{

    public OperatorInvokeDynamic()
    {
        super(EOpcodes.INVOKEDYNAMIC, "invokedynamic");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InvokeDynamicInsnNode operand)
    {
        String name = operand.name;
        MethodDescriptor descriptor = MethodDescriptor.parse(operand.desc);

        Handle bsmHandle = operand.bsm;
        Object[] bsmArgs = operand.bsmArgs;
        VMClass bsmClass = frame.getVm().getClassLoader().findClass(ClassReference.of(bsmHandle.getOwner()));
        MethodDescriptor bsmDescriptor = MethodDescriptor.parse(bsmHandle.getDesc());

        VMMethodHandleLookupObject lookup = ReflectUtils.createLookupChain(frame, bsmClass);
    }
}
