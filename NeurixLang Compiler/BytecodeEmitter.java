package Compiler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

public class BytecodeEmitter {

    Parser ast;
    SymbolTables table;
    ConstantPool pool;
    ByteArrayOutputStream bytecode;
    ASTResolver resolver;
    byte[] bytecodeVersion;

    ByteArrayOutputStream functionTable;
    ByteArrayOutputStream ClassTable;
    ByteArrayOutputStream MainTable;

    public BytecodeEmitter(Parser ast,SymbolTables table,ConstantPool pool,byte[] bytecodeVersion) throws IOException {
        this.ast = ast;
        this.pool = pool;
        this.table = table;
        bytecode = new ByteArrayOutputStream();
        this.bytecodeVersion = bytecodeVersion;
        resolver = new ASTResolver(ast,table,pool);
        Generate();
    }

    public void Generate() throws IOException {
        sortOutTables();
        createHeaders();
        createConstantPool();
        createFunctionTable();
        createClassTable();
        createMainAssemblyStack();
    }

    public ByteArrayOutputStream getBytecode() {
        return bytecode;
    }

    private void sortOutTables() throws IOException {
        HashMap<Integer,ByteArrayOutputStream> codes = resolver.codes;
        if (codes.get(-1) != null){
            MainTable = codes.get(-1);
        }
        for (int i = 0; i < table.Table.size();i++){
            if (Objects.equals(table.Table.get(i).type, SymbolTables.FUNCTION_TYPE)){
                if (functionTable == null){
                    functionTable = codes.get(i);
                }else {
                    functionTable.write(codes.get(i).toByteArray());
                }
            } else if (Objects.equals(table.Table.get(i).type, SymbolTables.CLASS_TYPE)) {
                if (ClassTable == null){
                    ClassTable = codes.get(i);
                }else {
                    ClassTable.write(codes.get(i).toByteArray());
                }
            }
        }
    }

    public void createHeaders() throws IOException {
        bytecode.write(bytecodeVersion);
    }

    public void createConstantPool() throws IOException {
        bytecode.write(new byte[]{0x00,0x01});
        for (String value:pool.constants){
            try{
                int val = Integer.parseInt(value);
                ByteArrayOutputStream  out = new ByteArrayOutputStream();
                while(val > 0){
                    out.write(val & 0xff);
                    val >>= 8;
                }
                bytecode.write(out.size());
                bytecode.write(out.toByteArray());
            } catch (NumberFormatException e) {
                try{
                    boolean val = Boolean.parseBoolean(value);
                    if (!(Objects.equals(value, "false") && !val)){
                        throw new Error("");
                    }
                    bytecode.write(0x01);
                    if (!val){
                        bytecode.write(0x00);
                    }else {
                        bytecode.write(0x01);
                    }
                }catch (Error e1){
                    bytecode.write(value.getBytes().length);
                    bytecode.write(value.getBytes());
                }
            }
        }
    }

    public void createFunctionTable() throws IOException {
        bytecode.write(new byte[]{0x00,0x03});
        if(functionTable == null){
            return;
        }
        bytecode.write(functionTable.toByteArray());
    }

    public void createClassTable() throws IOException {
        bytecode.write(new byte[]{0x00,0x02});
        if(ClassTable == null){
            return;
        }
        bytecode.write(ClassTable.toByteArray());
    }

    public void createMainAssemblyStack() throws IOException {
        bytecode.write(new byte[]{0x00,0x04});
        if(MainTable == null){
            return;
        }
        bytecode.write(MainTable.toByteArray());
    }
}
