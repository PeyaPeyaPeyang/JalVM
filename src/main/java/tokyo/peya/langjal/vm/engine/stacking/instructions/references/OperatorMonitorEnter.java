package tokyo.peya.langjal.vm.engine.stacking.instructions.references;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.engine.threading.VMMonitor;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMReferenceValue;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorMonitorEnter extends AbstractInstructionOperator<InsnNode>
{

    public OperatorMonitorEnter()
    {
        super(EOpcodes.MONITORENTER, "monitorenter");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        VMReferenceValue referenceValue = frame.getStack().popType(VMType.ofGenericObject(frame));
        if (!(referenceValue instanceof VMObject vmObject))
            throw new VMPanic("MONITORENTER requires an object reference, but got: " + referenceValue);

        VMMonitor monitor = vmObject.getMonitor();
        monitor.acquire(frame.getThread());
    }
}
