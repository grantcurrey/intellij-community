/*
 * User: anna
 * Date: 16-May-2007
 */
package com.intellij.codeInspection;

import com.intellij.JavaTestUtil;
import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase;
import com.intellij.codeInspection.sameParameterValue.SameParameterValueInspection;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NonNls;

public class InlineSameParameterValueTest extends LightQuickFixTestCase {
  @Override
  protected String getTestDataPath() {
    return JavaTestUtil.getJavaTestDataPath() + "/inspection";
  }

  public void test() throws Exception {
    doAllTests();
  }

  @Override
  protected void doAction(final String text, final boolean actionShouldBeAvailable, final String testFullPath, final String testName)
    throws Exception {
    final LocalQuickFix fix = (LocalQuickFix)new SameParameterValueInspection().getQuickFix(text);
    assert fix != null;
    final int offset = getEditor().getCaretModel().getOffset();
    final PsiElement psiElement = getFile().findElementAt(offset);
    assert psiElement != null;
    final ProblemDescriptor descriptor = InspectionManager.getInstance(getProject())
      .createProblemDescriptor(psiElement, "", fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, true);
    fix.applyFix(getProject(), descriptor);
    final String expectedFilePath = getBasePath() + "/after" + testName;
    checkResultByFile("In file :" + expectedFilePath, expectedFilePath, false);
  }


  @Override
  @NonNls
  protected String getBasePath() {
    return "/quickFix/SameParameterValue";
  }
}
