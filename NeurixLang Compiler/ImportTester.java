package Compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

public class ImportTester {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        File file = new File(args[0]);
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
        System.out.println((ArrayList<SymbolNode>)in.readObject());
    }
}
