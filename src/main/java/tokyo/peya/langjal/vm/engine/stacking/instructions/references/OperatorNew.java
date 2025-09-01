package tokyo.peya.langjal.vm.engine.stacking.instructions.references;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.TypeInsnNode;
import tokyo.peya.langjal.compiler.jvm.ClassReferenceType;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMObject;

public class OperatorNew extends AbstractInstructionOperator<TypeInsnNode>
{

    public OperatorNew()
    {
        super(EOpcodes.NEW, "new");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull TypeInsnNode operand)
    {
        String descriptor = operand.desc;
        ClassReferenceType classReferenceType = ClassReferenceType.parse(descriptor);

        VMClass clazz = frame.getClassLoader().findClass(ClassReference.of(classReferenceType));
        if (!clazz.isInitialised())
        {
            // クラスが初期化されていない場合は初期化をして，この命令を再実行する
            clazz.initialise(frame.getThread());
            frame.rerunInstruction();
            return;
        }

        VMObject newObject = clazz.createInstance();
        frame.getTracer().pushHistory(
                ValueTracingEntry.generation(newObject, frame.getMethod(), operand)
        );

        frame.getStack().push(newObject);
    }
}
