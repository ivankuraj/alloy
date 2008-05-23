/*
 * Alloy Analyzer 4 -- Copyright (c) 2006-2008, Felix Chang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.mit.csail.sdg.alloy4compiler.translator;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.alloy4compiler.ast.Expr;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprBinary;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprBuiltin;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprCall;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprConstant;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprITE;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprLet;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprQuant;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprUnary;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprVar;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig;
import edu.mit.csail.sdg.alloy4compiler.ast.VisitReturn;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig.Field;

/**
 * Immutable; this class rearranges the AST to promote as many clauses up to the top level as possible
 * (in order to get better precision unsat core results)
 */

final class ConvToConjunction extends VisitReturn<Expr> {

    /** {@inheritDoc} */
    @Override public Expr visit(ExprBinary x) throws Err {
        if (x.op == ExprBinary.Op.AND) {
            Expr a = visitThis(x.left);
            Expr b = visitThis(x.right);
            return a.and(b);
        }
        return x;
    }

    /** {@inheritDoc} */
    @Override public Expr visit(ExprQuant x) throws Err {
        if (x.op == ExprQuant.Op.ALL) {
            Expr s = x.sub;
            while(s instanceof ExprUnary && ((ExprUnary)s).op==ExprUnary.Op.NOOP) s=((ExprUnary)s).sub;
            if (s instanceof ExprBinary && ((ExprBinary)s).op==ExprBinary.Op.AND) {
                Expr a = visitThis(x.op.make(Pos.UNKNOWN, Pos.UNKNOWN, x.vars, ((ExprBinary)s).left));
                Expr b = visitThis(x.op.make(Pos.UNKNOWN, Pos.UNKNOWN, x.vars, ((ExprBinary)s).right));
                return a.and(b);
            }
        }
        return x;
    }

    /** {@inheritDoc} */
    @Override public Expr visit(ExprUnary x) throws Err {
        if (x.op == ExprUnary.Op.NOOP) {
            return visitThis(x.sub);
        }
        if (x.op == ExprUnary.Op.NOT && x.sub instanceof ExprBinary) {
            ExprBinary bin = (ExprBinary)(x.sub);
            if (bin.op == ExprBinary.Op.OR) {
                Expr a = visitThis(bin.left.not());
                Expr b = visitThis(bin.right.not());
                return a.and(b);
            }
        }
        return x;
    }

    /** {@inheritDoc} */
    @Override public Expr visit(ExprBuiltin x) { return x; }

    /** {@inheritDoc} */
    @Override public Expr visit(ExprCall x) { return x; }

    /** {@inheritDoc} */
    @Override public Expr visit(ExprConstant x) { return x; }

    /** {@inheritDoc} */
    @Override public Expr visit(ExprITE x) { return x; }

    /** {@inheritDoc} */
    @Override public Expr visit(ExprLet x) { return x; }

    /** {@inheritDoc} */
    @Override public Expr visit(ExprVar x) { return x; }

    /** {@inheritDoc} */
    @Override public Expr visit(Sig x) { return x; }

    /** {@inheritDoc} */
    @Override public Expr visit(Field x) { return x; }
}
