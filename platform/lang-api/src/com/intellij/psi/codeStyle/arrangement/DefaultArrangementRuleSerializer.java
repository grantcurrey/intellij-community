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
package com.intellij.psi.codeStyle.arrangement;

import com.intellij.psi.codeStyle.arrangement.match.DefaultArrangementEntryMatcherSerializer;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementEntryMatcher;
import com.intellij.psi.codeStyle.arrangement.order.ArrangementEntryOrderType;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
/**
 * @author Denis Zhdanov
 * @since 7/18/12 10:37 AM
 */
public class DefaultArrangementRuleSerializer implements ArrangementRuleSerializer {

  public static final ArrangementRuleSerializer INSTANCE = new DefaultArrangementRuleSerializer();

  @NotNull @NonNls private static final String RULE_ELEMENT_NAME       = "rule";
  @NotNull @NonNls private static final String MATCHER_ELEMENT_NAME    = "match";
  @NotNull @NonNls private static final String ORDER_TYPE_ELEMENT_NAME = "order";

  @NotNull private final DefaultArrangementEntryMatcherSerializer myMatcherSerializer = new DefaultArrangementEntryMatcherSerializer();

  @Nullable
  @Override
  public StdArrangementRule deserialize(@NotNull Element element) {
    Element matcherElement = element.getChild(MATCHER_ELEMENT_NAME);
    if (matcherElement == null) {
      return null;
    }

    StdArrangementEntryMatcher matcher = null;
    for (Object o : matcherElement.getChildren()) {
      matcher = myMatcherSerializer.deserialize((Element)o);
      if (matcher != null) {
        break;
      }
    }

    if (matcher == null) {
      return null;
    }

    Element orderTypeElement = element.getChild(ORDER_TYPE_ELEMENT_NAME);
    ArrangementEntryOrderType orderType = ArrangementRule.DEFAULT_ORDER_TYPE;
    if (orderTypeElement != null) {
      orderType = ArrangementEntryOrderType.valueOf(orderTypeElement.getText());
    }

    return new StdArrangementRule(matcher, orderType);
  }

  @Nullable
  @Override
  public Element serialize(@NotNull ArrangementRule rule) {
    Element matcherElement = myMatcherSerializer.serialize(rule.getMatcher());
    if (matcherElement == null) {
      return null;
    }
    
    Element result = new Element(RULE_ELEMENT_NAME);
    result.addContent(new Element(MATCHER_ELEMENT_NAME).addContent(matcherElement));
    if (rule.getOrderType() != ArrangementRule.DEFAULT_ORDER_TYPE) {
      result.addContent(new Element(ORDER_TYPE_ELEMENT_NAME).setText(rule.getOrderType().toString()));
    }
    return result;
  }
}
