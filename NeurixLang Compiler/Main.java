package Compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws IOException {
        long time = System.nanoTime();
        String path = args[0];
        Path path1 = Paths.get(path);
        String contentStr = new String(Files.readAllBytes(path1));
        String projectDirectory = args.length < 2 ? path1.getParent().toString() : args[1];
        String OutDirectory = args.length < 2 ? path1.getParent().toString() : args[1] + "//out";
        String filename = (path1.getFileName().toString().substring(0, path1.getFileName().toString().indexOf(".")));
        SymbolTables globalTable = new SymbolTables();
        ConstantPool pool = new ConstantPool();
        try {
            Lexer lex = new Lexer(contentStr);
            Parser parser = new Parser(lex, contentStr,globalTable, pool,projectDirectory,OutDirectory + "//" + filename + ".nb");
            BytecodeEmitter emitter = new BytecodeEmitter(parser,globalTable,pool,new byte[]{0x00,0x01});
            File bytecodeFile = new File(OutDirectory + "//" + filename + ".nb");
            bytecodeFile.getParentFile().mkdir();
            OutDirectory = bytecodeFile.getParent();
            FileOutputStream fileWriter = new FileOutputStream(bytecodeFile);
            emitter.getBytecode().writeTo(fileWriter);
            fileWriter.close();
            globalTable.finalizeTable(OutDirectory,filename);
            OutDirectory = bytecodeFile.getAbsolutePath();
            if (args.length < 3) {
                System.out.println("Compiled! Output bytecode location: " + OutDirectory);
                System.out.println("\nTime : " + (System.nanoTime() - time) + "ns");
            }
        } catch (Exception e) {
            if(e instanceof Error) {
                System.out.println(e.getMessage());
            }else {
                e.printStackTrace();
            }
        }
    }
}