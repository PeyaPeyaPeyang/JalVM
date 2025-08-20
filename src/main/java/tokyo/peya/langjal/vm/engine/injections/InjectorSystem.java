package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.MethodDescriptor;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMValue;

public class InjectorSystem implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("java/lang/System");

    @Override
    public ClassReference suitableClass()
    {
        return CLAZZ;
    }

    @Override
    public void inject(@NotNull VMSystemClassLoader cl, @NotNull VMClass clazz)
    {/*
        clazz.injectField(
                cl,
                new InjectedField(
                        clazz, VMType.ofClassName("java/io/PrintStream"),
                        new FieldNode(
                                EOpcodes.ACC_PUBLIC | EOpcodes.ACC_STATIC,
                                "out",
                                "Ljava/io/PrintStream;",
                                null,
                                null
                        )
                )
                {

                    @Override
                    public VMValue get(@NotNull VMClass caller, @Nullable VMObject instance)
                    {
                        return cl.findClass(CLAZZ).createInstance();
                    }

                    @Override
                    public void set(@NotNull VMClass caller, @Nullable VMObject instance, @NotNull VMValue value)
                    {

                    }
                }
        );*/
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "registerNatives",
                        "()V",
                        null,
                        null
                )
                )
                {
                    @Override
                    @Nullable VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        assert caller != null : "Caller class cannot be null";

                        VMClass systemClass = caller.getLinkedClass();
                        VMMethod method = systemClass.findMethod("initPhase1", MethodDescriptor.parse("()V"));
                        if (method == null)
                            throw new VMPanic("System class does not have initPhase1 method");
                        VMFrame f = thread.createFrame(
                                method,
                                true
                        );
                        f.activate();

                        return null;
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_STATIC,
                        "currentTimeMillis",
                        "()J",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        long currentTime = System.currentTimeMillis();
                        return new VMLong(currentTime);
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_STATIC,
                        "nanoTime",
                        "()J",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        long currentTime = System.nanoTime();
                        return new VMLong(currentTime);
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_STATIC,
                        "arraycopy",
                        "(Ljava/lang/Object;ILjava/lang/Object;II)V",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        if (args.length != 5)
                            throw new IllegalArgumentException("arraycopy requires 5 arguments, got: " + args.length);

                        VMArray src = (VMArray) args[0];
                        int srcPos = ((VMInteger) args[1]).asNumber().intValue();
                        VMArray dest = (VMArray) args[2];
                        int destPos = ((VMInteger) args[3]).asNumber().intValue();
                        int length = ((VMInteger) args[4]).asNumber().intValue();
                        if (srcPos < 0 || destPos < 0 || length < 0)
                            throw new IndexOutOfBoundsException("Negative index in arraycopy");

                        if (srcPos + length > src.length() || destPos + length > dest.length())
                            throw new IndexOutOfBoundsException("Array copy exceeds bounds");

                        for (int i = 0; i < length; i++)
                        {
                            VMValue value = src.get(srcPos + i);
                            dest.set(destPos + i, value);
                        }

                        return null;
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "setIn0",
                        "(Ljava/io/InputStream;)V",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMObject inputStream = (VMObject) args[0];
                        clazz.setStaticField("in", inputStream);
                        return null;
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "setOut0",
                        "(Ljava/io/PrintStream;)V",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMObject printStream = (VMObject) args[0];
                        clazz.setStaticField("out", printStream);
                        return null;
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "setErr0",
                        "(Ljava/io/PrintStream;)V",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMObject printStream = (VMObject) args[0];
                        clazz.setStaticField("err", printStream);
                        return null;
                    }
                }
        );
    }

}
