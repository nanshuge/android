// This is a generated file. Not intended for manual editing.
package com.android.tools.idea.lang.db3.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.android.tools.idea.lang.db3.psi.DbTokenTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.android.tools.idea.lang.db3.psi.*;

public class PsiDbExprImpl extends ASTWrapperPsiElement implements PsiDbExpr {

  public PsiDbExprImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PsiDbVisitor) ((PsiDbVisitor)visitor).visitExpr(this);
    else super.accept(visitor);
  }

}
