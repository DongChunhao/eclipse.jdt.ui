/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java.hover;

import java.util.Properties;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

import org.eclipse.ui.IEditorInput;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.StringLiteral;

import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.nls.AccessorClassReference;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSHintHelper;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditor;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.HTMLPrinter;

/**
 * Provides externalized string as hover info for NLS key.
 * 
 * @since 3.1
 */
public class NLSStringHover extends AbstractJavaEditorTextHover {

	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.hover.AbstractJavaEditorTextHover#getHoverRegion(org.eclipse.jface.text.ITextViewer, int)
	 */
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		if (!(getEditor() instanceof JavaEditor))
			return null;

		IJavaElement je= getEditorInputJavaElement();
		if (je == null)
			return null;
		
		// Never wait for an AST in UI thread.
		CompilationUnit ast= JavaPlugin.getDefault().getASTProvider().getAST(je, ASTProvider.WAIT_NO, null);

		ASTNode node= NodeFinder.perform(ast, offset, 1);
		if (!(node instanceof StringLiteral))
			return null;
		StringLiteral stringLiteral= (StringLiteral)node;
		
		return new Region(stringLiteral.getStartPosition(), stringLiteral.getLength());
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.hover.AbstractJavaEditorTextHover#getHoverInfo(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		if (!(getEditor() instanceof JavaEditor))
			return null;

		IJavaElement je= getEditorInputJavaElement();
		if (je == null)
			return null;
		
		CompilationUnit ast= JavaPlugin.getDefault().getASTProvider().getAST(je, ASTProvider.WAIT_ACTIVE_ONLY, null);

		ASTNode node= NodeFinder.perform(ast, hoverRegion.getOffset(), hoverRegion.getLength());
		if (!(node instanceof StringLiteral))
			return null;
		StringLiteral stringLiteral= (StringLiteral)node;
		
		AccessorClassReference ref= NLSHintHelper.getAccessorClassReference(ast, hoverRegion);
		if (ref == null)
			return null;
		
		Properties properties= NLSHintHelper.getProperties(je.getJavaProject(), ref.getBinding());
		
		if (properties.isEmpty())
			return null;
		
		String value= properties.getProperty(stringLiteral.getLiteralValue(), null);
		if (value != null)
			value= HTMLPrinter.convertToHTMLContent(value);
		else
			value= JavaHoverMessages.getString("NLSStringHover.NLSStringHover.missingKeyWarning"); //$NON-NLS-1$
		
		return toHtml(value);
	}
	
	private String toHtml(String string) {
		
		StringBuffer buffer= new StringBuffer();
		
		HTMLPrinter.addSmallHeader(buffer, JavaHoverMessages.getString("NLSStringHover.header")); //$NON-NLS-1$
		HTMLPrinter.addParagraph(buffer, string);
		HTMLPrinter.insertPageProlog(buffer, 0);
		HTMLPrinter.addPageEpilog(buffer);
		return buffer.toString();
	}

	private IJavaElement getEditorInputJavaElement() {
		if (getEditor() instanceof CompilationUnitEditor)
			return JavaPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(getEditor().getEditorInput());
		else if (getEditor() instanceof ClassFileEditor) {
			IEditorInput editorInput= getEditor().getEditorInput(); 
			if (editorInput instanceof IClassFileEditorInput)
				return ((IClassFileEditorInput)editorInput).getClassFile();
			
		}
		return null;
	}
	
}
