package Compiler;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SymbolNode implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    String name;
    String type;
    List<String> scope = new ArrayList<>();
    int parentIndex = -1;
    ArrayList<Integer> childIndex = new ArrayList<>();

    public SymbolNode(){}

    public SymbolNode(String name,String type,int ParentIndex){
        this.name = name;
        this.type = type;
        this.parentIndex = ParentIndex;
    }

    @Override
    public String toString() {
        return "SymbolNode{" +
                "\n\tname=" + name +
                "\n\ttype=" + type +
                "\n\tparentIndex=" + parentIndex +
                "\n\tchildIndex=\n\t" + childIndex +
                "\n\tscope=" + scope +
                "\n}";
    }

    @Override
    public boolean equals(Object obj) {
        return (Objects.equals(type, ((SymbolNode) obj).type)) && (Objects.equals(name, ((SymbolNode) obj).name) && (parentIndex == SymbolTables.ALL_CHILD||(Objects.equals(parentIndex,((SymbolNode)obj).parentIndex))));
    }
}
