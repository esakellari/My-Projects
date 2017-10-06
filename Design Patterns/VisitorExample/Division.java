package VisitorExample;

//The Visitable class Division.
public class Division implements Expression {
    int firstTerm;
    int secondTerm;
    
    Division(int firstTerm, int secondTerm){
        this.firstTerm = firstTerm;
        this.secondTerm = secondTerm;
    }
    
    public void accept(ExpressionVisitor v){ 
        v.visit(this); 
    }
}
