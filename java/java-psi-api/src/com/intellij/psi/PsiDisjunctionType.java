/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.psi;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.Function;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Composite type resulting from Project Coin's multi-catch statements, i.e. <code>FileNotFoundException | EOFException</code>.
 * In most cases should be threatened via its least upper bound (<code>IOException</code> in the example above).
 */
public class PsiDisjunctionType extends PsiType {
  private final PsiManager myManager;
  private final List<PsiType> myTypes;
  private final CachedValue<PsiType> myLubCache;

  public PsiDisjunctionType(@NotNull final List<PsiType> types, @NotNull final PsiManager psiManager) {
    super(PsiAnnotation.EMPTY_ARRAY);

    myManager = psiManager;
    myTypes = Collections.unmodifiableList(types);

    final CachedValuesManager cacheManager = CachedValuesManager.getManager(psiManager.getProject());
    myLubCache = cacheManager.createCachedValue(new CachedValueProvider<PsiType>() {
      @Override
      public Result<PsiType> compute() {
        PsiType lub = myTypes.get(0);
        for (int i = 1; i < myTypes.size(); i++) {
          lub = GenericsUtil.getLeastUpperBound(lub, myTypes.get(i), psiManager);
          if (lub == null) {
            lub = PsiType.getJavaLangObject(myManager, GlobalSearchScope.allScope(myManager.getProject()));
            break;
          }
        }
        return Result.create(lub, PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT);
      }
    }, false);
  }

  @NotNull
  public PsiType getLeastUpperBound() {
    return myLubCache.getValue();
  }

  @NotNull
  public List<PsiType> getDisjunctions() {
    return myTypes;
  }

  @NotNull
  public PsiDisjunctionType newDisjunctionType(final List<PsiType> types) {
    return new PsiDisjunctionType(types, myManager);
  }

  @Override
  public String getPresentableText() {
    return StringUtil.join(myTypes, new Function<PsiType, String>() {
      @Override public String fun(PsiType psiType) { return psiType.getPresentableText(); }
    }, " | ");
  }

  @Override
  public String getCanonicalText() {
    return StringUtil.join(myTypes, new Function<PsiType, String>() {
      @Override public String fun(PsiType psiType) { return psiType.getCanonicalText(); }
    }, " | ");
  }

  @Override
  public String getInternalCanonicalText() {
    return StringUtil.join(myTypes, new Function<PsiType, String>() {
      @Override public String fun(PsiType psiType) { return psiType.getInternalCanonicalText(); }
    }, " | ");
  }

  @Override
  public boolean isValid() {
    for (PsiType type : myTypes) {
      if (!type.isValid()) return false;
    }
    return true;
  }

  @Override
  public boolean equalsToText(@NonNls final String text) {
    return Comparing.equal(text, getCanonicalText());
  }

  @Override
  public <A> A accept(@NotNull final PsiTypeVisitor<A> visitor) {
    return visitor.visitDisjunctionType(this);
  }

  @Override
  public GlobalSearchScope getResolveScope() {
    return getLeastUpperBound().getResolveScope();
  }

  @NotNull
  @Override
  public PsiType[] getSuperTypes() {
    final PsiType lub = getLeastUpperBound();
    if (lub instanceof PsiIntersectionType) {
      return ((PsiIntersectionType)lub).getConjuncts();
    }
    else {
      return new PsiType[]{lub};
    }
  }
}
