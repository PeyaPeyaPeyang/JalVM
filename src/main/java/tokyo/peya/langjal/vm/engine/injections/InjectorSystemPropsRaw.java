package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.threads.VMThread;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMStringCreator;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

import java.util.Locale;
import java.util.Properties;
import java.util.Set;

public class InjectorSystemPropsRaw implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("jdk/internal/util/SystemProps$Raw");

    @Override
    public ClassReference suitableClass()
    {
        return CLAZZ;
    }
    public static String[] createPlatformProperties() {
        Locale locale = Locale.getDefault();
        Properties sys = System.getProperties();

        String[] props = new String[40];

        // ===== locale系 =====
        props[0]  = locale.getDisplayCountry();  // _display_country_NDX
        props[1]  = locale.getDisplayLanguage(); // _display_language_NDX
        props[2]  = locale.getDisplayScript();   // _display_script_NDX
        props[3]  = locale.getDisplayVariant();  // _display_variant_NDX

        // ===== file / path / line separator =====
        props[4]  = sys.getProperty("file.encoding");
        props[5]  = sys.getProperty("file.separator");
        props[6]  = locale.getCountry();        // _format_country_NDX
        props[7]  = locale.getLanguage();       // _format_language_NDX
        props[8]  = locale.getScript();         // _format_script_NDX
        props[9]  = locale.getVariant();        // _format_variant_NDX

        // ===== FTP/HTTP proxy =====
        props[10] = sys.getProperty("ftp.nonProxyHosts");
        props[11] = sys.getProperty("ftp.proxyHost");
        props[12] = sys.getProperty("ftp.proxyPort");
        props[13] = sys.getProperty("http.nonProxyHosts");
        props[14] = sys.getProperty("http.proxyHost");
        props[15] = sys.getProperty("http.proxyPort");
        props[16] = sys.getProperty("https.proxyHost");
        props[17] = sys.getProperty("https.proxyPort");

        // ===== tmpdir / line separator =====
        props[18] = sys.getProperty("java.io.tmpdir");
        props[19] = sys.getProperty("line.separator");

        // ===== OS =====
        props[20] = sys.getProperty("os.arch");
        props[21] = sys.getProperty("os.name");
        props[22] = sys.getProperty("os.version");
        props[23] = sys.getProperty("path.separator");

        // ===== SOCKS proxy =====
        props[24] = sys.getProperty("socksNonProxyHosts");
        props[25] = sys.getProperty("socksProxyHost");
        props[26] = sys.getProperty("socksProxyPort");

        // ===== stdout / stderr encoding =====
        props[27] = sys.getProperty("sun.stderr.encoding", sys.getProperty("file.encoding"));
        props[28] = sys.getProperty("sun.stdout.encoding", sys.getProperty("file.encoding"));

        // ===== sun / arch 系 =====
        props[29] = sys.getProperty("sun.arch.abi");
        props[30] = sys.getProperty("sun.arch.data.model");
        props[31] = sys.getProperty("sun.cpu.endian");
        props[32] = sys.getProperty("sun.cpu.isalist");
        props[33] = sys.getProperty("sun.io.unicode.encoding");
        props[34] = sys.getProperty("sun.jnu.encoding");
        props[35] = sys.getProperty("sun.os.patch.level");

        // ===== user =====
        props[36] = sys.getProperty("user.dir");
        props[37] = sys.getProperty("user.home");
        props[38] = sys.getProperty("user.name");

        // props[39] は Raw.FIXED_LENGTH = 1 + _user_name_NDX の余白
        props[39] = null;

        return props;
    }

    public static String[] createVMProperties() {
        Properties sys = System.getProperties();
        Set<String> keys = sys.stringPropertyNames();

        // 2倍サイズ + 1 (終端記号 null)
        String[] props = new String[keys.size() * 2 + 1];
        int i = 0;
        for (String key : keys) {
            props[i++] = key;
            props[i++] = sys.getProperty(key);
        }
        // 配列の末尾は null
        props[i] = null;

        return props;
    }

    @Override
    public void inject(@NotNull VMSystemClassLoader cl, @NotNull VMClass clazz)
    {
        String[] platformProperties = createPlatformProperties();
        String[] vmProperties = createVMProperties();

        VMValue[] platformProps = VMStringCreator.createStringArray(platformProperties);
        VMValue[] vmProps = VMStringCreator.createStringArray(vmProperties);

        VMArray platformPropsArray = new VMArray(VMType.STRING, platformProps);
        VMArray vmPropsArray = new VMArray(VMType.STRING, vmProps);

        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "platformProperties",
                        "()[Ljava/lang/String;",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        return platformPropsArray;
                    }
                }
        );

        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "vmProperties",
                        "()[Ljava/lang/String;",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        return vmPropsArray;
                    }
                }
        );
    }

}
