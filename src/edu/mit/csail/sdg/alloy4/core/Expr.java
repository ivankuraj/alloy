package edu.mit.csail.sdg.alloy4.core;

/**
 * Immutable; represents a formula or expression; subclasses must also be immutable.
 *
 * <p/> <b>Invariant:</b>  pos!=null
 *
 * <p/> <b>Invariant:</b>  mult==0 || mult==1 || mult==2
 *
 * @author Felix Chang
 */

public abstract class Expr {

    /** Accepts the return visitor. */
    public abstract Object accept(VisitReturn visitor);

    /** Accepts the typecheck visitor bottom-up. */
    public abstract Expr accept(VisitTypechecker visitor);

    /** Accepts the typecheck visitor top-down. */
    public abstract Expr accept(VisitTypechecker visitor, Type obj);

    /**
     * The filename, line, and column position
     * in the original Alloy model file (cannot be null).
     */
    public final Pos pos;

    /**
     * This field records the type for this node
     * (null if this node has not been typechecked).
     */
    public final Type type;

    /**
     * This field records whether the node is a multiplicity constraint.
     *
     * <br> If it's 2, that means it is a multiplicity constraint (X ?->? X),
     *      or has the form (A -> B) where A and/or B is a multiplicity constraint.
     *
     * <br> If it's 1, that means it is a multiplicity constraint of the form (? X)
     *
     * <br> If it's 0, that means it does not have either form.
     */
    public final int mult;

    /**
     * Constructs a new expression node
     *
     * @param pos - the original position in the file
     *
     * @param type - the type (null if this expression has not been typechecked)
     *
     * @param mult - the multiplicity
     * <br>If it's 2, that means it is a multiplicity constraint (X ?->? X),
     *     or has the form (A -> B) where A and/or B is a multiplicity constraint.
     * <br>If it's 1, that means it is a multiplicity constraint of the form (? X)
     * <br>If it's 0, that means it does not have either form.
     *
     * @throws ErrorInternal if pos==null, or mult is not one of {0,1,2}
     */
    public Expr (Pos pos, Type type, int mult) {
        this.pos=nonnull(pos);
        this.type=type;
        this.mult=mult;
        if (mult<0 || mult>2) throw internalError("Expr node's multiplicity must be 0, 1, or 2.");
    }

    /** Convenience method that constructs a syntax error exception. */
    public final ErrorSyntax syntaxError (String s) {
        return new ErrorSyntax(pos, s);
    }

    /** Convenience method that constructs a type error exception. */
    public final ErrorType typeError (String s) {
        return new ErrorType(pos, this, s);
    }

    /** Convenience method that constructs an internal error exception. */
    public final ErrorInternal internalError (String s) {
        return new ErrorInternal(pos, this, s);
    }

    /**
     * Convenience method that checks if x is null or not;
     * (it returns x if nonnull, and throws an exception if null).
     *
     * @return x if it is not null
     * @throws ErrorInternal if x is null
     */
    public final<T> T nonnull (T x) {
        if (x==null) throw internalError("NullPointerException"); else return x;
    }

    /**
     * Convenience method that returns true if and only if "this" has the form (:set X)
     * and X's arity is 1.
     *
     * @throws ErrorInternal if this node is not fully typechecked
     */
    public final boolean isSetOf1ary() {
        if (this.type==null)
            throw internalError("isSetOf1ary() cannot be called until typechecking is done");
        if (!(this instanceof ExprUnary)) return false;
        if (((ExprUnary)this).op!=ExprUnary.Op.SETMULT) return false;
        return this.type.arity()==1;
    }

    /**
     * Convenience method that
     * returns the subexpression of an ExprUnary expression.
     *
     * @return ((ExprUnary)this).sub if this node is an ExprUnary expression
     *
     * @throws ErrorInternal if this node is not an ExprUnary expression
     */
    public final Expr getUnarySub() {
        if (this instanceof ExprUnary) return ((ExprUnary)this).sub;
        throw internalError("getUnarySub() can be used only on an ExprUnary object!");
    }

    /**
     * Convenience method that
     * returns a typechecked node representing ("this in y")
     * <br/> Note: this node and y must both be fully typechecked.
     *
     * @throws ErrorInternal if this node and y are not both fully typechecked
     * @throws ErrorInternal if this node and y are not compatible
     */
    public final Expr in(Expr y) {
        if (this.type==null) throw this.internalError("The node is not yet typechecked");
        if (y.type==null) throw y.internalError("The node is not yet typechecked");
        int thisArity=this.type.arity();
        if (thisArity>0 && thisArity==y.type.arity())
        	return ExprBinary.Op.IN.make(this.pos, this, y, Type.FORMULA);
        throw internalError("Cannot perform Expr.in()");
    }

    /**
     * Convenience method that
     * returns a typechecked node representing ("! this")
     * <br/> Note: this node must be fully typechecked.
     *
     * @throws ErrorInternal if this node is not fully typechecked
     * @throws ErrorInternal if this node is not a formula
     */
    public final Expr not() {
        if (this.type==null) throw internalError("The node is not yet typechecked");
        if (this.type.isBool) return ExprUnary.Op.NOT.make(this.pos, this, Type.FORMULA);
        throw internalError("Cannot perform Expr.not()");
    }

    /**
     * Convenience method that
     * returns a typechecked node representing (this &amp;&amp; y)
     * <br/> Note: this node and y must both be fully typechecked.
     *
     * @throws ErrorInternal if this node and y are not both fully typechecked
     * @throws ErrorInternal if this node and y are not compatible
     */
    public final Expr and(Expr y) {
        if (this.type==null) throw this.internalError("The node is not yet typechecked");
        if (y.type==null) throw y.internalError("The node is not yet typechecked");
        if (this.type.isBool && y.type.isBool)
        	return ExprBinary.Op.AND.make(this.pos, this, y, Type.FORMULA);
        throw internalError("Cannot perform Expr.and()");
    }

    /**
     * Convenience method that
     * returns a typechecked node representing (this.y)
     * <br/> Note: this node and y must both be fully typechecked.
     *
     * @throws ErrorInternal if this node and y are not both fully typechecked
     * @throws ErrorInternal if this node and y are not compatible
     */
    public final Expr join(Expr y) {
        if (this.type==null) throw this.internalError("The node is not yet typechecked");
        if (y.type==null) throw y.internalError("The node is not yet typechecked");
        Type ans=this.type.join(y.type);
        if (ans.arity()<1) throw internalError("Cannot perform Expr.join()");
        return new ExprJoin(this.pos, this, y, ans);
    }

    /**
     * Convenience method that
     * returns a typechecked node representing (this -> y)
     * <br/> Note: this node and y must both be fully typechecked.
     *
     * @throws ErrorInternal if this node and y are not both fully typechecked
     * @throws ErrorInternal if this node and y are not compatible
     */
    public final Expr product(Expr y) {
        if (this.type==null) throw this.internalError("The node is not yet typechecked");
        if (y.type==null) throw y.internalError("The node is not yet typechecked");
        Type ans=this.type.product_of_anyEmptyness(y.type);
        if (ans.arity()<1) throw internalError("Cannot perform Expr.product()");
        return ExprBinary.Op.ARROW.make(this.pos, this, y, ans);
    }

    /**
     * Convenience method that
     * returns a typechecked node representing the universal set.
     *
     * @param p - the position in the file where the universal set is used
     */
    public static final Expr univ(Pos p) {
        return ExprConstant.Op.UNIV.make(p);
    }
}