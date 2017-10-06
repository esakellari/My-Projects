public class List {

    Node head = null;
    String name;

    List(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
    public void prepend(Object obj) {
        Node newNode = new Node(obj);
        newNode.next = this.head;
        this.head = newNode;
    }

    public Object pop() {
        Node node = this.head;
        if(node != null) {
            this.head = node.next;
            return node.data;
        }
        return null;
    }

    public Object head() {
        return this.head == null ? null : this.head.data;
    }

    public void printList() {
        Node pointer = this.head;
        System.out.println(this.name);
        if(pointer == null) {
            System.out.println("The list is empty");
            return;
        }
        while(pointer != null) {
            System.out.println((String)pointer.data);
            pointer = pointer.next;
        }
    }
}