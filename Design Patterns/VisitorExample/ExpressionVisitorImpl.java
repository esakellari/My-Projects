package VisitorExample;

//The implementation of the Visitor.
public class ExpressionVisitorImpl implements ExpressionVisitor {
    public void visit(Addition a){
        int result = a.firstTerm + a.secondTerm;
        System.out.println("The result of your addition is: " + result); 
    }
    
    public void visit(Subtraction s) {
        int result = s.firstTerm - s.secondTerm;
        System.out.println("The result of your subtraction is: " + result);
    }
    
    public void visit(Multiplication m) {
        int result = m.firstTerm * m.secondTerm;
        System.out.println("The result of your multiplication is: " + result); 
    }
    
    public void visit(Division d) {
        int result = d.firstTerm / d.secondTerm;
        System.out.println("The result of your division is: " + result); 
    }
}