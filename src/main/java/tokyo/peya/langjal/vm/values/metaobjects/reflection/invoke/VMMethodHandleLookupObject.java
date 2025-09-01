package tokyo.peya.langjal.vm.values.metaobjects.reflection.invoke;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.metaobjects.VMClassObject;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

@Getter
public class VMMethodHandleLookupObject extends VMObject
{
    private final VMClassObject lookupClass;
    private final VMClassObject previousLookupClass;
    private final VMInteger allowedModes;

    public VMMethodHandleLookupObject(@NotNull JalVM vm, @NotNull VMClassObject lookupClass,
                                      @Nullable VMClassObject previousLookupClass, int allowedModes)
    {
        super(vm.getClassLoader().findClass(ClassReference.of("java/lang/invoke/MethodHandles$Lookup")));
        this.lookupClass = lookupClass;
        this.previousLookupClass = previousLookupClass;
        this.allowedModes = new VMInteger(vm, allowedModes);

        this.setField("lookupClass", lookupClass);
        if (previousLookupClass != null)
            this.setField("prevLookupClass", previousLookupClass);
        this.setField("allowedModes", this.allowedModes);

        this.forceInitialise(vm.getClassLoader());
    }

    public VMMethodHandleLookupObject(@NotNull JalVM vm, @NotNull VMClassObject lookupClass,
                                      @Nullable VMClassObject previousLookupClass, @NotNull VMInteger lookupMode)
    {
        this(vm, lookupClass, previousLookupClass, lookupMode.asNumber().intValue());
    }

    public static VMMethodHandleLookupObject createLookupChain(@NotNull VMFrame frame, @NotNull VMClass clazz)
    {
        List<VMClassObject> lookupChain = findMethodChain(frame, clazz);
        VMMethodHandleLookupObject lookup = createChainedLookup(
                frame,
                lookupChain
        );

        if (lookup == null)
            throw new VMPanic("Failed to create MethodHandle lookup chain");

        return lookup;
    }

    private static @NotNull List<VMClassObject> findMethodChain(@NotNull VMFrame frame, @NotNull VMClass clazz)
    {
        List<VMClassObject> lookupChain = new ArrayList<>();

        VMFrame currentFrame = frame;
        VMClass currentClass = clazz;
        while (currentClass != null)
        {
            lookupChain.add(currentClass.getClassObject());
            VMFrame prev = currentFrame.getPrevFrame();
            if (prev == null)
                break;
            else
            {
                currentFrame = prev;
                currentClass = currentFrame.getMethod().getClazz();
            }
        }
        return lookupChain;
    }

    private static @Nullable VMMethodHandleLookupObject createChainedLookup(@NotNull VMFrame frame,
                                                                            @NotNull List<? extends VMClassObject> lookupChain)
    {
        VMMethodHandleLookupObject lookup = null;
        for (int i = lookupChain.size() - 1; i >= 0; i--)
        {
            VMClassObject currentLookupClass = lookupChain.get(i);
            VMClassObject previousLookupClass = (i > 0) ? lookupChain.get(i - 1) : null;

            if (lookup == null)
                lookup = new VMMethodHandleLookupObject(
                        frame.getVM(),
                        currentLookupClass,
                        previousLookupClass,
                        MethodHandles.Lookup.PUBLIC | MethodHandles.Lookup.PROTECTED | MethodHandles.Lookup.PACKAGE | MethodHandles.Lookup.PRIVATE
                );
            else
                lookup = new VMMethodHandleLookupObject(
                        frame.getVM(),
                        currentLookupClass,
                        previousLookupClass,
                        lookup.getAllowedModes()
                );
        }
        return lookup;
    }
}
