// Copyright 2008-2010 Victor Iacoban
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distributed under
// the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// either express or implied. See the License for the specific language governing permissions and
// limitations under the License.
package org.zmlx.hg4idea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.zmlx.hg4idea.HgPusher;
import org.zmlx.hg4idea.HgVcsMessages;
import org.zmlx.hg4idea.command.HgTagBranch;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;

public class HgPushDialog extends DialogWrapper {

  private final Project myProject;

  private JPanel contentPanel;
  private JTextField repositoryTxt;
  private JCheckBox revisionCbx;
  private JTextField revisionTxt;
  private HgRepositorySelectorComponent hgRepositorySelectorComponent;
  private JCheckBox forceCheckBox;
  private JCheckBox branchCheckBox;
  private JComboBox branchComboBox;

  public HgPushDialog(Project project, Collection<VirtualFile> repos, String defaultPushPath, List<HgTagBranch> branches) {
    super(project, false);
    myProject = project;

    hgRepositorySelectorComponent.setRoots(repos);
    updateBranchComboBox(branches);
    updateRepositoryUrlText(defaultPushPath);

    hgRepositorySelectorComponent.setTitle("Select repository to push from");
    hgRepositorySelectorComponent.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        updateRepository();
      }
    });

    final UpdatingListener updatingListener = new UpdatingListener();
    revisionCbx.addChangeListener(updatingListener);
    branchCheckBox.addChangeListener(updatingListener);
    repositoryTxt.getDocument().addDocumentListener(updatingListener);
    revisionTxt.getDocument().addDocumentListener(updatingListener);

    setTitle(HgVcsMessages.message("hg4idea.push.dialog.title"));
    setOKButtonText("Push");
    init();
  }

  public VirtualFile getRepository() {
    return hgRepositorySelectorComponent.getRepository();
  }

  public String getTarget() {
    return repositoryTxt.getText();
  }

  @Nullable
  public String getRevision() {
    return revisionCbx.isSelected() ? revisionTxt.getText() : null;
  }

  @Nullable
  public HgTagBranch getBranch() {
    return branchCheckBox.isSelected() ? (HgTagBranch) branchComboBox.getSelectedItem() : null;
  }

  public boolean isForce() {
    return forceCheckBox.isSelected();
  }

  protected JComponent createCenterPanel() {
    return contentPanel;
  }

  @Override
  protected String getHelpId() {
    return "reference.mercurial.push.dialog";
  }

  public void updateRepository() {
    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
      @Override
      public void run() {
        final VirtualFile repo = hgRepositorySelectorComponent.getRepository();
        final String defaultPath = HgPusher.getDefaultPushPath(myProject, repo);
        final List<HgTagBranch> branches = HgPusher.getBranches(myProject, repo);
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            updateRepositoryUrlText(defaultPath);
            updateBranchComboBox(branches);
          }
        }, ModalityState.stateForComponent(getRootPane()));
      }
    });
  }

  private void updateRepositoryUrlText(String defaultPath) {
    repositoryTxt.setText(defaultPath);
    update();
  }

  private void updateBranchComboBox(@NotNull List<HgTagBranch> branches) {
    branchComboBox.setModel(new DefaultComboBoxModel(branches.toArray()));
  }

  private void update() {
    setOKActionEnabled(validateOptions());
    revisionTxt.setEnabled(revisionCbx.isSelected());
    branchComboBox.setEnabled(branchCheckBox.isSelected());
  }

  private boolean validateOptions() {
    return !StringUtil.isEmptyOrSpaces(repositoryTxt.getText())
      && !(revisionCbx.isSelected() && StringUtil.isEmptyOrSpaces(revisionTxt.getText()))
      && !(branchCheckBox.isSelected() && (branchComboBox.getSelectedItem() == null));
  }

  @Override
  protected String getDimensionServiceKey() {
    return HgPushDialog.class.getName();
  }

  /**
   * Updates the form on every change.
   */
  private class UpdatingListener implements ChangeListener, DocumentListener {

    public void stateChanged(ChangeEvent e) {
      update();
    }

    public void insertUpdate(DocumentEvent e) {
      update();
    }

    public void removeUpdate(DocumentEvent e) {
      update();
    }

    public void changedUpdate(DocumentEvent e) {
      update();
    }
  }

}
