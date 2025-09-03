package tokyo.peya.langjal.vm.api.events;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMType;

@Getter
@Setter
public class VMLinkClassEvent extends JalVMEvent
{
    public static final VMEventHandlerList<VMLinkClassEvent> HANDLER_LIST = new VMEventHandlerList<>(VMLinkClassEvent.class);

    private final VMType<?> linking;

    public VMLinkClassEvent(@NotNull JalVM vm, @NotNull VMType<?> linking)
    {
        super(vm);
        this.linking = linking;
    }
}
