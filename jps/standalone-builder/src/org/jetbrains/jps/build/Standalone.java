package org.jetbrains.jps.build;

import com.intellij.util.ArrayUtil;
import com.intellij.util.ParameterizedRunnable;
import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import org.jetbrains.jps.api.BuildType;
import org.jetbrains.jps.api.CanceledStatus;
import org.jetbrains.jps.cmdline.BuildRunner;
import org.jetbrains.jps.cmdline.JpsModelLoader;
import org.jetbrains.jps.cmdline.JpsModelLoaderImpl;
import org.jetbrains.jps.cmdline.ProjectDescriptor;
import org.jetbrains.jps.incremental.MessageHandler;
import org.jetbrains.jps.incremental.Utils;
import org.jetbrains.jps.incremental.fs.BuildFSState;
import org.jetbrains.jps.incremental.messages.BuildMessage;
import org.jetbrains.jps.model.JpsModel;

import java.io.File;
import java.util.*;

/**
 * @author nik
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "CallToPrintStackTrace"})
public class Standalone {
  @Argument(value = "config", prefix = "--", description = "Path to directory containing global options (idea.config.path)")
  public String configPath;

  @Argument(value = "script", prefix = "--", description = "Path to Groovy script which will be used to initialize global options")
  public String initializationScriptPath;

  @Argument(value = "modules", prefix = "--", delimiter = ",", description = "Comma-separated list of modules to compile")
  public String[] modules = ArrayUtil.EMPTY_STRING_ARRAY;

  @Argument(value = "artifacts", prefix = "--", delimiter = ",", description = "Comma-separated list of artifacts to build")
  public String[] artifacts = ArrayUtil.EMPTY_STRING_ARRAY;

  @Argument(value = "i", description = "Build incrementally")
  public boolean incremental;

  public static void main(String[] args) {
    Standalone instance = new Standalone();
    List<String> projectPaths;
    try {
      projectPaths = Args.parse(instance, args);
    }
    catch (Exception e) {
      printUsageAndExit();
      return;
    }

    if (projectPaths.isEmpty()) {
      System.out.println("Path to project is not specified");
      printUsageAndExit();
    }
    if (projectPaths.size() > 1) {
      System.out.println("Only one project can be specified");
      printUsageAndExit();
    }



    instance.loadAndRunBuild(projectPaths.get(0));
    System.exit(0);
  }

  private static void printUsageAndExit() {
    Args.usage(System.err, new Standalone());
    System.exit(0);
  }

  public void loadAndRunBuild(final String projectPath) {
    String globalOptionsPath = null;
    if (configPath != null) {
      File optionsDir = new File(configPath, "options");
      if (!optionsDir.isDirectory()) {
        System.err.println("'" + configPath + "' is not valid config path: " + optionsDir.getAbsolutePath() + " not found");
        return;
      }
      globalOptionsPath = optionsDir.getAbsolutePath();
    }

    ParameterizedRunnable<JpsModel> initializer = null;
    String scriptPath = initializationScriptPath;
    if (scriptPath != null) {
      File scriptFile = new File(scriptPath);
      if (!scriptFile.isFile()) {
        System.err.println("Script '" + scriptPath + "' not found");
        return;
      }
      initializer = new GroovyModelInitializer(scriptFile);
    }

    Map<String, String> pathVars = new HashMap<String, String>();
    pathVars.put("USER_HOME", System.getProperty("user.home"));
    JpsModelLoaderImpl loader = new JpsModelLoaderImpl(projectPath, globalOptionsPath, pathVars, null, "", initializer);
    BuildType buildType = incremental ? BuildType.MAKE : BuildType.PROJECT_REBUILD;
    Set<String> modulesSet = new HashSet<String>(Arrays.asList(modules));
    List<String> artifactsList = Arrays.asList(artifacts);
    File dataStorageRoot = Utils.getDataStorageRoot(projectPath);
    if (dataStorageRoot == null) {
      System.err.println("Error: Cannot determine build data storage root for project " + projectPath);
      return;
    }

    try {
      runBuild(loader, dataStorageRoot, buildType, modulesSet, artifactsList, true, new ConsoleMessageHandler());
    }
    catch (Throwable t) {
      System.err.println("Internal error: " + t.getMessage());
      t.printStackTrace();
    }
  }

  public static void runBuild(JpsModelLoader loader, final File dataStorageRoot, BuildType buildType, Set<String> modulesSet,
                              List<String> artifactsList, final boolean includeTests, final MessageHandler messageHandler) throws Exception {
    final BuildRunner buildRunner = new BuildRunner(loader, modulesSet, artifactsList, Collections.<String>emptyList(), Collections.<String, String>emptyMap());
    ProjectDescriptor descriptor = buildRunner.load(messageHandler, dataStorageRoot, new BuildFSState(true));
    buildRunner.runBuild(descriptor, CanceledStatus.NULL, null, messageHandler, includeTests, buildType);
  }

  private static class ConsoleMessageHandler implements MessageHandler {
    @Override
    public void processMessage(BuildMessage msg) {
      if (msg.getKind() == BuildMessage.Kind.ERROR) {
        System.err.println("Error: " + msg.getMessageText());
      }
      else {
        System.out.println(msg.getMessageText());
      }
    }
  }
}
