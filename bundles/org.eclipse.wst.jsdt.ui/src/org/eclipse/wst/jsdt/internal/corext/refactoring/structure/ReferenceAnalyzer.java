/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.structure;

import org.eclipse.wst.jsdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.EnumDeclaration;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IMethodBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.ImportDeclaration;
import org.eclipse.wst.jsdt.core.dom.MemberRef;
import org.eclipse.wst.jsdt.core.dom.MethodDeclaration;
import org.eclipse.wst.jsdt.core.dom.MethodInvocation;
import org.eclipse.wst.jsdt.core.dom.MethodRef;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;

/**
 * Updates references to moved static members.
 * Accepts <code>CompilationUnit</code>s.
 */
/* package */ class ReferenceAnalyzer extends MoveStaticMemberAnalyzer {
	
	public ReferenceAnalyzer(CompilationUnitRewrite cuRewrite, IBinding[] members, ITypeBinding target, ITypeBinding source) {
		super(cuRewrite, members, source, target);
	}
	
	public boolean needsTargetImport() {
		return fNeedsImport;
	}
	
	//---- Moved members are handled by the MovedMemberAnalyzer --------------

	public boolean visit(TypeDeclaration node) {
		ITypeBinding binding= node.resolveBinding();
		if (binding != null) {
			binding= binding.getTypeDeclaration();
			if (isMovedMember(binding))
				return false;
		}
		return super.visit(node);
	}
	
	public boolean visit(VariableDeclarationFragment node) {
		if (isMovedMember(node.resolveBinding()))
			return false;
		return super.visit(node);
	}
	
	public boolean visit(FieldDeclaration node) {
		//see bug 42383: multiple VariableDeclarationFragments not supported:
		VariableDeclarationFragment singleFragment= (VariableDeclarationFragment) node.fragments().get(0);
		if (isMovedMember(singleFragment.resolveBinding()))
			return false; // don't update javadoc of moved field here
		return super.visit(node);
	}
	
	public boolean visit(MethodDeclaration node) {
		if (isMovedMember(node.resolveBinding()))
			return false;
		return super.visit(node);
	}
	
	
	//---- types and fields --------------------------
		
	public boolean visit(SimpleName node) {
		if (! node.isDeclaration() && isMovedMember(node.resolveBinding()) && ! isProcessed(node))
			rewrite(node, fTarget);
		return false;
	}
	
	public boolean visit(QualifiedName node) {
		if (isMovedMember(node.resolveBinding())) {
			if (node.getParent() instanceof ImportDeclaration) {
				ITypeBinding typeBinding= node.resolveTypeBinding();
				if (typeBinding != null) 
				 	fCuRewrite.getImportRewrite().removeImport(typeBinding.getQualifiedName());
				String imp= fCuRewrite.getImportRewrite().addImport(fTarget.getQualifiedName() + '.' + node.getName().getIdentifier());
				fCuRewrite.getImportRemover().registerAddedImport(imp);
			} else {
				rewrite(node, fTarget);
			}
			return false;
		} else {
			return super.visit(node);
		}
	}
	
	public boolean visit(FieldAccess node) {
		if (isMovedMember(node.resolveFieldBinding()))
			rewrite(node, fTarget);
		return super.visit(node);
	}
	
	//---- method invocations ----------------------------------
	
	public boolean visit(MethodInvocation node) {
		IMethodBinding binding= node.resolveMethodBinding();
		if (binding != null) {
			binding= binding.getMethodDeclaration();
			if (isMovedMember(binding))
				rewrite(node, fTarget);
		}
		return super.visit(node);
	}
	
	//---- javadoc references ----------------------------------
	
	public boolean visit(MemberRef node) {
		if (isMovedMember(node.resolveBinding()))
			rewrite(node, fTarget);
		return false;
	}
	
	public boolean visit(MethodRef node) {
		if (isMovedMember(node.resolveBinding()))
			rewrite(node, fTarget);
		return false;
	}

	public boolean visit(AnnotationTypeDeclaration node) {
		if (isMovedMember(node.resolveBinding()))
			return false;
		return super.visit(node);
	}

	public boolean visit(EnumDeclaration node) {
		if (isMovedMember(node.resolveBinding()))
			return false;
		return super.visit(node);
	}
}