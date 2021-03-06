package org.jetbrains.jps.cmdline;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ParameterizedRunnable;
import org.jetbrains.jps.Project;
import org.jetbrains.jps.idea.IdeaProjectLoader;
import org.jetbrains.jps.idea.SystemOutErrorReporter;
import org.jetbrains.jps.model.JpsElementFactory;
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.model.serialization.JpsGlobalLoader;
import org.jetbrains.jps.model.serialization.JpsProjectLoader;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author nik
 */
public class JpsModelLoaderImpl implements JpsModelLoader {
  public static final String IDEA_PROJECT_DIRNAME = ".idea";
  private static final Logger LOG = Logger.getInstance("#org.jetbrains.jps.cmdline.JpsModelLoaderImpl");
  private final String myProjectPath;
  private final String myGlobalOptionsPath;
  private final Map<String, String> myPathVars;
  private final String myGlobalEncoding;
  private final String myIgnorePatterns;
  private final ParameterizedRunnable<JpsModel> myModelInitializer;

  public JpsModelLoaderImpl(String projectPath,
                            String globalOptionsPath,
                            Map<String, String> pathVars,
                            String globalEncoding,
                            String ignorePatterns, ParameterizedRunnable<JpsModel> initializer) {
    myProjectPath = projectPath;
    myGlobalOptionsPath = globalOptionsPath;
    myPathVars = pathVars;
    myGlobalEncoding = globalEncoding;
    myIgnorePatterns = ignorePatterns;
    myModelInitializer = initializer;
  }

  @Override
  public JpsModel loadModel() {
    final long start = System.currentTimeMillis();
    try {
      final JpsModel model = JpsElementFactory.getInstance().createModel();
      try {
        if (myGlobalOptionsPath != null) {
          JpsGlobalLoader.loadGlobalSettings(model.getGlobal(), myPathVars, myGlobalOptionsPath);
        }
        JpsProjectLoader.loadProject(model.getProject(), myPathVars, myProjectPath);
        if (myModelInitializer != null) {
          myModelInitializer.run(model);
        }
        LOG.info("New JPS model: " + model.getProject().getModules().size() + " modules, " + model.getProject().getLibraryCollection().getLibraries().size() + " libraries");
      }
      catch (IOException e) {
        LOG.info(e);
      }
      return model;
    }
    finally {
      final long loadTime = System.currentTimeMillis() - start;
      LOG.info("New JPS model: project " + myProjectPath + " loaded in " + loadTime + " ms");
    }
  }

  @Override
  public Project loadOldProject() {
    final long start = System.currentTimeMillis();
    try {
      final Project project = new Project();

      final File projectFile = new File(myProjectPath);

      final String loadPath = isDirectoryBased(projectFile) ? new File(projectFile, IDEA_PROJECT_DIRNAME).getPath() : myProjectPath;
      IdeaProjectLoader.loadFromPath(project, loadPath, myPathVars, null, new SystemOutErrorReporter(false));
      final String globalEncoding = myGlobalEncoding;
      if (!StringUtil.isEmpty(globalEncoding) && project.getProjectCharset() == null) {
        project.setProjectCharset(globalEncoding);
      }
      project.getIgnoredFilePatterns().loadFromString(myIgnorePatterns);
      return project;
    }
    finally {
      final long loadTime = System.currentTimeMillis() - start;
      LOG.info("Project " + myProjectPath + " loaded in " + loadTime + " ms");
    }
  }

  private static boolean isDirectoryBased(File projectFile) {
    return !(projectFile.isFile() && projectFile.getName().endsWith(".ipr"));
  }
}
