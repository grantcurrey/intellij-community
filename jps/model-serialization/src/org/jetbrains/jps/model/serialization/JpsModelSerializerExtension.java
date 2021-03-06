package org.jetbrains.jps.model.serialization;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.JpsCompositeElement;
import org.jetbrains.jps.model.JpsElementReference;
import org.jetbrains.jps.model.module.JpsDependencyElement;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.serialization.artifact.JpsArtifactPropertiesSerializer;
import org.jetbrains.jps.model.serialization.artifact.JpsPackagingElementSerializer;
import org.jetbrains.jps.model.serialization.facet.JpsFacetConfigurationSerializer;
import org.jetbrains.jps.model.serialization.library.JpsLibraryPropertiesSerializer;
import org.jetbrains.jps.model.serialization.library.JpsLibraryRootTypeSerializer;
import org.jetbrains.jps.model.serialization.library.JpsSdkPropertiesSerializer;
import org.jetbrains.jps.model.serialization.module.JpsModulePropertiesSerializer;
import org.jetbrains.jps.service.JpsServiceManager;

import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public abstract class JpsModelSerializerExtension {
  public static Iterable<JpsModelSerializerExtension> getExtensions() {
    return JpsServiceManager.getInstance().getExtensions(JpsModelSerializerExtension.class);
  }

  public void loadRootModel(@NotNull JpsModule module, @NotNull Element rootModel) {
  }

  public void saveRootModel(@NotNull JpsModule module, @NotNull Element rootModel) {
  }

  public List<JpsLibraryRootTypeSerializer> getLibraryRootTypeSerializers() {
    return Collections.emptyList();
  }

  @NotNull
  public List<JpsLibraryRootTypeSerializer> getSdkRootTypeSerializers() {
    return Collections.emptyList();
  }

  public void loadModuleDependencyProperties(JpsDependencyElement dependency, Element orderEntry) {
  }

  public void saveModuleDependencyProperties(JpsDependencyElement dependency, Element orderEntry) {
  }

  @Nullable
  public JpsElementReference<? extends JpsCompositeElement> createLibraryTableReference(String tableLevel) {
    return null;
  }

  @Nullable
  public String getLibraryTableLevelId(JpsElementReference<? extends JpsCompositeElement> reference) {
    return null;
  }

  @NotNull
  public List<? extends JpsProjectExtensionSerializer> getProjectExtensionSerializers() {
    return Collections.emptyList();
  }

  @NotNull
  public List<? extends JpsGlobalExtensionSerializer> getGlobalExtensionSerializers() {
    return Collections.emptyList();
  }

  @NotNull
  public List<? extends JpsModulePropertiesSerializer<?>> getModulePropertiesSerializers() {
    return Collections.emptyList();
  }

  @NotNull
  public List<? extends JpsLibraryPropertiesSerializer<?>> getLibraryPropertiesSerializers() {
    return Collections.emptyList();
  }

  @NotNull
  public List<? extends JpsSdkPropertiesSerializer<?>> getSdkPropertiesSerializers() {
    return Collections.emptyList();
  }

  public List<? extends JpsFacetConfigurationSerializer<?>> getFacetConfigurationSerializers() {
    return Collections.emptyList();
  }

  public List<? extends JpsPackagingElementSerializer<?>> getPackagingElementSerializers() {
    return Collections.emptyList();
  }

  public List<? extends JpsArtifactPropertiesSerializer<?>> getArtifactTypePropertiesSerializers() {
    return Collections.emptyList();
  }
}
