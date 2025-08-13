package tokyo.peya.langjal.vm.low;

import tokyo.peya.langjal.compiler.jvm.MethodDescriptor;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

public final class NativeLibrary {
    static {
        System.loadLibrary("nativebridge");
    }

    private long handle; // ネイティブ側のポインタを格納（native long）

    private NativeLibrary(long handle) {
        this.handle = handle;
    }

    public static NativeLibrary open(String path) {
        long h = openNative(path);
        if (h == 0L)
            throw new VMPanic("cannot open: " + path);
        return new NativeLibrary(h);
    }

    private static native long openNative(String path);

    private static native long lookupNative(long libHandle, String symbol);

    private static native void closeNative(long libHandle);

    public FunctionHandle lookup(String symbol, MethodDescriptor desc) {
        long ptr = lookupNative(handle, symbol);
        if (ptr == 0L)
            throw new VMPanic("symbol not found: " + symbol);

        return new FunctionHandle(ptr, desc);
    }

    public void close() {
        closeNative(handle);
        handle = 0L;
    }
}
