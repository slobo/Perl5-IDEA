/*
 * Copyright 2016 Alexandr Evstigneev
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

package com.perl5.lang.tt2.utils;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.perl5.lang.tt2.TemplateToolkitParserDefinition;
import org.jetbrains.annotations.Nullable;

/**
 * Created by hurricup on 12.06.2016.
 */
public class TemplateToolkitPsiUtils
{
	@Nullable
	public static PsiElement getPrevSignificantSibling(PsiElement element)
	{
		PsiElement result = element.getPrevSibling();
		while (true)
		{
			if (result == null)
			{
				break;
			}

			IElementType tokenType = result.getNode().getElementType();

			if (!TemplateToolkitParserDefinition.WHITESPACES_AND_COMMENTS.contains(tokenType))
			{
				break;
			}
			result = result.getPrevSibling();
		}
		return result;
	}

}
