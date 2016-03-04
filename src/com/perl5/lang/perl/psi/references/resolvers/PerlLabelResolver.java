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

package com.perl5.lang.perl.psi.references.resolvers;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.intellij.util.Processor;
import com.perl5.lang.perl.psi.PerlLabel;
import com.perl5.lang.perl.psi.PerlLabelDeclaration;
import com.perl5.lang.perl.psi.PsiPerlGotoExpr;
import com.perl5.lang.perl.psi.references.PerlLabelReference;
import com.perl5.lang.perl.psi.utils.PerlPsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by hurricup on 04.03.2016.
 */
public class PerlLabelResolver implements ResolveCache.AbstractResolver<PerlLabelReference, PerlLabelDeclaration>
{
	@Nullable
	@Override
	public PerlLabelDeclaration resolve(@NotNull PerlLabelReference perlLabelReference, boolean incompleteCode)
	{
		PerlLabel label = perlLabelReference.getElement();
		PsiElement parent = label == null ? null : label.getParent();

		if (label == null || parent instanceof PerlLabelDeclaration)
			return null;

		LabelSeeker processor = new LabelSeeker(label.getText());

		if (parent instanceof PsiPerlGotoExpr) // goto
		{
			PerlPsiUtil.processGotoLabelDeclarations(label.getParent(), processor);
		}
		else // suppose it's last, next or redo
		{
			PerlPsiUtil.processNextRedoLastLabelDeclarations(label.getParent(), processor);
		}
		return processor.getResult();
	}

	protected static class LabelSeeker implements Processor<PerlLabelDeclaration>
	{
		protected final String myName;
		protected PerlLabelDeclaration myResult = null;

		public LabelSeeker(@NotNull String myName)
		{
			this.myName = myName;
		}

		@Override
		public boolean process(PerlLabelDeclaration perlLabelDeclaration)
		{
			if (StringUtil.equals(perlLabelDeclaration.getName(), myName))
			{
				myResult = perlLabelDeclaration;
				return false;
			}
			return true;
		}

		public PerlLabelDeclaration getResult()
		{
			return myResult;
		}
	}
}
