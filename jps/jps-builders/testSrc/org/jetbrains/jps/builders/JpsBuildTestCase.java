package org.jetbrains.jps.builders;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.UsefulTestCase;
import junit.framework.Assert;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.JpsPathUtil;
import org.jetbrains.jps.Project;
import org.jetbrains.jps.api.CanceledStatus;
import org.jetbrains.jps.cmdline.ClasspathBootstrap;
import org.jetbrains.jps.cmdline.ProjectDescriptor;
import org.jetbrains.jps.idea.IdeaProjectLoader;
import org.jetbrains.jps.incremental.*;
import org.jetbrains.jps.incremental.fs.BuildFSState;
import org.jetbrains.jps.incremental.storage.BuildDataManager;
import org.jetbrains.jps.incremental.storage.ProjectTimestamps;
import org.jetbrains.jps.model.JpsDummyElement;
import org.jetbrains.jps.model.JpsElementFactory;
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.java.*;
import org.jetbrains.jps.model.library.JpsOrderRootType;
import org.jetbrains.jps.model.library.JpsTypedLibrary;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.serialization.JpsProjectLoader;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * @author nik
 */
public abstract class JpsBuildTestCase extends UsefulTestCase {
  protected JpsProject myJpsProject;
  protected JpsModel myModel;
  protected Project myProject;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myProject = new Project();
    myProject.setProjectName(getProjectName());
    myModel = JpsElementFactory.getInstance().createModel();
    myJpsProject = myModel.getProject();
    Utils.setSystemRoot(FileUtil.createTempDirectory("compile-server", null));
  }

  protected JpsSdk<JpsDummyElement> addJdk(final String name) {
    try {
      return addJdk(name, FileUtil.toSystemIndependentName(ClasspathBootstrap.getResourcePath(Object.class).getCanonicalPath()));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected JpsSdk<JpsDummyElement> addJdk(final String name, final String path) {
    String homePath = System.getProperty("java.home");
    String versionString = System.getProperty("java.version");
    JpsTypedLibrary<JpsSdk<JpsDummyElement>> jdk = myModel.getGlobal().addSdk(name, homePath, versionString, JpsJavaSdkType.INSTANCE);
    jdk.addRoot(JpsPathUtil.pathToUrl(path), JpsOrderRootType.COMPILED);
    return jdk.getProperties();
  }

  protected String getProjectName() {
    return StringUtil.decapitalize(StringUtil.trimStart(getName(), "test"));
  }

  protected ProjectDescriptor createProjectDescriptor(final BuildLoggingManager buildLoggingManager) {
    try {
      final File dataStorageRoot = Utils.getDataStorageRoot(myProject);
      ProjectTimestamps timestamps = new ProjectTimestamps(dataStorageRoot);
      BuildDataManager dataManager = new BuildDataManager(dataStorageRoot, true);
      return new ProjectDescriptor(myProject, myModel, new BuildFSState(true), timestamps, dataManager, buildLoggingManager);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected void loadProject(String projectPath) {
    loadProject(projectPath, Collections.<String, String>emptyMap());
  }

  protected void loadProject(String projectPath,
                             Map<String, String> pathVariables) {
    try {
      String testDataRootPath = getTestDataRootPath();
      String fullProjectPath = FileUtil.toSystemDependentName(testDataRootPath != null ? testDataRootPath + "/" + projectPath : projectPath);
      pathVariables = addPathVariables(pathVariables);
      IdeaProjectLoader.loadFromPath(myProject, fullProjectPath, pathVariables);
      JpsProjectLoader.loadProject(myJpsProject, pathVariables, fullProjectPath);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected Map<String, String> addPathVariables(Map<String, String> pathVariables) {
    return pathVariables;
  }

  @Nullable
  protected String getTestDataRootPath() {
    return null;
  }

  protected JpsModule addModule(String moduleName,
                                String[] srcPaths,
                                @Nullable final String outputPath,
                                final JpsSdk<JpsDummyElement> jdk) {
    final JpsModule module = myJpsProject.addModule(moduleName, JpsJavaModuleType.INSTANCE);
    module.getSdkReferencesTable().setSdkReference(JpsJavaSdkType.INSTANCE, jdk.createReference());
    module.getDependenciesList().addSdkDependency(JpsJavaSdkType.INSTANCE);
    if (srcPaths.length > 0) {
      for (String srcPath : srcPaths) {
        module.getContentRootsList().addUrl(JpsPathUtil.pathToUrl(srcPath));
        module.addSourceRoot(JpsPathUtil.pathToUrl(srcPath), JavaSourceRootType.SOURCE);
      }
      JpsJavaModuleExtension extension = JpsJavaExtensionService.getInstance().getOrCreateModuleExtension(module);
      if (outputPath != null) {
        extension.setOutputUrl(JpsPathUtil.pathToUrl(outputPath));
      }
      else {
        extension.setInheritOutput(true);
      }
    }
    return module;
  }

  protected BuildResult doBuild(final ProjectDescriptor descriptor, CompileScope scope,
                                final boolean make, final boolean rebuild, final boolean forceCleanCaches) {
    IncProjectBuilder builder = new IncProjectBuilder(descriptor, BuilderRegistry.getInstance(), Collections.<String, String>emptyMap(), CanceledStatus.NULL, null);
    BuildResult result = new BuildResult();
    builder.addMessageHandler(result);
    try {
      builder.build(scope, make, rebuild, forceCleanCaches);
    }
    catch (RebuildRequestedException e) {
      Assert.fail(e.getMessage());
    }
    return result;
  }
}
