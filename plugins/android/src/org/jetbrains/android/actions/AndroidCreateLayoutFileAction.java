/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

package org.jetbrains.android.actions;

import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.intellij.CommonBundle;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.containers.HashSet;
import org.jetbrains.android.dom.layout.AndroidLayoutUtil;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.facet.AndroidRootUtil;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.android.util.AndroidCommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;

/**
 * @author Eugene.Kudelevsky
 */
public class AndroidCreateLayoutFileAction extends CreateTypedResourceFileAction {
  private static final Logger LOG = Logger.getInstance("#org.jetbrains.android.actions.AndroidCreateLayoutFileAction");

  private String myLastRootComponentName;

  public AndroidCreateLayoutFileAction() {
    super("Layout", ResourceFolderType.LAYOUT, false, false);
  }

  @NotNull
  @Override
  protected PsiElement[] invokeDialog(Project project, PsiDirectory directory) {
    final AndroidFacet facet = AndroidFacet.getInstance(directory);
    LOG.assertTrue(facet != null);
    MyInputValidator validator = new MyInputValidator(project, directory);
    final MyDialog dialog = new MyDialog(facet, validator);
    dialog.show();
    return PsiElement.EMPTY_ARRAY;
  }

  @NotNull
  @Override
  protected PsiElement[] create(String newName, PsiDirectory directory) throws Exception {
    final String rootTag = myLastRootComponentName != null ? myLastRootComponentName : getDefaultRootTag();
    return doCreateAndNavigate(newName, directory, rootTag, false);
  }

  @Override
  protected void doNavigate(XmlFile file) {
    final Project project = file.getProject();
    final String preferredEditorProviderId = computePreferredEditorForLayoutFiles(project);
    super.doNavigate(file);

    if (preferredEditorProviderId != null) {
      final FileEditorManager manager = FileEditorManager.getInstance(project);
      if (manager instanceof FileEditorManagerImpl) {
        final VirtualFile vFile = file.getVirtualFile();
        if (vFile != null) {
          ((FileEditorManagerImpl)manager).setSelectedEditor(vFile, preferredEditorProviderId);
        }
      }
    }
  }

  @NotNull
  @Override
  public List<String> getAllowedTagNames(@NotNull AndroidFacet facet) {
    return AndroidLayoutUtil.getPossibleRoots(facet);
  }

  @Nullable
  private static String computePreferredEditorForLayoutFiles(Project project) {
    final EditorHistoryManager manager = EditorHistoryManager.getInstance(project);
    final Set<VirtualFile> resourceDirs = new HashSet<VirtualFile>();
    final List<AndroidFacet> facets = ProjectFacetManager.getInstance(project).getFacets(AndroidFacet.ID);

    for (AndroidFacet facet : facets) {
      final VirtualFile resourceDir = AndroidRootUtil.getResourceDir(facet);
      if (resourceDir != null) {
        resourceDirs.add(resourceDir);
      }
    }

    final VirtualFile[] files = manager.getFiles();
    for (int i = files.length - 1; i >= 0; i--) {
      final VirtualFile file = files[i];
      final VirtualFile parent = file.getParent();
      
      if (parent != null) {
        final String resType = AndroidCommonUtils.getResourceTypeByDirName(parent.getName());
        
        if (ResourceType.LAYOUT.getName().equals(resType) && resourceDirs.contains(parent.getParent())) {
          final FileEditorProvider provider = manager.getSelectedProvider(file);
          
          if (provider != null) {
            return provider.getEditorTypeId();
          }
        }
      }
    }
    return null;
  }

  public class MyDialog extends DialogWrapper {
    private final InputValidator myValidator;

    private JTextField myFileNameField;
    private TextFieldWithAutoCompletion<String> myRootElementField;
    private JPanel myPanel;
    private JPanel myRootElementFieldWrapper;
    private JBLabel myRootElementLabel;

    protected MyDialog(@NotNull AndroidFacet facet, @Nullable InputValidator validator) {
      super(facet.getModule().getProject());
      myValidator = validator;
      setTitle(AndroidBundle.message("new.typed.resource.dialog.title", "Layout"));
      final List<String> tagNames = getSortedAllowedTagNames(facet);
      myRootElementField = new TextFieldWithAutoCompletion<String>(
        facet.getModule().getProject(), new TextFieldWithAutoCompletion.StringsCompletionProvider(tagNames, null), true);
      myRootElementField.setText(myDefaultRootTag);
      myRootElementFieldWrapper.add(myRootElementField, BorderLayout.CENTER);
      myRootElementLabel.setLabelFor(myRootElementField);
      init();
    }

    @Override
    protected JComponent createCenterPanel() {
      return myPanel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
      return myFileNameField;
    }

    @Override
    protected void doOKAction() {
      final String fileName = myFileNameField.getText().trim();
      myLastRootComponentName = myRootElementField.getText().trim();

      if (fileName.length() == 0) {
        Messages.showErrorDialog(myPanel, AndroidBundle.message("file.name.not.specified.error"), CommonBundle.getErrorTitle());
        return;
      }
      if (myLastRootComponentName.length() == 0) {
        Messages.showErrorDialog(myPanel, AndroidBundle.message("root.element.not.specified.error"), CommonBundle.getErrorTitle());
        return;
      }
      if (myValidator == null ||
          myValidator.checkInput(fileName) && myValidator.canClose(fileName)) {
        super.doOKAction();
      }
    }
  }
}
