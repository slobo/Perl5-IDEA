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

package com.perl5.lang.perl.idea.intellilang;

import com.intellij.psi.ElementManipulators;
import com.intellij.psi.InjectedLanguagePlaces;
import com.intellij.psi.LanguageInjector;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.perl5.lang.perl.PerlLanguage;
import org.jetbrains.annotations.NotNull;

/**
 * Created by hurricup on 01.08.2016.
 */
public class PerlYamlAnnotationsInjector implements LanguageInjector
{
	static Class<?> ourScalarListClass;

	static
	{
		try
		{
			ourScalarListClass = Class.forName("org.jetbrains.yaml.psi.impl.YAMLScalarListImpl", true, PerlYamlAnnotationsInjector.class.getClassLoader());
			if (!PsiLanguageInjectionHost.class.isAssignableFrom(ourScalarListClass))
			{
				ourScalarListClass = null;
			}
		}
		catch (ClassNotFoundException ignored)
		{
		}
	}


	@Override
	public void getLanguagesToInject(@NotNull PsiLanguageInjectionHost host, @NotNull InjectedLanguagePlaces injectionPlacesRegistrar)
	{
		if (host.getClass() != ourScalarListClass)
		{
			return;
		}

		injectionPlacesRegistrar.addPlace(PerlLanguage.INSTANCE, ElementManipulators.getManipulator(host).getRangeInElement(host), null, null);
	}
}
