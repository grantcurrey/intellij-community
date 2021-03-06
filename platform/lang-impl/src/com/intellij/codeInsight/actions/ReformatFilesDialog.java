/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package com.intellij.codeInsight.actions;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ReformatFilesDialog extends DialogWrapper {
  private JPanel myPanel;
  private JCheckBox myOptimizeImports;
  private JCheckBox myOnlyChangedText;
  private final VirtualFile[] myFiles;

  public ReformatFilesDialog(@NotNull Project project, @NotNull VirtualFile[] files) {
    super(project, true);
    myFiles = files;
    setTitle(CodeInsightBundle.message("dialog.reformat.files.title"));
    myOptimizeImports.setSelected(isOptmizeImportsOptionOn());
    boolean canTargetVcsChanges = false;
    for (VirtualFile file : files) {
      if (FormatChangedTextUtil.hasChanges(file, project)) {
        canTargetVcsChanges = true;
        break;
      }
    }
    myOnlyChangedText.setEnabled(canTargetVcsChanges);
    myOnlyChangedText.setSelected(
      canTargetVcsChanges && PropertiesComponent.getInstance().getBoolean(LayoutCodeConstants.PROCESS_CHANGED_TEXT_KEY, false)
    ); 
    myOptimizeImports.setSelected(isOptmizeImportsOptionOn());
    init();
  }

  protected JComponent createCenterPanel() {
    return myPanel;
  }

  public boolean optimizeImports(){
    return myOptimizeImports.isSelected();
  }

  public boolean isProcessOnlyChangedText() {
    return myOnlyChangedText.isEnabled() && myOnlyChangedText.isSelected();
  }

  protected void doOKAction() {
    super.doOKAction();
    PropertiesComponent.getInstance().setValue(LayoutCodeConstants.OPTIMIZE_IMPORTS_KEY, Boolean.toString(myOptimizeImports.isSelected()));
    PropertiesComponent.getInstance().setValue(LayoutCodeConstants.PROCESS_CHANGED_TEXT_KEY,
                                               Boolean.toString(myOnlyChangedText.isSelected()));
  }

  static boolean isOptmizeImportsOptionOn() {
    return PropertiesComponent.getInstance().getBoolean(LayoutCodeConstants.OPTIMIZE_IMPORTS_KEY, false);
  }

}
