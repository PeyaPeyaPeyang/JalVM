package tokyo.peya.langjal.vm.values.metaobjects;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.reflection.VMMethodObject;

@Getter
public class VMStackTraceElementObject extends VMObject
{
    private final JalVM vm;
    private final String classLoaderName;
    private final String moduleName;
    private final String moduleVersion;
    private final String declaringClass;
    private final String methodName;
    private final String fileName;
    private final int lineNumber;

    public VMStackTraceElementObject(@NotNull JalVM vm,
                                     @NotNull VMMethod method,
                                     int lineNumber)
    {
        super(vm.getClassLoader().findClass(ClassReference.of("java/lang/StackTraceElement")));
        this.vm = vm;
        this.classLoaderName = null;
        VMClass clazz = method.getClazz();
        this.moduleName = clazz.getClazz().module.name;
        this.moduleVersion = clazz.getClazz().module.version;
        this.declaringClass = clazz.getReference().getFullQualifiedDotName();
        this.methodName = method.getName();
        this.fileName = clazz.getClazz().sourceFile;
        this.lineNumber = lineNumber;

        this.setField("classLoaderName", VMStringObject.createString(vm, this.classLoaderName));
        this.setField("moduleName", VMStringObject.createString(vm, this.moduleName));
        this.setField("moduleVersion", VMStringObject.createString(vm, this.moduleVersion));
        this.setField("declaringClass", VMStringObject.createString(vm, this.declaringClass));
        this.setField("methodName", VMStringObject.createString(vm, this.methodName));
        this.setField("fileName", VMStringObject.createString(vm, this.fileName));
        this.setField("lineNumber", new VMInteger(vm, this.lineNumber));

        this.forceInitialise(vm.getClassLoader());
    }
}
