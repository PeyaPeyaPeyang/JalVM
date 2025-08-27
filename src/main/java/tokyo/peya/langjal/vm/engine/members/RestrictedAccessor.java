package tokyo.peya.langjal.vm.engine.members;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;
import tokyo.peya.langjal.compiler.jvm.AccessAttributeSet;
import tokyo.peya.langjal.compiler.jvm.AccessLevel;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.references.ClassReference;

import java.util.List;

public interface RestrictedAccessor
{

    VMClass getOwningClass();  // これがクラスなら自分自身

    AccessLevel getAccessLevel();

    AccessAttributeSet getAccessAttributes();

    default boolean canAccessFrom(@Nullable VMClass callerClass)
    {
        VMClass target = this.getOwningClass();
        AccessLevel targetAccessLevel = this.getAccessLevel();

        if (callerClass == null)
            return targetAccessLevel == AccessLevel.PUBLIC; // 呼び出し元が null = VM ならpublicのみアクセス可能

        switch (targetAccessLevel)
        {
            case PUBLIC:
                return true; // publicはどこからでもアクセス可能

            case PROTECTED:
                if (callerClass.isSubclassOf(target)
                        || callerClass.getReference().isEqualPackage(target.getReference()))
                    return true;
                /* fall-through */
            case PACKAGE_PRIVATE:
                if (callerClass.getReference().isEqualPackage(target.getReference())
                        || callerClass.getReference().isEqualPackage(target.getSuperLink().getReference()))
                    return true;
                /* fall-through */
            case PRIVATE:
                if (callerClass.equals(target))
                    return true; // privateは同じクラスからのみアクセス可能

                for (VMClass inner: target.getInnerLinks())
                {
                    if (inner.getReference().equals(callerClass.getReference()))
                        return true; // privateなメンバクラスは外側のクラスからアクセス可能
                }
                for (VMClass inner: callerClass.getInnerLinks())
                {
                    if (inner.getReference().equals(target.getReference()))
                        return true; // privateなメンバクラスは外側のクラスからアクセス可能
                }

                /* fall-through */
            default:
                return false; // その他のアクセスレベルはアクセス不可
        }
    }
}
