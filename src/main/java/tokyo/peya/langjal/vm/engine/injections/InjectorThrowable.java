package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMStackTraceElementObject;

import java.util.ArrayList;
import java.util.List;

public class InjectorThrowable implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("java/lang/Throwable");

    @Override
    public ClassReference suitableClass()
    {
        return CLAZZ;
    }

    @Override
    public void inject(@NotNull VMSystemClassLoader cl, @NotNull VMClass clazz)
    {
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "fillInStackTrace",
                        "(I)Ljava/lang/Throwable;",
                        null,
                        null
                )
                )
                {
                    @Override
                    VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                            @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        assert instance != null;
                        int depth = ((VMInteger) args[0]).asNumber().intValue();
                        if (depth < 0)
                            throw new VMPanic("NegativeArraySizeException");
                        else if (depth == 0)
                            depth = Integer.MAX_VALUE;

                        List<VMStackTraceElementObject> elements = new ArrayList<>();
                        int currentDepth = 0;
                        VMFrame currentFrame = thread.getCurrentFrame();
                        do
                        {
                            VMMethod method = currentFrame.getMethod();
                            int currentInstruction = currentFrame.getInterpreter().getCurrentInstructionIndex();
                            int currentLineNumber = currentFrame.getInterpreter().getLineNumberOf(currentInstruction);

                            elements.add(new VMStackTraceElementObject(
                                    thread.getVM(),
                                    method,
                                    currentLineNumber
                            ));

                            currentFrame = currentFrame.getPrevFrame();
                        } while (currentFrame != null && ++currentDepth < depth);

                        VMArray array = new VMArray(
                                thread.getVM(),
                                cl.findClass(ClassReference.of("java/lang/StackTraceElement")),
                                elements.toArray(new VMValue[0])
                        );

                        instance.setField("stackTrace", array);
                        return null;
                    }
                }
        );
    }

}
