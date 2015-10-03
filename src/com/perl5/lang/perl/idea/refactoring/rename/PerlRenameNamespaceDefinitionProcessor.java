/*
 * Copyright 2015 Alexandr Evstigneev
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

package com.perl5.lang.perl.idea.refactoring.rename;

import com.intellij.ide.projectView.impl.nodes.PackageUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;
import com.perl5.lang.perl.psi.PerlNamespaceDefinition;
import com.perl5.lang.perl.psi.PerlNamespaceElement;
import com.perl5.lang.perl.psi.impl.PerlFileImpl;
import com.perl5.lang.perl.psi.properties.PerlNamedElement;
import com.perl5.lang.perl.psi.utils.PerlElementFactory;
import com.perl5.lang.perl.util.PerlPackageUtil;
import com.perl5.lang.perl.util.PerlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by hurricup on 29.05.2015.
 */
public class PerlRenameNamespaceDefinitionProcessor extends PerlRenamePolyReferencedElementProcessor
{
	@Override
	public boolean canProcessElement(@NotNull PsiElement element)
	{
		return element instanceof PerlNamespaceDefinition;
	}

	public void prepareDependentFiles(PerlNamespaceDefinition namespaceDefinition, String newName, Map<PsiElement, String> allRenames)
	{
		PsiFile file = namespaceDefinition.getContainingFile();

		String currentPackageName = namespaceDefinition.getName();
		assert currentPackageName != null;
		List<String> currentPackageChunks = Arrays.asList(currentPackageName.split("::"));
		assert currentPackageChunks.size() > 0;
		String currentFileName = currentPackageChunks.get(currentPackageChunks.size() - 1) + ".pm";

		if (currentFileName.equals(file.getName()) && !allRenames.containsKey(file))
		{
			String currentPackageRelativePath = PerlPackageUtil.getPackagePathByName(currentPackageName);
			VirtualFile virtualFile = file.getVirtualFile();

			if (virtualFile != null && virtualFile.getPath().endsWith(currentPackageRelativePath))    // we suppose this is enough
			{
				allRenames.put(file, newName);
				System.err.println("Processing " + virtualFile.getPath() + " psi file name: " + file.getName());
			} else
			{
				System.err.println("Incorrect path " + virtualFile.getPath() + " psi file name: " + file.getName());
			}
		}
	}

	@Override
	public void prepareRenaming(PsiElement element, String newName, Map<PsiElement, String> allRenames, SearchScope scope)
	{
		preparePsiElementRenaming(element, newName, allRenames);
		super.prepareRenaming(element, newName, allRenames, scope);
	}

	@Override
	public void preparePsiElementRenaming(PsiElement element, String newBaseName, Map<PsiElement, String> allRenames)
	{
		super.preparePsiElementRenaming(element, newBaseName, allRenames);
		if (element instanceof PerlNamespaceDefinition)
		{
			prepareDependentFiles((PerlNamespaceDefinition) element, newBaseName, allRenames);
		}
	}

	@Nullable
	@Override
	public Runnable getPostRenameCallback(PsiElement element, String newName, RefactoringElementListener elementListener)
	{
		System.err.println("Getting postrename callback");
		return super.getPostRenameCallback(element, newName, elementListener);
	}

/*
	@Nullable
	@Override
	public Runnable getPostRenameCallback(final PsiElement element, String newName, RefactoringElementListener elementListener)
	{
		Runnable newProcess = null;

		if (element instanceof PerlNamespaceDefinition || element.getParent() instanceof PerlNamespaceDefinition)
		{
			final PsiFile psiFile = element.getContainingFile();
			String currentPackageName = ((PerlNamedElement) element).getName();
			final String newPackageName = PerlPackageUtil.getCanonicalPackageName(newName);

			if (psiFile instanceof PerlFileImpl)
			{
				final String currentFilePackageName = ((PerlFileImpl) psiFile).getFilePackageName();

				if (currentFilePackageName != null && currentFilePackageName.equals(currentPackageName) && !newPackageName.equals(currentPackageName))
				{
					// todo we need to check if this is a first namespace definition in the file
					// ok, it's package with same name and we've got a new name
					final VirtualFile virtualFile = psiFile.getVirtualFile();
					final Project project = element.getProject();

					final RenameRefactoringQueue queue = new RenameRefactoringQueue(project);

					for (PsiReference inboundReference : ReferencesSearch.search(psiFile))
					{
						if (inboundReference.getElement() instanceof PerlNamespaceElement)
							queue.addElement(inboundReference.getElement(), newPackageName);
					}

					newProcess = new Runnable()
					{
						@Override
						public void run()
						{
							// looking/creating new path for a file
							VirtualFile newParent = PerlUtil.getFileClassRoot(project, virtualFile);
							VirtualFile currentParent = virtualFile.getParent();

							List<String> packageDirs = Arrays.asList(newPackageName.split(":+"));
							String newFileName = packageDirs.get(packageDirs.size() - 1) + ".pm";

							for (int i = 0; i < packageDirs.size() - 1; i++)
							{
								String dir = packageDirs.get(i);

								VirtualFile subDir = newParent.findChild(dir);
								try
								{
									newParent = (subDir != null) ? subDir : newParent.createChildDirectory(null, dir);
								} catch (IOException e)
								{
									throw new IncorrectOperationException("Could not create subdirectory: " + newParent.getPath() + "/" + dir);
								}
							}

							try
							{
								if (!newParent.getPath().equals(currentParent.getPath()))
								{
									// we need to handle references ourselves
									virtualFile.move(element, newParent);
								}

								virtualFile.rename(element, newFileName);
							} catch (IOException e)
							{
								throw new IncorrectOperationException("Could not rename or move package file: " + e.getMessage());
							}

							queue.run();
						}
					};
				}
			}
		}

		return newProcess;
	}

*/
}
