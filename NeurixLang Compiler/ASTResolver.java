package Compiler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class ASTResolver {
    Parser parser;
    SymbolTables globalTable;
    ConstantPool pool;

    HashMap<Integer, ByteArrayOutputStream> codes = new HashMap<>();
    int parentIndex = -1;
    byte[] AccessAddress;
    int StkPushMemoryAddress;
    int IFoffset = 0;

    public ASTResolver(Parser ast,SymbolTables table,ConstantPool pool) throws IOException {
        parser = ast;
        globalTable = table;
        globalTable.pool = pool;
        this.pool = pool;
        this.StkPushMemoryAddress = pool.constants.size();
        codes.put(-1,new ByteArrayOutputStream());
        resolve(parser.nodes);
    }

    public void resolve(List<AST> nodes) throws IOException {
        for (AST node:nodes){
            if (node instanceof ClassNode){
                resolveClassNode((ClassNode)node);
            } else if (node instanceof FunctionNode) {
                resolveFunctionNode((FunctionNode)node);
            }else if (node instanceof ClassInit){
                resolveClassInit((ClassInit)node);
            } else if (node instanceof DeclarationNode) {
                resolveDeclaration((DeclarationNode)node);
            }else if (node instanceof LiteralNode) {
                resolveConstant((LiteralNode) node);
            } else if (node instanceof BoolNode){
                resolveBoolConstant((BoolNode) node);
            }else if (node instanceof NumberNode){
                resolveNumConstant((NumberNode)node);
            } else if (node instanceof FunctionCall){
                resolveFunctionCall((FunctionCall)node);
            } else if (node instanceof AccessNode){
                resolveAccess((AccessNode)node);
            } else if (node instanceof ScopeNode){
                resolve(((ScopeNode)node).node);
            } else if (node instanceof ReturnNode){
                resolveReturn((ReturnNode)node);
            } else if (node instanceof RedeclareVar){
                resolveRedeclare((RedeclareVar)node);
            }else if (node instanceof BinaryExpressionNode) {
               resolveBinary((BinaryExpressionNode)node);
            } else if (node instanceof NotNode) {
                resolveNot((NotNode)node);
            } else if (node instanceof IFNode){
                resolveIF((IFNode)node);
            } else if (node instanceof WhileNode){
                resolveWhile((WhileNode) node);
            } else if (node instanceof IdentifierNode) {
                resolveIdentifier((IdentifierNode) node);
            } else if (node instanceof NullNode) {
                resolveNull();
            } else if (node instanceof ImportNode) {
                resolveImport((ImportNode)node);
            }
        }
    }

    private void resolveImport(ImportNode node) throws IOException {
        LiteralNode node2 = new LiteralNode();
        node2.value = node.path;
        resolveConstant(node2);
        codes.get(parentIndex).write(new byte[]{0x32});
    }

    private void resolveNull() throws IOException {
        codes.get(parentIndex).write(new byte[]{0x31,0x01,(byte)(StkPushMemoryAddress + 1)});
    }

    private void resolveIdentifier(IdentifierNode node) throws IOException {
        int key = globalTable.findNode(new SymbolNode(node.value, SymbolTables.FUNCTION_TYPE,node.parentIndex));
        int key2 = globalTable.findNode(new SymbolNode(node.value,SymbolTables.VARIABLE_TYPE,node.parentIndex));
        key = key + key2 + 1;
        codes.get(parentIndex).write(new byte[]{0x31,0x01,(byte)key});
    }

    private void resolveWhile(WhileNode node) throws IOException {
        List<AST> nodes = new ArrayList<>();
        nodes.add(node.condition);
        codes.get(parentIndex).write(new byte[]{(byte)(0xfe+IFoffset)});
        resolve(nodes);
        codes.get(parentIndex).write(new byte[]{0x02,0x00});
        ByteArrayOutputStream temp = codes.get(parentIndex);
        codes.put(parentIndex,new ByteArrayOutputStream());
        IFoffset++;
        resolve(node.expr);
        IFoffset--;
        codes.get(parentIndex).write(new byte[]{0x29,(byte)(0xfe+IFoffset)});
        temp.write(new byte[]{0x11,(byte)codes.get(parentIndex).size()});
        temp.write(codes.get(parentIndex).toByteArray());
        codes.put(parentIndex,temp);
    }

    private void resolveIF(IFNode node) throws IOException {
        List<AST> nodes = new ArrayList<>();
        nodes.add(node.condition);
        resolve(nodes);
        codes.get(parentIndex).write(new byte[]{0x02,0x00});
        ByteArrayOutputStream temp = codes.get(parentIndex);
        codes.put(parentIndex,new ByteArrayOutputStream());
        IFoffset++;
        resolve(node.expr);
        IFoffset--;
        codes.get(parentIndex).write(new byte[]{0x29,(byte)(0xfe+IFoffset)});
        if (!node.ElifCondition.isEmpty()){
            IFNode node1 = new IFNode();
            node1.condition = node.ElifCondition.getFirst();
            node1.expr = node.elifexpr.getFirst();
            node1.elseexpr = node.elseexpr;
            node1.ElifCondition = node.ElifCondition.subList(1,node.ElifCondition.size());
            node1.elifexpr = node.elifexpr.subList(1,node.elifexpr.size());
            resolveIF(node1);
        }if (node.elseexpr != null) {
            temp.write(new byte[]{0x11, (byte) (codes.get(parentIndex).toByteArray().length)});
            temp.write(codes.get(parentIndex).toByteArray());
            codes.put(parentIndex,temp);
            IFoffset++;
            resolve(node.elseexpr);
            IFoffset--;
            codes.get(parentIndex).write(new byte[]{(byte)(0xfe+IFoffset)});
        }else {
            temp.write(new byte[]{0x10,(byte)(codes.get(parentIndex).toByteArray().length)});
            temp.write(codes.get(parentIndex).toByteArray());
            codes.put(parentIndex,temp);
        }
    }

    private void resolveNot(NotNode node) throws IOException {
        List<AST> nodes = new ArrayList<>();
        nodes.add(node.childNode);
        resolve(nodes);
        codes.get(parentIndex).write(new byte[]{0x21});
    }

    private void resolveBinary(BinaryExpressionNode node) throws IOException {
        List<AST> list = new ArrayList<>();
        list.add(node.value1);
        resolve(list);
        list.set(0,node.value2);
        resolve(list);
        switch (node.operation) {
            case "+":codes.get(parentIndex).write(new byte[]{0x06});break;
            case "-":codes.get(parentIndex).write(new byte[]{0x07});break;
            case "*":codes.get(parentIndex).write(new byte[]{0x08});break;
            case "/":codes.get(parentIndex).write(new byte[]{0x09});break;
            case "&&","==":codes.get(parentIndex).write(new byte[]{0x19});break;
            case "||":codes.get(parentIndex).write(new byte[]{0x20});break;
            case "%":codes.get(parentIndex).write(new byte[]{0x23});break;
            case "<":codes.get(parentIndex).write(new byte[]{0x24,0x00});break;
            case "<=":codes.get(parentIndex).write(new byte[]{0x24,0x01});break;
            case ">":codes.get(parentIndex).write(new byte[]{0x25,0x00});break;
            case ">=":codes.get(parentIndex).write(new byte[]{0x25,0x01});break;
            case "++":codes.get(parentIndex).write(new byte[]{0x26});break;
            case "--":codes.get(parentIndex).write(new byte[]{0x27});break;
        }
        codes.get(parentIndex).write(new byte[]{0x02,(byte)(0xff - StkPushMemoryAddress)});
    }

    private void resolveRedeclare(RedeclareVar node) throws IOException {
        DeclarationNode node1 = new DeclarationNode();
        node1.ParentIndex = node.ParentIndex;
        node1.name = node.name;
        List<AST> nodes = new ArrayList<>();
        nodes.add(node.value);
        resolve(nodes);
        resolveDeclaration(node1);
    }

    private void resolveReturn(ReturnNode node) throws IOException {
        List<AST> nodes = new ArrayList<>();
        nodes.add(node.returnValue);
        resolve(nodes);
        codes.get(parentIndex).write((byte)0x12);
    }

    private void resolveBoolConstant(BoolNode node) throws IOException {
        LiteralNode node1 = new LiteralNode();
        node1.value = Objects.equals(node.value, "true") ?"1":"0";
        resolveConstant(node1);
    }

    private void resolveAccess(AccessNode node) throws IOException {
        List<AST> nodes = new ArrayList<>();
        nodes.add(node.var);
        int index = node.selfIndex;
        ByteArrayOutputStream addr = new ByteArrayOutputStream();
        while (index > 0){
            addr.write((byte) index & 0xff);
            index >>= 8;
        }
        AccessAddress = addr.toByteArray();
        resolve(nodes);
    }

    private void resolveFunctionCall(FunctionCall node) throws IOException {
        int key = globalTable.findNode(new SymbolNode(node.name,SymbolTables.FUNCTION_TYPE,node.ParentIndex));
        ByteArrayOutputStream code;
        boolean cond = false;
        try{
            code = codes.get(parentIndex);
            code.size();
        }catch (NullPointerException e){
            cond = true;
            code = new ByteArrayOutputStream();
        }
        if (cond){
            codes.put(-1,code);
            code = codes.get(-1);
        }
        int temp = parentIndex;
        if(cond) {
            parentIndex = -1;
        }
        resolve(node.arg);
        parentIndex = temp;
        code.write(new byte[]{0x28,(byte) AccessAddress.length});
        for (byte part : AccessAddress){
            code.write(new byte[]{part});
        }
        int len = 0;
        ByteArrayOutputStream temp2 = new ByteArrayOutputStream();
        while (key > 0){
            temp2.write(new byte[]{(byte)(key & 0xff)});
            key>>=8;
            len++;
        }
        code.write(new byte[]{0x30,(byte)len});
        code.write(temp2.toByteArray());
        code.write(new byte[]{0x17});
    }

    private void resolveClassInit(ClassInit node) throws IOException {
        int key = globalTable.findNode(new SymbolNode(node.name,SymbolTables.CLASS_TYPE,node.ParentIndex));
        ByteArrayOutputStream code = new ByteArrayOutputStream();
        int len = 0;
        while(key>=0){
            code.write(new byte[]{(byte)(key & 0xff)});
            key >>= 8;
            if (key == 0){
                key--;
            }
            len++;
        }
        ByteArrayOutputStream code1 = codes.get(parentIndex);
        code1.write(new byte[]{0x30,(byte)len});
        code1.write(code.toByteArray());
    }

    private void resolveNumConstant(NumberNode node) throws IOException {
        LiteralNode node1 = new LiteralNode();
        node1.value = node.value;
        resolveConstant(node1);
    }

    private void resolveConstant(LiteralNode node) throws IOException {
        codes.get(parentIndex).write(new byte[]{0x31,0x01});
        codes.get(parentIndex).write(new byte[]{(byte)(0xff-pool.getSpecificMemoryAddress(node.value))});

    }

    private void resolveFunctionNode(FunctionNode node) throws IOException {
        int key = globalTable.findNode(new SymbolNode(node.name,SymbolTables.FUNCTION_TYPE,node.ParentIndex));
        ByteArrayOutputStream parentCode = codes.get(parentIndex);
        ByteArrayOutputStream code = new ByteArrayOutputStream();
        uploadThroughStk(parentCode,(byte)0x13,key);
        uploadThroughStk(code,(byte)0x14,key);
        int length = node.arguments.size();
        code.write(new byte[]{0x16,(byte)length});
        codes.put(key,code);
        int temp = parentIndex;
        parentIndex = key;
        resolve(node.expr);
        parentIndex = temp;
        if (codes.get(key).toByteArray()[codes.get(key).toByteArray().length-1] != 0x12){
            codes.get(key).write(0x12);
        }
    }

    private void resolveDeclaration(DeclarationNode node) throws IOException {
        ByteArrayOutputStream code;
        boolean cond = false;
        int key = globalTable.findNode(new SymbolNode(node.name,SymbolTables.VARIABLE_TYPE,node.ParentIndex));
        try{
            code = codes.get(parentIndex);
            code.size();
        }catch (NullPointerException e){
            cond = true;
            code = new ByteArrayOutputStream();
        }
        if (cond){
            codes.put(-1,code);
            code = codes.get(-1);
        }
        List<AST> nodes = new ArrayList<>();
        int temp = parentIndex;
        if(cond) {
            parentIndex = -1;
        }
        nodes.add(node.exp);
        resolve(nodes);
        parentIndex = temp;
        int length = 0;
        if (AccessAddress == null) {
            ByteArrayOutputStream temp2 = new ByteArrayOutputStream();
            while (key >= 0) {
                temp2.write(new byte[]{(byte) (key & 0xff)});
                key >>= 8;
                length++;
                if (key == 0){
                    break;
                }
            }
            code.write(new byte[]{0x30,(byte) length});
            code.write(temp2.toByteArray());
        }else{
            code.write(new byte[]{0x28,(byte) AccessAddress.length});
            for (byte part : AccessAddress){
                code.write(new byte[]{part});
            }
            ByteArrayOutputStream temp2 = new ByteArrayOutputStream();
            while (key >= 0) {
                temp2.write(new byte[]{(byte) (key & 0xff)});
                key >>= 8;
                length++;
                if (key == 0){
                    break;
                }
            }
            code.write(new byte[]{0x30,(byte) length});
            code.write(temp2.toByteArray());
        }
        code.write(new byte[]{0x01});
    }

    public void uploadThroughStk(ByteArrayOutputStream out,byte opCode,int key) throws IOException {
        int temp  = key;
        int length = 0;
        while (key > 0) {
            key >>= 8;
            length++;
        }
        out.write(new byte[]{opCode,(byte)(length)});
        while (length >= 0){
            out.write(new byte[]{0x02,(byte)(temp & 0xff)});
            temp>>=8;
            length--;
        }
    }

    private void resolveClassNode(ClassNode node) throws IOException {
        ByteArrayOutputStream code = new ByteArrayOutputStream();
        int key = globalTable.findNode(new SymbolNode(node.name,SymbolTables.CLASS_TYPE,node.ParentIndex));
        uploadThroughStk(code,(byte)0x15,key);
        codes.put(key,code);
        int temp = parentIndex;
        parentIndex = key;
        resolve(node.expr);
        parentIndex = temp;
    }
}
