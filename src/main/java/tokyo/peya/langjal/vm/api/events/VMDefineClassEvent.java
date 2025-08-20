package tokyo.peya.langjal.vm.api.events;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.VMEvent;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;
import tokyo.peya.langjal.vm.references.ClassReference;

@Getter
@Setter
public class VMDefineClassEvent extends JalVMEvent
{
    public static final VMEventHandlerList<VMDefineClassEvent> HANDLER_LIST = new VMEventHandlerList<>(VMDefineClassEvent.class);

    private final ClassReference reference;
    private ClassNode node;

    public VMDefineClassEvent(@NotNull JalVM vm, @NotNull ClassReference reference, @NotNull ClassNode node)
    {
        super(vm);
        this.reference = reference;
        this.node = node;
    }
}
