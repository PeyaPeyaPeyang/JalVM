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

                // インナー・クラスの場合，呼び出し元と対象が内外関係ならアクセス可能
                String targetName = target.getReference().getFullQualifiedName();
                String callerName = callerClass.getReference().getFullQualifiedName();

                // 呼び出し元の innerClasses を確認
                for (InnerClassNode inner : callerClass.getClazz().innerClasses)
                {
                    if (inner.outerName == null)
                        inner.outerName = inner.name.substring(0, inner.name.lastIndexOf('$'));

                    if ((callerName.equals(inner.name) && targetName.equals(inner.outerName))
                            || callerName.equals(inner.outerName) && targetName.equals(inner.name))
                        return true;
                }

                // 呼び出し先（target）の innerClasses も確認（逆の関係用）
                for (InnerClassNode inner : target.getClazz().innerClasses)
                {
                    if (inner.outerName == null)
                        inner.outerName = inner.name.substring(0, inner.name.lastIndexOf('$'));

                    if ((targetName.equals(inner.name) && callerName.equals(inner.outerName))
                            || (targetName.equals(inner.outerName) && callerName.equals(inner.name)))
                        return true;
                }

                /* fall-through */
            default:
                return false; // その他のアクセスレベルはアクセス不可
        }
    }
}
