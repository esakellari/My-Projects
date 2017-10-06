package VisitorExample;

//The Visitable class Subtraction.
public class Subtraction implements Expression {
    int firstTerm;
    int secondTerm;
    
    Subtraction(int firstTerm, int secondTerm){
        this.firstTerm = firstTerm;
        this.secondTerm = secondTerm;
    }
    
    public void accept(ExpressionVisitor v){ 
        v.visit(this); 
    }
}