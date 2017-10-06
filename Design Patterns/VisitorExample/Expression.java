package VisitorExample;

//All Visitable classes inherit from this class.
public interface Expression {
    public void accept(ExpressionVisitor visitor);
}