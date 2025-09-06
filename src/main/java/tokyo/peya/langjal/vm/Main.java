package tokyo.peya.langjal.vm;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import tokyo.peya.langjal.vm.references.ClassReference;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class Main
{
    public static void main(String[] args)
    {
        // Main の位置を探す
        int mainIndex = -1;
        for (int i = 0; i < args.length; i++)
        {
            if (!args[i].startsWith("-"))
            { // 単純に最初の非オプションを Main 扱い
                mainIndex = i;
                break;
            }
        }

        if (mainIndex == -1)
        {
            System.err.println("No main class specified");
            System.exit(1);
        }

        // 前半（ランチャー用オプション）だけ取り出す
        String[] launcherArgs = Arrays.copyOfRange(args, 0, mainIndex);
        String mainClass = args[mainIndex];
        String[] mainArgs = Arrays.copyOfRange(args, mainIndex + 1, args.length);

        // ランチャーオプション解析
        OptionParser parser = new OptionParser();
        parser.accepts("cp").withOptionalArg().describedAs("Path to the .jar, .jmod, or directory to load classes from");
        parser.accepts("ea").withOptionalArg().describedAs("Enable assertions");
        parser.allowsUnrecognizedOptions();

        OptionSet opts = parser.parse(launcherArgs);

        String classPath = opts.valueOf("cp") == null ? System.getProperty("java.class.path"): opts.valueOf("cp")
                                                                                                   .toString();

        VMConfiguration.VMConfigurationBuilder builder = VMConfiguration.builder();
        Arrays.stream(classPath.split(":"))
                                      .map(Path::of)
                                      .forEach(builder::classPath);

        if (opts.has("ea"))
            builder.enableAssertions(true);

        JalVM vm = new JalVM(builder.build());
        vm.executeMain(
                ClassReference.of(mainClass),
                mainArgs
        );
    }
}
