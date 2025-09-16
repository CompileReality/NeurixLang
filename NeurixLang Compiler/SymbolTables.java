package Compiler;

import java.io.*;
import java.util.ArrayList;
import java.util.Objects;

public class SymbolTables {
    ArrayList<SymbolNode> Table = new ArrayList<>();
    ArrayList<ArrayList<SymbolNode>> importedTables = new ArrayList<>();
    public ConstantPool pool;

    public static final String CLASS_TYPE = "class";
    public static final String VARIABLE_TYPE = "var";
    public static final String FUNCTION_TYPE = "func";
    public static final String OTHER_SCOPE_TYPE = "other";
    public static final int ALL_CHILD = 10000;

    public void addObject(SymbolNode node){
        if (checkAvailability(node)) {
            Table.add(node);
        } else {
            throw new Error("DuplicateSymbolError: Symbol named '"+node.name+"' already exist");
        }
    }

    public void attachChildToParent(int ParentIndex,int child){
        if (ParentIndex != -1) {
            if (Table.get(ParentIndex).childIndex == null){
                Table.get(ParentIndex).childIndex  = new ArrayList<>();
            }
            Table.get(ParentIndex).childIndex.add(child);
        }
    }

    public void importTable(String path,String projectDirectory) throws IOException, ClassNotFoundException {
        File code = new File(path);
        String filename = code.getName().substring(0,code.getName().indexOf('.'));
        code = new File(code.getParent() + "//"+filename+".nst");
        if (!code.exists()){
            code = new File(projectDirectory +"//out//"+ filename+".nst");
            if (!code.exists() && projectDirectory != null){

                Main.main(new String[]{path,projectDirectory,"0"});
            }
        }
        ObjectInputStream stream = new ObjectInputStream(new FileInputStream(code));
        ArrayList<SymbolNode> nodes = (ArrayList<SymbolNode>)stream.readObject();
        importedTables.add(nodes);
        stream.close();
    }

    public void finalizeTable(String path,String filename) throws IOException {
        File code = new File(path+"//"+filename+".nst");
        code.createNewFile();
        ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(code));
        int i =0;
        ArrayList<Integer> list = new ArrayList<>();
        for (SymbolNode node:Table){
            if (!node.scope.contains("public") && !(node.parentIndex == -1)){
                list.add(i);
            }
            i++;
        }
        i = 0;
        int k =0;
        for (int item:list){
            Table.remove(item - i);
            if (list.get(k+1) > item){
                i++;
            }
        }
        output.writeObject(Table);
        output.close();
    }

    public boolean checkAvailability(SymbolNode node){
        try {
            checkName(node,Table);
            for (ArrayList<SymbolNode> tables:importedTables){
                checkName(node,tables);
            }
        }catch (Error e){
            return false;
        }
        return true;
    }

    public int findNode(SymbolNode node){
        return findNode(node,0);
    }

    public int findNode(SymbolNode node,int code){
        int index = 0;
        int retr = -1;
        importedTables.addFirst(Table);
        if (code != 0) {
            for (ArrayList<SymbolNode> table : importedTables) {
                for (SymbolNode node1 : table) {
                    if (node.equals(node1)) {
                        if (pool == null) {
                            retr = index;
                            break;
                        }
                        if (pool.constants.isEmpty()) {
                            retr = index;
                            break;
                        }
                        if (index < (0xff - pool.constants.size() - 2) || index > 0xff) {
                            retr = index;
                            break;
                        }
                        retr = (index + pool.constants.size() + 2);
                        break;
                    }
                    index++;
                }
            }
        }
        else {
            for (SymbolNode node1 : Table) {
                if (node.equals(node1)) {
                    if (pool == null) {
                        retr = index;
                        break;
                    }
                    if (pool.constants.isEmpty()) {
                        retr = index;
                        break;
                    }
                    if (index < (0xff - pool.constants.size() - 2) || index > 0xff) {
                        retr = index;
                        break;
                    }
                    retr = (index + pool.constants.size() + 2);
                    break;
                }
                index++;
            }
        }
        importedTables.removeFirst();
        return retr;
    }

    private void checkName(SymbolNode node,ArrayList<SymbolNode> Table) {
        int parentIndex = node.parentIndex;
        while(parentIndex >= -1 || parentIndex == ALL_CHILD ){
            if (parentIndex == -1 || parentIndex == ALL_CHILD){
                for (SymbolNode child : Table){
                    if (child.parentIndex != -1 && !(parentIndex == ALL_CHILD)){
                        continue;
                    }
                    if (Objects.equals(child.name, node.name) && Objects.equals(child.type, node.type)){
                        throw new Error("DuplicateSymbolError: Symbol named '"+node.name+"' already exist");
                    }
                }
                return;
            }else{
                SymbolNode parent = Table.get(parentIndex);
                for (int i:parent.childIndex){
                    try{
                        Table.get(i);
                    } catch (IndexOutOfBoundsException e) {
                        continue;
                    }
                    if (Objects.equals(Table.get(i).name, node.name) && Objects.equals(Table.get(i).type, node.type)){
                        throw new Error("DuplicateSymbolError: Symbol named '"+node.name+"' already exist");
                    }
                }
                parentIndex = parent.parentIndex;
            }
        }
    }
}
