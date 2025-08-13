package tokyo.peya.langjal.vm.engine.members;

import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.compiler.jvm.AccessAttributeSet;
import tokyo.peya.langjal.compiler.jvm.AccessLevel;
import tokyo.peya.langjal.vm.engine.VMClass;

public interface RestrictedAccessor {

    VMClass getOwningClass();  // これがクラスなら自分自身
    AccessLevel getAccessLevel();
    AccessAttributeSet getAccessAttributes();

    default boolean canAccessFrom(@Nullable VMClass callerClass) {
        VMClass target = this.getOwningClass();
        AccessLevel targetAccessLevel = this.getAccessLevel();

        if (callerClass == null)
            return targetAccessLevel == AccessLevel.PUBLIC; // 呼び出し元が null = VM ならpublicのみアクセス可能

        return switch (targetAccessLevel) {
            case PUBLIC -> true; // publicはどこからでもアクセス可能
            case PROTECTED -> callerClass.isSubclassOf(target) || callerClass.getReference().isEqualPackage(target.getReference());
            case PACKAGE_PRIVATE -> callerClass.getReference().isEqualPackage(target.getReference()) ||
                    callerClass.getReference().isEqualPackage(target.getSuperLink().getReference()); // 同じパッケージ内，またはスーパークラスが同じパッケージ内ならアクセス可能
            case PRIVATE -> false; // privateは同じクラス内のみアクセス可能
        };
    }
}
