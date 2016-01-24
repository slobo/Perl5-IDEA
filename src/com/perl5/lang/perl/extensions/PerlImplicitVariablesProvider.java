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

package com.perl5.lang.perl.extensions;

import com.intellij.psi.PsiElement;
import com.perl5.lang.perl.psi.PerlVariableDeclarationWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by hurricup on 08.01.2016.
 * This interfact must be implemented in psi elements, which providing implicit variables declarations
 */
public interface PerlImplicitVariablesProvider extends PsiElement
{
	/**
	 * Returns plain list of full-qualified variable names sigil and name
	 *
	 * @return variable names
	 */
	@NotNull
	List<PerlVariableDeclarationWrapper> getImplicitVariables();
}