/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.aelements

import org.jetbrains.kotlin.com.intellij.psi.PsiAnnotation
import org.jetbrains.kotlin.com.intellij.psi.PsiArrayAccessExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiAssignmentExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiBinaryExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiBlockStatement
import org.jetbrains.kotlin.com.intellij.psi.PsiClass
import org.jetbrains.kotlin.com.intellij.psi.PsiClassObjectAccessExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiClassType
import org.jetbrains.kotlin.com.intellij.psi.PsiCodeBlock
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiExpressionList
import org.jetbrains.kotlin.com.intellij.psi.PsiField
import org.jetbrains.kotlin.com.intellij.psi.PsiFile
import org.jetbrains.kotlin.com.intellij.psi.PsiIfStatement
import org.jetbrains.kotlin.com.intellij.psi.PsiImportStatementBase
import org.jetbrains.kotlin.com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.com.intellij.psi.PsiJvmModifiersOwner
import org.jetbrains.kotlin.com.intellij.psi.PsiLambdaExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiLocalVariable
import org.jetbrains.kotlin.com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.com.intellij.psi.PsiMethodCallExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiPackageStatement
import org.jetbrains.kotlin.com.intellij.psi.PsiParameter
import org.jetbrains.kotlin.com.intellij.psi.PsiParameterList
import org.jetbrains.kotlin.com.intellij.psi.PsiReferenceExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiReferenceParameterList
import org.jetbrains.kotlin.com.intellij.psi.PsiStatement
import org.jetbrains.kotlin.com.intellij.psi.PsiTypeElement
import org.jetbrains.kotlin.com.intellij.psi.PsiTypeParameter
import org.jetbrains.kotlin.com.intellij.psi.PsiVariable
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtTypeConstraint
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtValueArgumentList

/** Converts a Psi Element to the most specific corresponding AElement. */
fun PsiElement.toAElement(): AElement =
    when (this) {
      is KtFile -> toAElement()
      is PsiJavaFile -> toAElement()
      is PsiClass -> toAElement()
      is KtClassOrObject -> toAElement()
      is PsiPackageStatement -> toAElement()
      is KtPackageDirective -> toAElement()
      is PsiImportStatementBase -> toAElement()
      is KtImportDirective -> toAElement()
      is PsiMethod -> toAElement()
      is KtConstructor<*> -> toAElement()
      is KtNamedFunction -> toAElement()
      is KtPropertyAccessor -> toAElement()
      is PsiAnnotation -> toAElement()
      is KtAnnotationEntry -> toAElement()
      is PsiExpressionList -> toAElement()
      is KtValueArgumentList -> toAElement()
      is PsiParameterList -> toAElement()
      is KtParameterList -> toAElement()
      is PsiParameter -> toAElement()
      is KtParameter -> toAElement()
      is PsiField -> toAElement()
      is PsiVariable -> toAElement()
      is KtProperty -> toAElement()
      is PsiReferenceParameterList -> toAElement()
      is KtTypeArgumentList -> toAElement()
      is PsiCodeBlock -> toAElement()
      is KtQualifiedExpression -> toAElement()
      is PsiMethodCallExpression -> toAElement()
      is PsiReferenceExpression -> toAElement()
      is KtCallExpression -> toAElement()
      is PsiBlockStatement -> toAElement()
      is KtBlockExpression -> toAElement()
      is PsiBinaryExpression -> toAElement()
      is KtArrayAccessExpression -> toAElement()
      is PsiArrayAccessExpression -> toAElement()
      is PsiAssignmentExpression -> toAElement()
      is KtBinaryExpression -> toAElement()
      is PsiIfStatement -> toAElement()
      is KtIfExpression -> toAElement()
      is PsiLambdaExpression -> toAElement()
      is KtLambdaExpression -> toAElement()
      is KtClassLiteralExpression -> toAElement()
      is PsiClassObjectAccessExpression -> toAElement()
      is KtExpression -> toAElement()
      is PsiExpression -> toAElement()
      is PsiStatement -> toAElement()
      is PsiTypeElement -> toAElement()
      is KtTypeReference -> toAElement()
      is PsiJvmModifiersOwner -> toAElement()
      is KtAnnotated -> toAElement()
      else -> AElementImpl(this)
    }

fun KtFile.toAElement() = AFile(this)

fun PsiFile.toAElement() = AFile(this)

fun PsiJavaFile.toAElement() = AFile(this)

fun PsiClass.toAElement() = AClassOrObject(this)

fun KtClassOrObject.toAElement() = AClassOrObject(this)

fun PsiPackageStatement.toAElement() = APackageDirective(this)

fun KtPackageDirective.toAElement() = APackageDirective(this)

fun PsiImportStatementBase.toAElement() = AImportDirective(this)

fun KtImportDirective.toAElement() = AImportDirective(this)

fun PsiMethod.toAElement(): ADeclarationWithBody =
    if (isConstructor) AConstructor(this) else ANamedFunction(this)

fun KtConstructor<*>.toAElement() = AConstructor(this)

fun KtNamedFunction.toAElement() = ANamedFunction(this)

/**
 * avoid making this into a ADeclarationWithBody to avoid a case where KtLambdaExpression contains a
 * KtFunctionLiteral which has the same fields, but is inconistent with Java PsiLambdaExpression
 */
fun KtFunctionLiteral.toAElement() = AElementImpl(this)

fun PsiAnnotation.toAElement() = AAnnotation(this)

fun KtAnnotationEntry.toAElement() = AAnnotation(this)

fun PsiExpressionList.toAElement() = AValueArgumentList(this)

fun KtValueArgumentList.toAElement() = AValueArgumentList(this)

fun PsiParameterList.toAElement() = AParameterList(this)

fun KtParameterList.toAElement() = AParameterList(this)

fun PsiParameter.toAElement() = AParameter(this)

fun KtParameter.toAElement() = AParameter(this)

fun PsiField.toAElement() = AMemberProperty(this)

fun PsiVariable.toAElement() =
    when (this) {
      is PsiLocalVariable -> toAElement()
      else -> AMemberProperty(this)
    }

fun PsiLocalVariable.toAElement() = ALocalProperty(this)

fun KtProperty.toAElement(): AProperty =
    if (isLocal) ALocalProperty(this) else AMemberProperty(this)

fun PsiReferenceParameterList.toAElement(): ATypeArgumentList = ATypeArgumentList(this)

fun KtTypeArgumentList.toAElement(): ATypeArgumentList = ATypeArgumentList(this)

fun PsiCodeBlock.toAElement() = ACodeBlockImpl(this)

fun KtQualifiedExpression.toAElement() =
    if (selectorExpression is KtCallExpression) AQualifiedCallExpression(this)
    else AQualifiedExpressionImpl(this)

fun PsiMethodCallExpression.toAElement() =
    if (methodExpression.qualifierExpression != null) AQualifiedCallExpression(this)
    else ACallExpressionImpl(this)

fun PsiReferenceExpression.toAElement(): AExpression =
    if (qualifierExpression != null) AQualifiedExpressionImpl(this) else AExpressionImpl(this)

fun KtCallExpression.toAElement() = ACallExpressionImpl(this)

fun PsiBlockStatement.toAElement() = ACodeBlockExpressionOrStatement(this)

fun KtBlockExpression.toAElement() = ACodeBlockExpressionOrStatement(this)

fun PsiBinaryExpression.toAElement() = ABinaryExpression(this)

fun PsiAssignmentExpression.toAElement() = AAssignmentExpression(this)

fun KtArrayAccessExpression.toAElement() = AArrayAccessExpression(this)

fun PsiArrayAccessExpression.toAElement() = AArrayAccessExpression(this)

fun KtBinaryExpression.toAElement() =
    if (operationReference.text == "=") AAssignmentExpression(this) else ABinaryExpression(this)

fun PsiIfStatement.toAElement() = AIfExpressionOrStatement(this)

fun KtIfExpression.toAElement() = AIfExpressionOrStatement(this)

fun PsiLambdaExpression.toAElement() = ALambdaExpression(this)

fun KtLambdaExpression.toAElement() = ALambdaExpression(this)

fun KtDeclarationWithBody.toAElement(): ADeclarationWithBody =
    when (this) {
      is KtNamedFunction -> toAElement()
      is KtConstructor<*> -> toAElement()
      is KtPropertyAccessor -> toAElement()
      is KtFunctionLiteral -> ADeclarationWithBodyImpl(this)
      else -> error("Unexpected type ${this::class.java.simpleName}")
    }

fun KtPropertyAccessor.toAElement() = APropertyAccessor(this)

fun KtTypeConstraint.toAElement() = ATypeConstraint(this)

fun KtExpression.toAElement(): AExpressionOrStatement =
    when (this) {
      is KtQualifiedExpression -> toAElement()
      is KtCallExpression -> toAElement()
      is KtBinaryExpression -> toAElement()
      is KtIfExpression -> toAElement()
      is KtLambdaExpression -> toAElement()
      else -> AExpressionImpl(this)
    }

fun PsiExpression.toAElement(): AExpression =
    when (this) {
      is PsiMethodCallExpression -> toAElement()
      is PsiReferenceExpression -> toAElement()
      is PsiBinaryExpression -> toAElement()
      is PsiLambdaExpression -> toAElement()
      else -> AExpressionImpl(this)
    }

fun PsiStatement.toAElement(): AExpressionOrStatement = AExpressionOrStatementImpl(this)

fun PsiJvmModifiersOwner.toAElement() =
    when (this) {
      is PsiClass -> toAElement()
      is PsiMethod -> toAElement()
      is PsiField -> toAElement()
      is PsiParameter -> toAElement()
      is PsiLocalVariable -> (this as PsiLocalVariable).toAElement()
      else -> AAnnotated(this)
    }

fun KtClassLiteralExpression.toAElement() = AClassLiteralExpression(this)

fun PsiClassObjectAccessExpression.toAElement() = AClassLiteralExpression(this)

fun PsiTypeElement.toAElement() = ATypeReference(this)

fun PsiClassType.toAElement() = ATypeReference(this.psiContext!!)

fun KtTypeReference.toAElement() = ATypeReference(this)

fun PsiTypeParameter.toAElement() = ATypeParameter(this)

fun KtTypeParameter.toAElement() = ATypeParameter(this)

fun KtAnnotated.toAElement() =
    when (this) {
      is KtClassOrObject -> toAElement()
      is KtNamedFunction -> toAElement()
      is KtProperty -> toAElement()
      is KtParameter -> toAElement()
      is KtConstructor<*> -> toAElement()
      else -> AAnnotated(this)
    }
