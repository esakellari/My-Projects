package VisitorExample;
import java.io.IOException;
import java.util.Scanner;

//This program presents the Visitor Design Pattern in a 
//very simple form of a calculator. The Mathematical expressions
//supported are : Addition, Subtraction, Multiplication and Division.
//The input is given from the user.
public class VisitorExample {
    public static void main(String[] args) throws IOException {
        
        Scanner scan = new Scanner(System.in);
        do {
            System.out.print("Please enter some mathematical "
                    + "calculation with two numbers and one "
                    + "operator separated by spaces. (For example: 2 + 3): ");

            String expr = scan.nextLine();
            
            //Split the input to get the terms and the operator.
            String terms[] = expr.split(" ");
            int firstTerm = Integer.parseInt(terms[0]);
            int secondTerm = Integer.parseInt(terms[2]);
            
            //The Visitor Class.
            ExpressionVisitor v = new ExpressionVisitorImpl();

            //Switch on the operator.
            //In this switch statement the Visitable objects are created.
            switch (terms[1]) {
                case "+":
                    Expression a = new Addition(firstTerm, secondTerm);
                    a.accept(v);
                    break;
                case "-":
                    Expression s = new Subtraction(firstTerm, secondTerm);
                    s.accept(v);
                    break;
                case "*":
                    Expression m = new Multiplication(firstTerm, secondTerm);
                    m.accept(v);
                    break;
                case "/":
                    Expression d = new Division(firstTerm, secondTerm);
                    d.accept(v);
                    break;
            } 

            System.out.println("Press enter to give another expression, or press \'exit\' to exit.");    
        }while (scan.nextLine().compareTo("exit") != 0);
    }
}