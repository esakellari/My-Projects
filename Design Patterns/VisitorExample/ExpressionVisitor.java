package VisitorExample;

//The interface of the Visitor.
public interface ExpressionVisitor {
    public void visit(Addition a);
    public void visit(Subtraction s);  
    public void visit(Multiplication m);
    public void visit(Division d);
}