/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.roots.ui.configuration.projectRoot;

import com.intellij.facet.FacetType;
import com.intellij.facet.impl.ui.facetType.FacetTypeEditor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.ui.NamedConfigurable;

import javax.swing.*;

/**
 * @author nik
 */
public class FacetTypeConfigurable extends NamedConfigurable<FacetType> {
  private final FacetStructureConfigurable myFacetStructureConfigurable;
  private final FacetType myFacetType;

  public FacetTypeConfigurable(final FacetStructureConfigurable facetStructureConfigurable, final FacetType facetType) {
    myFacetStructureConfigurable = facetStructureConfigurable;
    myFacetType = facetType;
  }

  public void setDisplayName(final String name) {
  }

  public FacetType getEditableObject() {
    return myFacetType;
  }

  public String getBannerSlogan() {
    return ProjectBundle.message("facet.type.banner.text", myFacetType.getPresentableName());
  }

  public JComponent createOptionsPanel() {
    return myFacetStructureConfigurable.getOrCreateFacetTypeEditor(myFacetType).createComponent();
  }

  public String getDisplayName() {
    return myFacetType.getPresentableName();
  }

  public String getHelpTopic() {
    final FacetTypeEditor editor = myFacetStructureConfigurable.getFacetTypeEditor(myFacetType);
    return editor != null ? editor.getHelpTopic() : null;
  }

  public boolean isModified() {
    return false;
  }

  public void apply() throws ConfigurationException {
  }

  public void reset() {
  }

  public void disposeUIResources() {
  }

  public void updateComponent() {
    resetOptionsPanel();
  }
}
