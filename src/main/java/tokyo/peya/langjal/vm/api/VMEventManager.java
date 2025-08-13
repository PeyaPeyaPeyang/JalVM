package tokyo.peya.langjal.vm.api;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class VMEventManager {
    private final List<VMListener> listeners;

    public VMEventManager() {
        this.listeners = new ArrayList<>();
    }

    public void dispatchEvent(@NotNull VMEvent event) {
        VMEventHandlerList<?> eventHandlerList;
        try {
            eventHandlerList = getHandlerListForEvent(event.getClass());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException("Failed to get handler list for event: " + event.getClass().getName(), e);
        }

        eventHandlerList.callEvent(event);
    }

    public void registerListener(@NotNull VMListener listener) {
        if (!this.listeners.contains(listener)) {
            this.listeners.add(listener);
            this.bakeListener(listener);
        }
    }

    private void bakeListener(@NotNull VMListener listener) {
        // private static final VMEventHandlerList<VMevent> HANDLER_LIST = new VMEventHandlerList<>(VMevent.class);

        try {
            for (Method method: listener.getClass().getDeclaredMethods()) {
                if (method.getAnnotationsByType(VMEventHandler.class).length == 0)
                    continue; // skip methods without VMEventHandler annotation

                VMEventHandlerList<?> eventHandlerList = getHandlerListForEvents(listener, method);

                method.setAccessible(true);
                eventHandlerList.registerHandler(
                        event -> {
                            try {
                                method.invoke(listener, event);
                            } catch (Throwable e) {
                                throw new IllegalArgumentException(
                                        "Failed to invoke method " + method.getName() + " in listener " + listener.getClass().getName() + " with event " + event.getClass().getName(),
                                        e
                                );
                            }
                        }
                );
            }
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException("Failed to bake listener: " + listener.getClass().getName(), e);
        }
    }

    private static @NotNull VMEventHandlerList<?> getHandlerListForEvents(@NotNull VMListener listener, Method method) throws NoSuchFieldException, IllegalAccessException {
        if (method.getParameterCount() != 1)
            throw new IllegalArgumentException("Method listener " + listener.getClass().getName() + " must have exactly one parameter.");

        Class<?> eventType = method.getParameterTypes()[0];
        VMEventHandlerList<?> eventHandlerList = getHandlerListForEvent(eventType);

        if (!eventHandlerList.getEventType().isAssignableFrom(eventType))
            throw new IllegalArgumentException("Field HANDLER_LIST in listener " + listener.getClass().getName() + " must be of type VMEventHandlerList<" + eventType.getName() + ">.");
        return eventHandlerList;
    }

    private static @NotNull VMEventHandlerList<?> getHandlerListForEvent(Class<?> eventType) throws NoSuchFieldException, IllegalAccessException {
        if (!VMEvent.class.isAssignableFrom(eventType))
            throw new IllegalArgumentException("Event type " + eventType.getName() + " must be a subclass of VMEvent.");

        Field handlerList = eventType.getDeclaredField("HANDLER_LIST");
        if (!handlerList.getType().equals(VMEventHandlerList.class))
            throw new IllegalArgumentException("Field HANDLER_LIST in listener " + eventType.getName() + " must be of type VMEventHandlerList.");
        handlerList.setAccessible(true);
        VMEventHandlerList<?> eventHandlerList = (VMEventHandlerList<?>) handlerList.get(eventType);
        if (eventHandlerList == null)
            throw new IllegalArgumentException("Field HANDLER_LIST in listener " + eventType.getName() + " must not be null.");
        return eventHandlerList;
    }
}
