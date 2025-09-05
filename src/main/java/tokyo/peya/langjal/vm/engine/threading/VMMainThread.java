package tokyo.peya.langjal.vm.engine.threading;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.metaobjects.VMStringObject;

public final class VMMainThread extends VMThread
{
    public VMMainThread(@NotNull JalVM vm, @NotNull VMThreadGroup group)
    {
        super(vm, group, "main");
    }

    public void invokeVMInitialisation(@NotNull VMMethod method)
    {
        this.group.setMainThread(this);
        this.firstFrame = this.createFrame(
                method,
                true
        );
        this.currentFrame = this.firstFrame;

        this.firstFrame.activate();

        // AccessibleObject の <clinit> を実行して， SharedSecrets の javaLangReflectAccess を設定する。
        // ２回実行するのは，最初に親を，次に子を初期化するため。
        VMClass accessibleObjectClass = this.vm.getClassLoader().findClass(ClassReference.of("java/lang/reflect/AccessibleObject"));
        accessibleObjectClass.initialise(this);
        accessibleObjectClass.initialise(this);
    }

    public void startMainThread(@NotNull VMMethod entryPointMethod, @NotNull String[] args)
    {
        this.setState(VMThreadState.NEW);
        this.group.setMainThread(this);
        this.firstFrame = this.createFrame(
                entryPointMethod,
                false,
                VMStringObject.createStringArray(this, args)
        );
        this.currentFrame = this.firstFrame;

        this.firstFrame.activate();
    }
}
