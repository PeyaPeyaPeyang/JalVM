package tokyo.peya.langjal.vm.api;

import tokyo.peya.langjal.vm.JalVM;

public interface VMPlugin
{
    void onLoad(JalVM vm);
}
