package tokyo.peya.langjal.vm.engine;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMSystemClassLoader;

public interface VMComponent
{
    @NotNull
    JalVM getVM();

    default @NotNull VMSystemClassLoader getClassLoader()
    {
        return this.getVM().getClassLoader();
    }
}
