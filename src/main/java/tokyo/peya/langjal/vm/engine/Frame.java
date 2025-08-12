package tokyo.peya.langjal.vm.engine;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.JalVM;

@Getter
public class Frame {
    private final VMMethod method;

    private Frame prevFrame;
    private Frame nextFrame;

    private final int frameIndex;

    public Frame(
            @NotNull JalVM vm,
            @NotNull VMEngine engine,
            @Nullable Frame prevFrame,
            @NotNull VMMethod method,
            int frameIdx) {
        this.prevFrame = prevFrame;
        this.method = method;
        this.frameIndex = frameIdx;
    }

    /* non-public */ void setNextFrame(@NotNull Frame nextFrame) {
        this.nextFrame = nextFrame;
    }

    /* non-public */ void setPrevFrame(@NotNull Frame prevFrame) {
        this.prevFrame = prevFrame;
    }
}
