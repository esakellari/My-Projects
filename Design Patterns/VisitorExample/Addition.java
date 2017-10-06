package VisitorExample;

//The Visitable class Addition.
public class Addition implements Expression{
    int firstTerm;
    int secondTerm;
    
    Addition(int firstTerm, int secondTerm){
        this.firstTerm = firstTerm;
        this.secondTerm = secondTerm;
    }
    
    public void accept(ExpressionVisitor v){ 
        v.visit(this); 
    }
}
