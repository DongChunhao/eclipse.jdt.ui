/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Body moved from org.eclipse.jdt.internal.ui.text.correction.proposals.NewAnnotationMemberProposal
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;

import org.eclipse.jdt.internal.ui.text.correction.ModifierCorrectionSubProcessorCore;

public class NewAnnotationMemberProposalCore extends LinkedCorrectionProposalCore {
	private static final String KEY_NAME= "name"; //$NON-NLS-1$
	private static final String KEY_TYPE= "type"; //$NON-NLS-1$

	private final ASTNode fInvocationNode;
	private final ITypeBinding fSenderBinding;

	public NewAnnotationMemberProposalCore(String label, ICompilationUnit targetCU, ASTNode invocationNode, ITypeBinding binding, int relevance) {
		super(label, targetCU, null, relevance);
		fInvocationNode= invocationNode;
		fSenderBinding= binding;
	}

	@Override
	protected ASTRewrite getRewrite() throws CoreException {
		CompilationUnit astRoot= ASTResolving.findParentCompilationUnit(fInvocationNode);
		ASTNode typeDecl= astRoot.findDeclaringNode(fSenderBinding);
		ASTNode newTypeDecl= null;
		if (typeDecl != null) {
			newTypeDecl= typeDecl;
		} else {
			astRoot= ASTResolving.createQuickFixAST(getCompilationUnit(), null);
			newTypeDecl= astRoot.findDeclaringNode(fSenderBinding.getKey());
		}
		createImportRewrite(astRoot);

		if (newTypeDecl instanceof AnnotationTypeDeclaration) {
			AnnotationTypeDeclaration newAnnotationTypeDecl= (AnnotationTypeDeclaration) newTypeDecl;

			ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());

			AnnotationTypeMemberDeclaration newStub= getStub(rewrite, newAnnotationTypeDecl);

			List<BodyDeclaration> members= newAnnotationTypeDecl.bodyDeclarations();
			int insertIndex= members.size();

			ListRewrite listRewriter= rewrite.getListRewrite(newAnnotationTypeDecl, AnnotationTypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			listRewriter.insertAt(newStub, insertIndex, null);

			return rewrite;
		}
		return null;
	}

	private AnnotationTypeMemberDeclaration getStub(ASTRewrite rewrite, AnnotationTypeDeclaration targetTypeDecl) {
		AST ast= targetTypeDecl.getAST();

		AnnotationTypeMemberDeclaration decl= ast.newAnnotationTypeMemberDeclaration();

		SimpleName newNameNode= getNewName(rewrite);

		decl.modifiers().addAll(ASTNodeFactory.newModifiers(ast, evaluateModifiers(targetTypeDecl)));

		ModifierCorrectionSubProcessorCore.installLinkedVisibilityProposals(getLinkedProposalModel(), rewrite, decl.modifiers(), true);

		decl.setName(newNameNode);

		Type returnType= getNewType(rewrite);
		decl.setType(returnType);
		return decl;
	}

	private Type getNewType(ASTRewrite rewrite) {
		AST ast= rewrite.getAST();
		Type newTypeNode= null;
		ITypeBinding binding= null;
		if (fInvocationNode.getLocationInParent() == MemberValuePair.NAME_PROPERTY) {
			Expression value= ((MemberValuePair) fInvocationNode.getParent()).getValue();
			binding= value.resolveTypeBinding();
		} else if (fInvocationNode instanceof Expression) {
			binding= ((Expression) fInvocationNode).resolveTypeBinding();
		}
		if (binding != null) {
			ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(fInvocationNode, getImportRewrite());
			newTypeNode= getImportRewrite().addImport(binding, ast, importRewriteContext, TypeLocation.RETURN_TYPE);
		}
		if (newTypeNode == null) {
			newTypeNode= ast.newSimpleType(ast.newSimpleName("String")); //$NON-NLS-1$
		}
		addLinkedPosition(rewrite.track(newTypeNode), false, KEY_TYPE);
		return newTypeNode;
	}

	private int evaluateModifiers(AnnotationTypeDeclaration targetTypeDecl) {
		List<BodyDeclaration> methodDecls= targetTypeDecl.bodyDeclarations();
		for (BodyDeclaration curr : methodDecls) {
			if (curr instanceof AnnotationTypeMemberDeclaration) {
				return ((AnnotationTypeMemberDeclaration) curr).getModifiers();
			}
		}
		return 0;
	}

	private SimpleName getNewName(ASTRewrite rewrite) {
		AST ast= rewrite.getAST();
		String name;
		if (fInvocationNode.getLocationInParent() == MemberValuePair.NAME_PROPERTY) {
			name= ((SimpleName) fInvocationNode).getIdentifier();
			if (ast == fInvocationNode.getAST()) {
				addLinkedPosition(rewrite.track(fInvocationNode), true, KEY_NAME);
			}
		} else {
			name= "value"; //$NON-NLS-1$
		}


		SimpleName newNameNode= ast.newSimpleName(name);
		addLinkedPosition(rewrite.track(newNameNode), false, KEY_NAME);
		return newNameNode;
	}

}