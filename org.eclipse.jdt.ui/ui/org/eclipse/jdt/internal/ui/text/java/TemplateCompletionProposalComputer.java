/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.TemplateContextType;

import org.eclipse.jdt.internal.corext.template.java.JavaContextType;
import org.eclipse.jdt.internal.corext.template.java.JavaDocContextType;

import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.ui.text.template.contentassist.TemplateEngine;


/**
 * Computer computing template proposals for Java and Javadoc context type.
 * 
 * @since 3.2
 */
public class TemplateCompletionProposalComputer extends AbstractTemplateCompletionProposalComputer {

	private final TemplateEngine fJavaTemplateEngine;
	private final TemplateEngine fJavadocTemplateEngine;

	public TemplateCompletionProposalComputer() {
		ContextTypeRegistry templateContextRegistry= JavaPlugin.getDefault().getTemplateContextRegistry();
		TemplateContextType contextType= templateContextRegistry.getContextType(JavaContextType.NAME);
		Assert.isNotNull(contextType);
		fJavaTemplateEngine= new TemplateEngine(contextType);

		contextType= templateContextRegistry.getContextType(JavaDocContextType.NAME);
		Assert.isNotNull(contextType);
		fJavadocTemplateEngine= new TemplateEngine(contextType);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.java.TemplateCompletionProposalComputer#computeCompletionEngine(org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext)
	 */
	protected TemplateEngine computeCompletionEngine(JavaContentAssistInvocationContext context) {
		try {
			String partition= TextUtilities.getContentType(context.getDocument(), IJavaPartitions.JAVA_PARTITIONING, context.getInvocationOffset(), true);
			if (partition.equals(IJavaPartitions.JAVA_DOC))
				return fJavadocTemplateEngine;
			else
				return fJavaTemplateEngine;
		} catch (BadLocationException x) {
			return null;
		}
	}

}
