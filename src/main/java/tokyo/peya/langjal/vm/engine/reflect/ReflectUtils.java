package tokyo.peya.langjal.vm.engine.reflect;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.values.metaobjects.VMClassObject;
import tokyo.peya.langjal.vm.values.metaobjects.VMMethodHandleLookupObject;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ReflectUtils
{
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
            lookupChain.add(clazz.getClassObject(frame.getVm().getClassLoader()));
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
                        frame.getVm(),
                        currentLookupClass,
                        previousLookupClass,
                        MethodHandles.Lookup.PUBLIC | MethodHandles.Lookup.PROTECTED | MethodHandles.Lookup.PACKAGE | MethodHandles.Lookup.PRIVATE
                );
            else
                lookup = new VMMethodHandleLookupObject(
                        frame.getVm(),
                        currentLookupClass,
                        previousLookupClass,
                        lookup.getAllowedModes()
                );
        }
        return lookup;
    }
}
