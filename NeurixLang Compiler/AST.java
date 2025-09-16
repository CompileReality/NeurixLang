package Compiler;

import java.util.HashMap;
import java.util.List;

public abstract class AST {}

class DeclarationNode extends AST{
    String name;

    int ParentIndex;

    @Optional
    AST exp;

    @Override
    public String toString() {
        return "\nDeclarationNode{" + "\n"+
                "name=" + name + '\n' +
                "exp=" + exp +
                '}';
    }
}

class IFNode extends AST{
    AST condition;

    @Optional
    List<AST> ElifCondition;

    @Optional
    List<AST> expr;
    @Optional
    List<AST> elseexpr;
    @Optional
    List<List<AST>> elifexpr;

    @Override
    public String toString() {
        return "\nIFNode{" +
                "\ncondition=" + condition +
                "\nexpr=" + expr +
                "\nElseNode=" + elseexpr +
                "\nElifCondition=" + ElifCondition +
                "\nElifNode=" + elifexpr +
                '}';
    }
}

class ImportNode extends AST{
    String path;

    @Override
    public String toString() {
        return "\nImportNode{" +
                "\npath='" + path + "'" +
                '}';
    }
}

class ClassInit extends AST {
    String name;

    int ParentIndex;

    @Optional
    List<AST> arg;

    @Override
    public String toString() {
        return "\nClassInit{" +
                "\nname='" + name +
                "\narg=" + arg +
                "\n}";
    }
}

class RedeclareVar extends AST {
    String name;

    int ParentIndex;

    @Optional
    AST value;

    @Override
    public String toString() {
        return "\nRedeclaringVar{" +
                "\nname=" + name +
                "\nvalue=" + value +
                "\n}";
    }
}

class AccessNode extends AST{
    String Super;

    int selfIndex;

    AST var;

    @Override
    public String toString() {
        return "\nAccessNode{" +
                "\nSuper='" + Super + "'" +
                "\nvar=" + var +
                '}';
    }
}

class FunctionCall extends AST{
    String name;

    int ParentIndex;

    @Optional
    List<AST> arg;

    @Override
    public String toString() {
        return "\nFunctionCall{" +
                "\nname='" + name + "'" +
                "\narg=" + arg +
                '}';
    }
}

class NotNode extends AST {
    AST childNode;

    @Override
    public String toString() {
        return "\nNotNode{" +
                "\nchildNode=" + childNode +
                "\n}";
    }
}

class WhileNode extends AST{
    AST condition;

    @Optional
    List<AST> expr;

    @Override
    public String toString() {
        return "\nWhileNode{" +
                "\ncondition=" + condition +
                "\nexpr=" + expr +
                '}';
    }
}

class ClassNode extends AST{
    String name;

    int ParentIndex;

    @Optional
    List<AST> expr;

    @Override
    public String toString() {
        return "\nClassNode{" +
                "name=" + name + '\n' +
                "expr=" + expr +
                '}';
    }
}

class ScopeNode extends AST{
    String scope;

    List<AST> node;

    @Override
    public String toString() {
        return "\nScopeNode{" +
                "scopes=" + scope + '\n' +
                "node=" + node +
                '}';
    }
}

class LiteralNode extends AST{
    @Optional
    String value;

    @Override
    public String toString() {
        return "\nLiteralNode{" +
                "\nvalue='" + value + "'" +
                '}';
    }
}

class ReturnNode extends AST{
    @Optional
    AST returnValue;

    @Override
    public String toString() {
        return "\nReturnNode{" +
                "\nreturnValue=" + returnValue +
                '}';
    }
}

class NumberNode extends AST{
    @Optional
    String value;

    @Override
    public String toString() {
        return "\nNumberNode{" + "\n"+
                "value=" + value +
                '}';
    }
}

class NullNode extends AST{
    @Override
    public String toString() {
        return "null(Node)";
    }
}

class BinaryExpressionNode extends AST{
    AST value1;
    String operation;
    AST value2;

    @Override
    public String toString() {
        return "\nBinaryExpressionNode{" +
                "\nvalue1=" + value1 +
                "\noperation='" + operation + "'"+
                "\nvalue2=" + value2 +
                '}';
    }
}

class BoolNode extends AST{
    String value;

    @Override
    public String toString() {
        return "\nBoolNode{" +
                "\nvalue='" + value + "'" +
                '}';
    }
}

class FunctionNode extends AST{

    String name;

    @Optional
    List<HashMap<String,String>> arguments;

    int ParentIndex;

    @Optional
    List<AST> expr;

    @Override
    public String toString() {
        return "\nFunctionNode{" +
                "\nname='" + name + "'" +
                "\narguments=" + arguments +
                "\nexpr=" + expr +
                '}';
    }
}

class IdentifierNode extends AST{
    String value;

    int parentIndex;

    @Override
    public String toString() {
        return "\nIdentifierNode{" +
                "\nvalue=" + value +
                '}';
    }
}