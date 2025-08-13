package tokyo.peya.langjal.vm.low;

import tokyo.peya.langjal.compiler.jvm.MethodDescriptor;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

public final class FunctionHandle {
    static {
        System.loadLibrary("nativebridge"); // さっきのネイティブライブラリ
    }

    private final long funcPtr;
    private final MethodDescriptor methodDescriptor; // 例: "(IDLjava/lang/String;)V"

    public FunctionHandle(long funcPtr, MethodDescriptor methodDescriptor) {
        this.funcPtr = funcPtr;
        this.methodDescriptor = methodDescriptor;
    }

    // こいつが JNI 経由でネイティブに丸投げ
    private native Object invokeNative(long funcPtr, String methodDescriptor, Object[] args);

    public Object invoke(Object... args) {
        if (args == null) args = new Object[0];
        int expectedArgs = this.methodDescriptor.getParameterTypes().length;
        if (args.length != expectedArgs) {
            throw new VMPanic("Expected " + expectedArgs + " arguments, but got " + args.length);
        }

        return invokeNative(funcPtr, methodDescriptor.toString(), args);
    }
}
