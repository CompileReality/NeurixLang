package Compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConstantPool {
    List<String> constants = new ArrayList<>();

    public void addConstant(String obj){
        if (!checkConstant(obj)){
            constants.add(obj);
        }
    }

    public boolean checkConstant(String obj){
        for (String value : constants){
            if (Objects.equals(value, obj)){
                return true;
            }
        }
        return false;
    }

    public int getSpecificMemoryAddress(String obj){
        int i = 0;
        for (String value : constants){
            if (Objects.equals(value, obj)){
                return i;
            }
            i++;
        }
        return -1;
    }
}
