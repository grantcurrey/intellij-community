package org.jetbrains.jps.model.serialization.module;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jdom.Element;
import org.jetbrains.jps.model.JpsCompositeElement;
import org.jetbrains.jps.model.JpsElementFactory;
import org.jetbrains.jps.model.JpsElementReference;
import org.jetbrains.jps.model.JpsSimpleElement;
import org.jetbrains.jps.model.java.JavaSourceRootProperties;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.java.JpsJavaSdkType;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsLibraryReference;
import org.jetbrains.jps.model.library.sdk.JpsSdkType;
import org.jetbrains.jps.model.library.sdk.JpsSdkReference;
import org.jetbrains.jps.model.module.*;
import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension;
import org.jetbrains.jps.model.serialization.library.JpsLibraryTableSerializer;
import org.jetbrains.jps.model.serialization.library.JpsSdkTableSerializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.intellij.openapi.util.JDOMUtil.getChildren;

/**
 * @author nik
 */
public class JpsModuleRootModelSerializer {
  public static final String URL_ATTRIBUTE = "url";
  public static final String CONTENT_TAG = "content";
  public static final String SOURCE_FOLDER_TAG = "sourceFolder";
  public static final String PACKAGE_PREFIX_ATTRIBUTE = "packagePrefix";
  public static final String IS_TEST_SOURCE_ATTRIBUTE = "isTestSource";
  public static final String EXCLUDE_FOLDER_TAG = "excludeFolder";
  public static final String ORDER_ENTRY_TAG = "orderEntry";
  public static final String TYPE_ATTRIBUTE = "type";
  public static final String SOURCE_FOLDER_TYPE = "sourceFolder";
  public static final String JDK_TYPE = "jdk";
  public static final String JDK_NAME_ATTRIBUTE = "jdkName";
  public static final String JDK_TYPE_ATTRIBUTE = "jdkType";
  public static final String INHERITED_JDK_TYPE = "inheritedJdk";
  public static final String LIBRARY_TYPE = "library";
  public static final String NAME_ATTRIBUTE = "name";
  public static final String LEVEL_ATTRIBUTE = "level";
  public static final String LIBRARY_TAG = "library";
  public static final String MODULE_LIBRARY_TYPE = "module-library";
  public static final String MODULE_TYPE = "module";
  public static final String MODULE_NAME_ATTRIBUTE = "module-name";
  private static final String GENERATED_LIBRARY_NAME_PREFIX = "#";

  public static void loadRootModel(JpsModule module, Element rootModelComponent, JpsSdkType<?> projectSdkType) {
    for (Element contentElement : getChildren(rootModelComponent, CONTENT_TAG)) {
      final String url = contentElement.getAttributeValue(URL_ATTRIBUTE);
      module.getContentRootsList().addUrl(url);
      for (Element sourceElement : getChildren(contentElement, SOURCE_FOLDER_TAG)) {
        final String sourceUrl = sourceElement.getAttributeValue(URL_ATTRIBUTE);
        final String packagePrefix = StringUtil.notNullize(sourceElement.getAttributeValue(PACKAGE_PREFIX_ATTRIBUTE));
        final boolean testSource = Boolean.parseBoolean(sourceElement.getAttributeValue(IS_TEST_SOURCE_ATTRIBUTE));
        final JavaSourceRootType rootType = testSource ? JavaSourceRootType.TEST_SOURCE : JavaSourceRootType.SOURCE;
        module.addSourceRoot(sourceUrl, rootType, JpsElementFactory.getInstance().createSimpleElement(new JavaSourceRootProperties(packagePrefix)));
      }
      for (Element excludeElement : getChildren(contentElement, EXCLUDE_FOLDER_TAG)) {
        module.getExcludeRootsList().addUrl(excludeElement.getAttributeValue(URL_ATTRIBUTE));
      }
    }

    final JpsDependenciesList dependenciesList = module.getDependenciesList();
    dependenciesList.clear();
    final JpsElementFactory elementFactory = JpsElementFactory.getInstance();
    int moduleLibraryNum = 0;
    for (Element orderEntry : getChildren(rootModelComponent, ORDER_ENTRY_TAG)) {
      String type = orderEntry.getAttributeValue(TYPE_ATTRIBUTE);
      if (SOURCE_FOLDER_TYPE.equals(type)) {
        dependenciesList.addModuleSourceDependency();
      }
      else if (JDK_TYPE.equals(type)) {
        String sdkName = orderEntry.getAttributeValue(JDK_NAME_ATTRIBUTE);
        String sdkTypeId = orderEntry.getAttributeValue(JDK_TYPE_ATTRIBUTE);
        final JpsSdkType<?> sdkType = JpsSdkTableSerializer.getSdkType(sdkTypeId);
        dependenciesList.addSdkDependency(sdkType);
        JpsSdkTableSerializer.setSdkReference(module.getSdkReferencesTable(), sdkName, sdkType);
      }
      else if (INHERITED_JDK_TYPE.equals(type)) {
        dependenciesList.addSdkDependency(projectSdkType != null ? projectSdkType : JpsJavaSdkType.INSTANCE);
      }
      else if (LIBRARY_TYPE.equals(type)) {
        String name = orderEntry.getAttributeValue(NAME_ATTRIBUTE);
        String level = orderEntry.getAttributeValue(LEVEL_ATTRIBUTE);
        final JpsLibraryDependency dependency =
          dependenciesList.addLibraryDependency(elementFactory.createLibraryReference(name, JpsLibraryTableSerializer
            .createLibraryTableReference(level)));
        loadModuleDependencyProperties(dependency, orderEntry);
      }
      else if (MODULE_LIBRARY_TYPE.equals(type)) {
        final Element moduleLibraryElement = orderEntry.getChild(LIBRARY_TAG);
        String name = moduleLibraryElement.getAttributeValue(NAME_ATTRIBUTE);
        if (name == null) {
          name = GENERATED_LIBRARY_NAME_PREFIX + (moduleLibraryNum++);
        }
        final JpsLibrary library = JpsLibraryTableSerializer.loadLibrary(moduleLibraryElement, name);
        module.addModuleLibrary(library);

        final JpsLibraryDependency dependency = dependenciesList.addLibraryDependency(library);
        loadModuleDependencyProperties(dependency, orderEntry);
        moduleLibraryNum++;
      }
      else if (MODULE_TYPE.equals(type)) {
        String name = orderEntry.getAttributeValue(MODULE_NAME_ATTRIBUTE);
        final JpsModuleDependency dependency = dependenciesList.addModuleDependency(elementFactory.createModuleReference(name));
        loadModuleDependencyProperties(dependency, orderEntry);
      }
    }

    for (JpsModelSerializerExtension extension : JpsModelSerializerExtension.getExtensions()) {
      extension.loadRootModel(module, rootModelComponent);
    }
  }

  public static void saveRootModel(JpsModule module, Element rootModelElement) {
    List<JpsModuleSourceRoot> sourceRoots = module.getSourceRoots();
    List<String> excludedUrls = getSortedList(module.getExcludeRootsList().getUrls());
    for (String url : getSortedList(module.getContentRootsList().getUrls())) {
      Element contentElement = new Element(CONTENT_TAG);
      contentElement.setAttribute(URL_ATTRIBUTE, url);
      rootModelElement.addContent(contentElement);
      for (JpsModuleSourceRoot root : sourceRoots) {
        if (FileUtil.startsWith(root.getUrl(), url)) {
          Element sourceElement = new Element(SOURCE_FOLDER_TAG);
          sourceElement.setAttribute(URL_ATTRIBUTE, root.getUrl());
          JpsModuleSourceRootType<?> type = root.getRootType();
          sourceElement.setAttribute(IS_TEST_SOURCE_ATTRIBUTE, Boolean.toString(type.equals(JavaSourceRootType.TEST_SOURCE)));
          if (type instanceof JavaSourceRootType) {
            JpsSimpleElement<JavaSourceRootProperties> properties = root.getProperties((JavaSourceRootType)type);
            if (properties != null) {
              String packagePrefix = properties.getData().getPackagePrefix();
              if (packagePrefix.length() > 0) {
                sourceElement.setAttribute(PACKAGE_PREFIX_ATTRIBUTE, packagePrefix);
              }
            }
          }
          contentElement.addContent(sourceElement);
        }
      }
      for (String excludedUrl : excludedUrls) {
        if (FileUtil.startsWith(excludedUrl, url)) {
          Element element = new Element(EXCLUDE_FOLDER_TAG).setAttribute(URL_ATTRIBUTE, excludedUrl);
          contentElement.addContent(element);
        }
      }
    }

    for (JpsDependencyElement dependency : module.getDependenciesList().getDependencies()) {
      if (dependency instanceof JpsModuleSourceDependency) {
        rootModelElement.addContent(createDependencyElement(SOURCE_FOLDER_TYPE).setAttribute("forTests", "false"));
      }
      else if (dependency instanceof JpsSdkDependency) {
        JpsSdkType<?> sdkType = ((JpsSdkDependency)dependency).getSdkType();
        JpsSdkReferencesTable table = module.getSdkReferencesTable();
        JpsSdkReference<?> reference = table.getSdkReference(sdkType);
        if (reference == null) {
          rootModelElement.addContent(createDependencyElement(INHERITED_JDK_TYPE));
        }
        else {
          Element element = createDependencyElement(JDK_TYPE);
          element.setAttribute(JDK_NAME_ATTRIBUTE, reference.getSdkName());
          element.setAttribute(JDK_TYPE_ATTRIBUTE, JpsSdkTableSerializer.getLoader(sdkType).getTypeId());
          rootModelElement.addContent(element);
        }
      }
      else if (dependency instanceof JpsLibraryDependency) {
        JpsLibraryReference reference = ((JpsLibraryDependency)dependency).getLibraryReference();
        JpsElementReference<? extends JpsCompositeElement> parentReference = reference.getParentReference();
        Element element;
        if (parentReference instanceof JpsModuleReference) {
          element = createDependencyElement(MODULE_LIBRARY_TYPE);
          saveModuleDependencyProperties(dependency, element);
          Element libraryElement = new Element(LIBRARY_TAG);
          JpsLibrary library = reference.resolve();
          String libraryName = library.getName();
          JpsLibraryTableSerializer.saveLibrary(library, libraryElement, isGeneratedName(libraryName) ? null : libraryName);
          element.addContent(libraryElement);
        }
        else {
          element = createDependencyElement(LIBRARY_TYPE);
          saveModuleDependencyProperties(dependency, element);
          element.setAttribute(NAME_ATTRIBUTE, reference.getLibraryName());
          element.setAttribute(LEVEL_ATTRIBUTE, JpsLibraryTableSerializer.getLevelId(parentReference));
        }
        rootModelElement.addContent(element);
      }
      else if (dependency instanceof JpsModuleDependency) {
        Element element = createDependencyElement(MODULE_TYPE);
        element.setAttribute(MODULE_NAME_ATTRIBUTE, ((JpsModuleDependency)dependency).getModuleReference().getModuleName());
        saveModuleDependencyProperties(dependency, element);
        rootModelElement.addContent(element);
      }
    }

    for (JpsModelSerializerExtension extension : JpsModelSerializerExtension.getExtensions()) {
      extension.saveRootModel(module, rootModelElement);
    }
  }

  private static boolean isGeneratedName(String libraryName) {
    return libraryName.startsWith(GENERATED_LIBRARY_NAME_PREFIX);
  }

  private static Element createDependencyElement(final String type) {
    return new Element(ORDER_ENTRY_TAG).setAttribute(TYPE_ATTRIBUTE, type);
  }

  private static List<String> getSortedList(final List<String> list) {
    List<String> strings = new ArrayList<String>(list);
    Collections.sort(strings);
    return strings;
  }

  private static void loadModuleDependencyProperties(JpsDependencyElement dependency, Element orderEntry) {
    for (JpsModelSerializerExtension extension : JpsModelSerializerExtension.getExtensions()) {
      extension.loadModuleDependencyProperties(dependency, orderEntry);
    }
  }

  private static void saveModuleDependencyProperties(JpsDependencyElement dependency, Element orderEntry) {
    for (JpsModelSerializerExtension extension : JpsModelSerializerExtension.getExtensions()) {
      extension.saveModuleDependencyProperties(dependency, orderEntry);
    }
  }
}
