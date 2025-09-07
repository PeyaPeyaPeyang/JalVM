package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMThreadObject;

public class InjectorThread implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("java/lang/Thread");

    @Override
    public @NotNull ClassReference suitableClass()
    {
        return CLAZZ;
    }

    @Override
    public void inject(@NotNull VMSystemClassLoader cl, @NotNull VMClass clazz)
    {
        final String FIELD_NEXT_THREAD_ID = "$nextThreadID";

        clazz.injectInstanceCreator((o) -> new VMThreadObject(cl, o));

        clazz.injectField(
                new InjectedField(
                        clazz,
                        VMType.of(clazz, PrimitiveTypes.LONG),
                        new FieldNode(
                                EOpcodes.ACC_PRIVATE | EOpcodes.ACC_STATIC,
                                FIELD_NEXT_THREAD_ID,
                                "J",
                                null,
                                null
                        )
                ) {
                    private long nextID = 4;  // 3 は main スレッドであるから，その次の ID から始める

                    @Override
                    public VMValue get(@NotNull VMClass caller, @Nullable VMObject instance)
                    {
                        return new VMLong(clazz.getVM(), this.nextID);
                    }

                    @Override
                    public void set(@NotNull VMClass caller, @Nullable VMObject instance, @NotNull VMValue value)
                    {
                        if (value instanceof VMLong l)
                            this.nextID = l.asNumber().longValue();
                    }
                }
        );

        clazz.injectMethod(
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
                    @Nullable
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        return null;
                    }
                }
        );
        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "currentThread",
                        "()Ljava/lang/Thread;",
                        null,
                        null
                )
                )
                {
                    @Override
                    @Nullable
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        return frame.getThread().getThreadObject();
                    }
                }
        );
        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "yield0",
                        "()V",
                        null,
                        null
                )
                )
                {
                    @Override
                    @Nullable
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        return null;
                    }
                }
        );
        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "getNextThreadIdOffset",
                        "()J",
                        null,
                        null
                )
                )
                {
                    @Override
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMField field = clazz.findField(FIELD_NEXT_THREAD_ID);
                        return new VMLong(frame, field.getFieldID());
                    }
                }
        );
        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE  | EOpcodes.ACC_NATIVE,
                        "setPriority0",
                        "(I)V",
                        null,
                        null
                )
                )
                {
                    @Override
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        int newPriority = ((VMInteger) args[0]).asNumber().intValue();
                        if (newPriority < 1 || newPriority > 10)
                            throw new VMPanic("Thread priority out of range: " + newPriority);

                        assert instance != null;
                        VMThreadObject to = instance.findSuper(VMThreadObject.class);
                        to.setInsideVMPriority(newPriority);
                        return null;
                    }
                }
        );
        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE  | EOpcodes.ACC_NATIVE,
                        "start0",
                        "()V",
                        null,
                        null
                )
                )
                {
                    @Override
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        assert instance != null;
                        VMThreadObject to = instance.findSuper(VMThreadObject.class);
                        to.startNewThreadByVM();
                        return null;
                    }
                }
        );
    }

}
