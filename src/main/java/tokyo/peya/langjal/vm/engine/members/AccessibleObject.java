package tokyo.peya.langjal.vm.engine.members;

import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.compiler.jvm.AccessAttributeSet;
import tokyo.peya.langjal.compiler.jvm.AccessLevel;
import tokyo.peya.langjal.vm.engine.VMClass;

public interface AccessibleObject
{

    VMClass getOwningClass();  // これがクラスなら自分自身

    AccessLevel getAccessLevel();

    AccessAttributeSet getAccessAttributes();

    default boolean canAccessFrom(@Nullable VMClass callerClass)
    {
        VMClass targetClass = this.getOwningClass();
        AccessLevel targetAccessLevel = this.getAccessLevel();

        if (callerClass == null)
            return targetAccessLevel == AccessLevel.PUBLIC; // 呼び出し元が null = VM ならpublicのみアクセス可能

        switch (targetAccessLevel)
        {
            case PUBLIC:
                return true; // publicはどこからでもアクセス可能

            case PROTECTED:
                if (callerClass.isSubclassOf(targetClass)
                    || targetClass.isSubclassOf(callerClass))
                    return true;
                /* fall-through */
            case PACKAGE_PRIVATE:
                if (callerClass.getReference().isEqualPackage(targetClass.getReference()))
                    return true;

                /* fall-through */
            case PRIVATE:
                if (callerClass.equals(targetClass))
                    return true; // privateは同じクラスからのみアクセス可能

                for (VMClass inner: targetClass.getInnerLinks())
                {
                    if (inner.getReference().equals(callerClass.getReference()))
                        return true; // privateなメンバクラスは外側のクラスからアクセス可能
                }
                for (VMClass inner: callerClass.getInnerLinks())
                {
                    if (inner.getReference().equals(targetClass.getReference()))
                        return true; // privateなメンバクラスは外側のクラスからアクセス可能
                }

                /* fall-through */
            default:
                return false; // その他のアクセスレベルはアクセス不可
        }
    }
}
