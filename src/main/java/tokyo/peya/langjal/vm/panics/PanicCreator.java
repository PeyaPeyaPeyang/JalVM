package tokyo.peya.langjal.vm.panics;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMStackTraceElementObject;
import tokyo.peya.langjal.vm.values.metaobjects.VMStringObject;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@UtilityClass
public class PanicCreator
{
    public static InternalErrorVMPanic createInternalPanic(@NotNull VMFrame frame, @NotNull String message, @Nullable VMObject cause)
    {
        VMClass internalErrorClass = frame.getClassLoader().findClass(
                ClassReference.of("java/lang/InternalError")
        );

        createThrowable(frame, internalErrorClass, message, cause);
        return new InternalErrorVMPanic(message, createThrowable(frame, internalErrorClass, message, cause));
    }

    public static IllegalOperationPanic createIllegalOperationPanic(@NotNull VMFrame frame, @NotNull String message, @Nullable VMObject cause)
    {
        VMClass verificationErrorClass = frame.getClassLoader().findClass(
                ClassReference.of("java/lang/VerifyError")
        );
        return new IllegalOperationPanic(message, createThrowable(frame, verificationErrorClass, message, cause));
    }

    public static IllegalOperationPanic createIllegalOperationPanic(@NotNull VMFrame frame, @NotNull String message)
    {
        return createIllegalOperationPanic(frame, message, null);
    }

    public static CodeThrownVMPanic createArrayIndexOutOfBoundsPanic(@NotNull VMFrame frame, int index, int arrayLength)
    {
        String message = "Array index out of bounds: " + index + " for length " + arrayLength;
        VMClass exceptionClass = frame.getClassLoader().findClass(
                ClassReference.of("java/lang/ArrayIndexOutOfBoundsException")
        );
        return new CodeThrownVMPanic(createThrowable(frame, exceptionClass, message, null));
    }

    public static VMArray collectStackTrace(@NotNull VMFrame frame, int depth)
    {
        if (depth < 0)
            throw new VMPanic("NegativeArraySizeException");
        else if (depth == 0)
            depth = Integer.MAX_VALUE;

        // 現在のフレームは Throwable.fillInStackTrace() のフレームなので，new MyException() のところまで遡る。
        if (!frame.getMethod().getName().equalsIgnoreCase("fillInStackTrace"))
            throw new VMPanic("PanicCreator.collectStackTrace must be called from Throwable.fillInStackTrace");

        VMObject exceptionInstance = (VMObject) frame.getLocals().getLocal(0, null);
        VMClass exceptionClass = exceptionInstance.getObjectType();

        VMFrame prev = frame.getPrevFrame();
        while (prev != null && prev.getMethod().isConstructor() && prev.getMethod().getClazz().isAssignableFrom(exceptionClass))
            prev = prev.getPrevFrame();

        if (prev == null)
            return new VMArray(
                    frame,
                    frame.getClassLoader().findClass(ClassReference.of("java/lang/StackTraceElement")),
                    new VMValue[0]
            );

        List<VMStackTraceElementObject> elements = new ArrayList<>();
        int currentDepth = 0;
        // 上記のループを抜けた時点で， current は new MyException() のフレームを指しているはず。
        // なので，その一つ前のフレームが，例外をスローしたフレームになる。
        VMFrame currentFrame = prev;
        do
        {
            VMMethod method = currentFrame.getMethod();
            int currentInstruction = currentFrame.getInterpreter().getCurrentInstructionIndex();
            int currentLineNumber = currentFrame.getInterpreter().getLineNumberOf(currentInstruction);

            elements.add(new VMStackTraceElementObject(
                    frame.getVM(),
                    method,
                    currentLineNumber
            ));

            currentFrame = currentFrame.getPrevFrame();
        } while (currentFrame != null && ++currentDepth < depth);

        return new VMArray(
                frame,
                frame.getClassLoader().findClass(ClassReference.of("java/lang/StackTraceElement")),
                elements.toArray(new VMValue[0])
        );
    }

    public static VMObject copyThrowable(@NotNull VMComponent com,
                                         @NotNull Throwable throwable)
    {
        VMObject obj = com.getClassLoader().findClass(
                ClassReference.of("java/lang/Throwable")
        ).createInstance();
        obj.setField("detailMessage", VMStringObject.createString(com, throwable.getMessage()));
        obj.setField("stackTrace", new VMArray(
                com,
                com.getClassLoader().findClass(ClassReference.of("java/lang/StackTraceElement")),
                Arrays.stream(throwable.getStackTrace())
                        .map(s -> new VMStackTraceElementObject(com.getVM(), s))
                        .toArray(VMValue[]::new)
        ));
        if (throwable.getCause() != null)
            obj.setField("cause", copyThrowable(com, throwable.getCause()));

        obj.forceInitialise(com.getClassLoader());
        return obj;
    }


    private static VMObject createThrowable(@NotNull VMFrame frame, @NotNull VMClass internalErrorClass, @NotNull String message, @Nullable VMObject cause)
    {
        VMObject obj = internalErrorClass.createInstance();
        VMObject throwableObj = obj;
        do
        {
            VMObject superCandidate = throwableObj.getSuperObject();
            if (superCandidate == null)
                throw new InternalErrorVMPanic("Class " + throwableObj.getObjectType().getClazz().name + " does not extend Throwable.");
            if (superCandidate.getObjectType().getClazz().name.equals("java/lang/Throwable"))
                break;

            throwableObj = superCandidate;
        } while (true);

        throwableObj.setField("detailMessage", VMStringObject.createString(frame, message));
        throwableObj.setField("stackTrace", collectStackTrace(frame, Integer.MAX_VALUE));
        if (cause != null)
            throwableObj.setField("cause", cause);

        obj.forceInitialise(frame.getClassLoader());
        return obj;
    }
}
