package tokyo.peya.langjal.vm;

import lombok.Getter;
import tokyo.peya.langjal.vm.engine.VMEngine;

@Getter
public class JalVM {
    private final VMHeap heap;
    private final VMClassLoader classLoader;
    private final VMEngine engine;

    public JalVM() {
        System.out.println("Initialising J(al)VM...");
        this.heap = new VMHeap();
        this.classLoader = new VMClassLoader(this.heap);
        this.engine = new VMEngine(this);
    }

}
