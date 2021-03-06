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

package com.intellij.ide.hierarchy;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.AnActionEvent;

/**
 * @author cdr
 */
public final class ViewClassHierarchyAction extends ChangeViewTypeActionBase {
  public ViewClassHierarchyAction() {
    super(IdeBundle.message("action.view.class.hierarchy"),
          IdeBundle.message("action.description.view.class.hierarchy"), AllIcons.Hierarchy.Class);
  }

  protected final String getTypeName() {
    return TypeHierarchyBrowserBase.TYPE_HIERARCHY_TYPE;
  }

  public final void update(final AnActionEvent event) {
    super.update(event);
    final TypeHierarchyBrowserBase browser = getTypeHierarchyBrowser(event.getDataContext());
    event.getPresentation().setEnabled(browser != null && !browser.isInterface());
  }
}
