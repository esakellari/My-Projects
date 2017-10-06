package VisitorExample;

//The Visitable class Multiplication.
public class Multiplication implements Expression {
    int firstTerm;
    int secondTerm;
    
    Multiplication(int firstTerm, int secondTerm){
        this.firstTerm = firstTerm;
        this.secondTerm = secondTerm;
    }
    
    public void accept(ExpressionVisitor v){ 
        v.visit(this); 
    }
}