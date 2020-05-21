package margotua.mining;

import margotua.util.Formatter;
import margotua.util.TaintedValue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UnparseTree {
    public final Node root;

    public UnparseTree() {
        this.root = new Node("START");
    }

    @Override
    public String toString() {
        return root.toString();
    }

    public static class Node {
        public final String name;
        public TaintedValue<String> value;
        public final Node parent;
        public final List<Node> descendants;

        public Node(Node parent, String name, TaintedValue<String> value) {
            this.parent = parent;
            this.name = name;
            this.value = value;
            descendants = new ArrayList<>();
        }

        public Node(Node parent, String name) {
            this(parent, name, null);
        }

        public Node(String name) {
            this(null, name, null);
        }

        public boolean isTerminal() {
            return value != null;
        }

        @Override
        public String toString() {
            return "Node<"+name+((value!=null)?", "+value:"")+">:{\n"+
                    Formatter.prefixLines("    ", descendants.stream()
                    .map(node->node.toString())
                    .collect(Collectors.joining(",\n")))+"\n}";
        }
    }
}
