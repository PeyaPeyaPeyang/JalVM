package tokyo.peya.langjal.vm.values.metaobjects;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMObject;

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
        this.allowedModes = new VMInteger(allowedModes);

        this.setField("lookupClass", lookupClass);
        if (previousLookupClass != null)
            this.setField("prevLookupClass", previousLookupClass);
        this.setField("allowedModes", this.allowedModes);

        this.forceInitialise();
    }

    public VMMethodHandleLookupObject(@NotNull JalVM vm, @NotNull VMClassObject lookupClass,
                                      @Nullable VMClassObject previousLookupClass, @NotNull VMInteger lookupMode)
    {
        this(vm, lookupClass, previousLookupClass, lookupMode.asNumber().intValue());
    }

}
