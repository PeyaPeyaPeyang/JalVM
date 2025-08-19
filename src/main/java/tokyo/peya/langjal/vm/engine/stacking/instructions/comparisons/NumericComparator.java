package tokyo.peya.langjal.vm.engine.stacking.instructions.comparisons;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.values.VMDouble;
import tokyo.peya.langjal.vm.values.VMFloat;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMType;

public class NumericComparator
{
    static void compareLong(@NotNull VMFrame frame)
    {
        VMLong value2 = frame.getStack().popType(VMType.LONG);
        VMLong value1 = frame.getStack().popType(VMType.LONG);

        long longValue1 = value1.asNumber().longValue();
        long longValue2 = value2.asNumber().longValue();
        VMInteger result;
        if (longValue1 > longValue2)
            result = VMInteger.ONE;
        else if (longValue1 < longValue2)
            result = VMInteger.M1;
        else
            result = VMInteger.ZERO;
        frame.getStack().push(result);
    }

    static void compareFloat(@NotNull VMFrame frame, @NotNull VMInteger onNaN)
    {
        VMFloat value2 = frame.getStack().popType(VMType.FLOAT);
        VMFloat value1 = frame.getStack().popType(VMType.FLOAT);
        if (value1.isNaN() || value2.isNaN())
            frame.getStack().push(onNaN);

        float floatValue1 = value1.asNumber().floatValue();
        float floatValue2 = value2.asNumber().floatValue();
        VMInteger result;
        if (floatValue1 > floatValue2)
            result = VMInteger.ONE;
        else if (floatValue1 < floatValue2)
            result = VMInteger.M1;
        else
            result = VMInteger.ZERO;
        frame.getStack().push(result);
    }

    static void compareDouble(@NotNull VMFrame frame, @NotNull VMInteger onNaN)
    {
        VMDouble value2 = frame.getStack().popType(VMType.DOUBLE);
        VMDouble value1 = frame.getStack().popType(VMType.DOUBLE);
        if (value1.isNaN() || value2.isNaN())
            frame.getStack().push(onNaN);

        double doubleValue1 = value1.asNumber().doubleValue();
        double doubleValue2 = value2.asNumber().doubleValue();
        VMInteger result;
        if (doubleValue1 > doubleValue2)
            result = VMInteger.ONE;
        else if (doubleValue1 < doubleValue2)
            result = VMInteger.M1;
        else
            result = VMInteger.ZERO;
        frame.getStack().push(result);
    }
}
