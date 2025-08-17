package tokyo.peya.langjal.vm.api;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Getter
public class VMEventHandlerList<T extends VMEvent>
{
    private final Class<T> eventType;
    private final List<Consumer<T>> handlers;

    public VMEventHandlerList(@NotNull Class<T> eventType)
    {
        this.eventType = eventType;
        this.handlers = new ArrayList<>();
    }

    public void registerHandler(@NotNull Consumer<T> handler)
    {
        this.handlers.add(handler);
    }

    @SuppressWarnings("unchecked")
    public void callEvent(@NotNull VMEvent event)
    {
        if (!this.eventType.isInstance(event))
            throw new IllegalArgumentException("Event type mismatch: expected " + this.eventType.getName() + " but got " + event.getClass()
                                                                                                                                .getName());

        for (Consumer<T> consumer : this.handlers)
            consumer.accept((T) event);
    }

    public List<Consumer<T>> getHandlers()
    {
        return Collections.unmodifiableList(this.handlers);
    }
}
