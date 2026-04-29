package tokyo.peya.langjal.vm.api.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import tokyo.peya.langjal.vm.api.VMEvent;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.references.ClassReference;

@Getter
public sealed class VMClassDefineEvent extends VMEvent permits VMClassDefineEvent.Pre, VMClassDefineEvent.Post
{
    private final ClassReference reference;

    protected VMClassDefineEvent(ClassReference reference)
    {
        this.reference = reference;
    }

    public static final class Pre extends VMClassDefineEvent
    {
        public static final VMEventHandlerList<VMClassDefineEvent> HANDLER_LIST = new VMEventHandlerList<>(VMClassDefineEvent.class);

        public Pre(ClassReference reference)
        {
            super(reference);
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static final class Post extends VMClassDefineEvent
    {
        public static final VMEventHandlerList<VMClassDefineEvent> HANDLER_LIST = new VMEventHandlerList<>(VMClassDefineEvent.class);

        private VMClass clazz;
        public Post(ClassReference reference, VMClass clazz)
        {
            super(reference);
            this.clazz = clazz;
        }
    }
}
