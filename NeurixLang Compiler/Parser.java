package Compiler;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Parser {

    ArrayList<AST> nodes = new ArrayList<>();
    Stack<String> braces = new Stack<>();
    int braceTarget = 0;
    Lexer lexer;
    int masterIndex = 0;
    int expressionstack = 0;
    int exprstack = 0;
    Lexer.Token tok = new Lexer.Token();
    Lexer.Token tok2 = new Lexer.Token();
    SymbolTables table;
    int ParentIndex = -1;
    int ActualParentIndex = -1;
    Stack<String> nextScope = new Stack<>();
    ConstantPool pool;
    String projectDirectory;
    String filePath;

    public Parser(Lexer lexer,String contentStr,SymbolTables table,ConstantPool pool,String projectDirectory,String filePath) throws IOException, ClassNotFoundException {
        tok2.type = Lexer.TokenType.LPAREN;
        tok.type = Lexer.TokenType.COLON;
        this.lexer = lexer;
        this.table = table;
        this.pool = pool;
        this.filePath = filePath;
        this.projectDirectory = projectDirectory;
        for (int i = 0; i < contentStr.split("\r\n|\r|\n").length; i++) {
            List<Lexer.Token> node = this.lexer.nextExpressionToken();
            if (node != null){
                AST parsed = parseExpression(node);
                if (parsed != null) {
                    nodes.add(parsed);
                }
            }
        }
    }

    private AST parseExpression(List<Lexer.Token> tokens) throws IOException, ClassNotFoundException {
        AST result = null;
        Stack<String> parens = new Stack<>();
        boolean Continue = true;
        for (int i =0 ; i < tokens.size();i++){
            Lexer.Token token = tokens.get(i);
            switch (token.type) {
                case NOT:
                    result = notOperator(tokens,i);
                    Continue = false;
                    break;
                case CLASS:
                    result = Classnode(tokens, token, i);
                    Continue = false;
                    break;
                case RBRACE:
                    try {
                        braces.pop();
                        if ((braceTarget == braces.size())) {
                            Continue = false;
                        }
                    } catch (EmptyStackException e) {
                        throw new Error("SyntaxError: Unexpected '}' at line number:" + token.linenumber);
                    }
                    break;
                case NEW:
                    Object[] obj1 = ClassInitialisation(tokens,token,i);
                    result = (AST) obj1[0];
                    i = (int)obj1[1] + i + 3;
                    break;
                case LBRACE:
                    braces.push("{");
                    break;
                case LPAREN:
                    parens.push("(");
                    break;
                case RPAREN:
                    try {
                        parens.pop();
                    } catch (EmptyStackException e) {
                        throw new Error("SyntaxError: Unexpected ')' at line number:" + token.linenumber);
                    }
                    break;
                case IF:
                    result = IFnode(tokens, token, i, braces);
                    Continue = false;
                    break;
                case IMPORT:
                    result = importNode(tokens, token, i);
                    Continue = false;
                    break;
                case LET:
                    result = Declarationnode(tokens, token, i);
                    Continue = false;
                    break;
                case NUMBER:
                    result = Numbernode(tokens, token, i);
                    if (result instanceof NumberNode) {
                        if (((NumberNode) result).value.contains(".")) {
                            i += 2;
                        }
                    }
                    if (expressionstack == 0) {
                        masterIndex = 0;
                    }
                    masterIndex++;
                    if (tokens.size() != 1) {
                        if (checkForBinary(tokens, i)) {
                            expressionstack++;
                            AST parsed = parseExpression(tokens.subList(i + 2, tokens.size()));
                            expressionstack--;
                            result = BinaryNode(result, tokens.get(i + 1), parsed);
                            i += (masterIndex * 2) - 1;
                        }
                    }
                    break;
                case TRUE, FALSE:
                    result = Boolnode(token);
                    if (expressionstack == 0) {
                        masterIndex = 0;
                    }
                    masterIndex++;
                    if (tokens.size() != 1) {
                        if (checkForBinary(tokens, i)) {
                            expressionstack++;
                            AST parsed = parseExpression(tokens.subList(i + 2, tokens.size()));
                            expressionstack--;
                            result = BinaryNode(result, tokens.get(i + 1), parsed);
                            i += (masterIndex * 2) - 1;
                        }
                    }
                    break;
                case NEW_LINE, ELSE, ELIF:
                    break;
                case PUBLIC, PRIVATE, STATIC:
                    result = Scopenode(tokens, token,i);
                    Continue = false;
                    break;
                case FUNC:
                    result = functionNode(tokens, token, i);
                    Continue = false;
                    break;
                case STRING:
                    result = StringNode(token);
                    if (expressionstack == 0) {
                        masterIndex = 0;
                    }
                    masterIndex++;
                    if (tokens.size() != 1) {
                        if (checkForBinary(tokens, i)) {
                            expressionstack++;
                            AST parsed = parseExpression(tokens.subList(i + 2, tokens.size()));
                            expressionstack--;
                            result = BinaryNode(result, tokens.get(i + 1), parsed);
                            i += (masterIndex * 2) - 1;
                        }
                    }
                    break;
                case RETURN:
                    result = Return(tokens, i);
                    Continue = false;
                    break;
                case NULL:
                    result = new NullNode();
                    break;
                case WHILE:
                    result = whileNode(tokens,token,i);
                    Continue = false;
                    break;
                case IDENTIFIER:
                    if(tokens.size() != 1) {
                        if (tokens.get(i + 1).type == Lexer.TokenType.DOT) {
                            result = AccesserNode(token, tokens, i);
                            Continue = false;
                        }
                        else if (tokens.get(i + 1).type == Lexer.TokenType.LPAREN) {
                            Object[]  obj = functionCallNode(tokens, token, i);
                            result = (AST) obj[0];
                            i = (int) obj[1] + i + 2;
                            if (expressionstack == 0) {
                                masterIndex = 0;
                            }
                            else {
                                masterIndex += ((int) obj[1]) + 2;
                            }
                            if (tokens.size() > i + 2) {
                                if (checkForBinary(tokens, i)) {
                                    expressionstack++;
                                    exprstack++;
                                    AST parsed = parseExpression(tokens.subList(i + 2, tokens.size()));
                                    exprstack--;
                                    result = BinaryNode(result, tokens.get(i + 1), parsed);
                                    i += masterIndex + expressionstack;
                                    if (exprstack == 0){
                                        expressionstack = 0;
                                    }
                                }
                            }
                        }
                        else if (tokens.get( i +1).type == Lexer.TokenType.EQUAL){
                            result = Redeclaringvar(tokens,token,i);
                            Continue = false;
                        }
                        else {
                            result = identifierNode(token);
                            if (expressionstack == 0) {
                                masterIndex = 0;
                            }
                            masterIndex++;
                            if (tokens.size() != 1) {
                                if (checkForBinary(tokens, i)) {
                                    expressionstack++;
                                    AST parsed = parseExpression(tokens.subList(i + 2, tokens.size()));
                                    expressionstack--;
                                    result = BinaryNode(result, tokens.get(i + 1), parsed);
                                    i += (masterIndex * 2) - 1;
                                }
                            }
                        }
                    }else {
                        result = identifierNode(token);
                        if (expressionstack == 0) {
                            masterIndex = 0;
                        }
                        masterIndex++;
                        if (tokens.size() != 1) {
                            if (checkForBinary(tokens, i)) {
                                expressionstack++;
                                AST parsed = parseExpression(tokens.subList(i + 2, tokens.size()));
                                expressionstack--;
                                result = BinaryNode(result, tokens.get(i + 1), parsed);
                                i += (masterIndex * 2) - 1;
                            }
                        }
                    }
            }
            if (!Continue){
                break;
            }
        }

        if (!parens.isEmpty()){
            throw new Error("SyntaxError: Expected "+parens.size()+" ')' at line number:"+tokens.getFirst().linenumber);
        }
        return result;
    }

    private AST notOperator(List<Lexer.Token> tokens,int i) throws IOException, ClassNotFoundException {
        NotNode not = new NotNode();
        not.childNode = parseExpression(tokens.subList(i + 1,tokens.size()));
        return not;
    }

    private AST Redeclaringvar(List<Lexer.Token> tokens, Lexer.Token token, int i)throws IOException, ClassNotFoundException {
        RedeclareVar var = new RedeclareVar();
        var.name = token.lexeme;
        SymbolNode node = new SymbolNode();
        node.type = SymbolTables.VARIABLE_TYPE;
        node.name = var.name;
        node.parentIndex = ActualParentIndex;
        var.ParentIndex = ActualParentIndex;
        if (table.checkAvailability(node)){
            throw new Error("NoVariableFoundError: No such variable found for name '"+var.name+"'");
        }
        var.value = parseExpression(tokens.subList(i+2,tokens.size()));
        return var;
    }

    private Object[] ClassInitialisation(List<Lexer.Token> tokens, Lexer.Token token, int i)throws IOException, ClassNotFoundException {
        token = tokens.get(i+1);
        i = i+1;
        Object[] array = new Object[2];
        ClassInit node = new ClassInit();
        node.name = Objects.requireNonNull(consume(Lexer.TokenType.IDENTIFIER, token, "SyntaxError: Expected name for class variable initialisation at line number:" + token.linenumber)).lexeme;
        node.arg = new ArrayList<>();
        Lexer.Token tok = new Lexer.Token();
        tok.type = Lexer.TokenType.RPAREN;
        SymbolNode symbolNode = new SymbolNode();
        symbolNode.parentIndex = SymbolTables.ALL_CHILD;
        node.ParentIndex = ParentIndex;
        symbolNode.name = node.name;
        symbolNode.type = SymbolTables.CLASS_TYPE;
        if (table.checkAvailability(symbolNode)){
            throw new Error("ClassNotFoundError: Class named '"+node.name+"' not found");
        }
        table.attachChildToParent(ParentIndex,table.findNode(symbolNode));
        i+=2;
        array[1] = getCorrectedIndex(tokens.subList(i,tokens.size()));
        try {
            tokens = tokens.subList(i, getCorrectedIndex(tokens.subList(i,tokens.size()))+i);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("SyntaxError: Expected ')' at line number:"+token.linenumber);
        }
        ArrayList<Integer> index = getIndex(tokens);
        if (index.isEmpty()) {
            node.arg.add(parseExpression(tokens));
        }else {
            for (int z =0;z <= index.size();z++) {
                if (z == 0){
                    int k = index.get(z);
                    node.arg.add(parseExpression(tokens.subList(0,k)));
                }else if(z != index.size()){
                    int k = index.get(z);
                    node.arg.add(parseExpression(tokens.subList(k-1,k)));
                }else {
                    int k = index.getLast();
                    node.arg.add(parseExpression(tokens.subList(k+1,getCorrectedIndex(tokens.subList(k+1,tokens.size())) + (k+1) )));
                }
            }
        }
        array[0] = node;
        return array;
    }

    private AST whileNode(List<Lexer.Token> tokens, Lexer.Token token, int i)throws IOException, ClassNotFoundException {
        consume(Lexer.TokenType.LPAREN,tokens.get(i+1),"SyntaxError: Expected '(' at line number:"+token.linenumber);
        WhileNode node  = new WhileNode();
        node.condition = parseExpression(tokens.subList(i+2,tokens.size()-3));
        node.expr = new ArrayList<>();
        consume(Lexer.TokenType.RPAREN,tokens.get(tokens.size()-3),"SyntaxError: Expected ')' at line number:"+token.linenumber);
        int temp = braceTarget;
        braceTarget = braces.size();
        braces.push(consume(Lexer.TokenType.LBRACE,tokens.get(tokens.size()-2),"SyntaxError: Expected '{' at line number:"+token.linenumber).lexeme);
        SymbolNode newScope = new SymbolNode();
        newScope.type = SymbolTables.OTHER_SCOPE_TYPE;
        newScope.name = String.valueOf(table.Table.size());
        newScope.scope = null;
        newScope.childIndex = new ArrayList<>();
        table.addObject(newScope);
        table.attachChildToParent(ParentIndex,table.Table.size()-1);
        int temporary = ParentIndex;
        ParentIndex = table.Table.size()-1;
        while (braceTarget != braces.size()){
            List<Lexer.Token> nextToken = lexer.nextExpressionToken();
            AST node3 = parseExpression(nextToken);
            if(node3 != null){
                node.expr.add(node3);
            }
        }
        ParentIndex = temporary;
        braceTarget = temp;
        return node;
    }

    private Object[] functionCallNode(List<Lexer.Token> tokens, Lexer.Token token, int i)throws IOException, ClassNotFoundException {
        Object[] array = new Object[2];
        FunctionCall node = new FunctionCall();
        node.name = token.lexeme;
        SymbolNode symbolNode = new SymbolNode();
        symbolNode.parentIndex = ActualParentIndex;
        node.ParentIndex = ActualParentIndex;
        symbolNode.name = node.name;
        symbolNode.type = SymbolTables.FUNCTION_TYPE;
        if (table.checkAvailability(symbolNode)){
            throw new Error("FunctionNotFoundError: Function named '"+node.name+"' not found");
        }
        node.arg = new ArrayList<>();
        Lexer.Token tok = new Lexer.Token();
        tok.type = Lexer.TokenType.RPAREN;
        i+=2;
        array[1] = getCorrectedIndex(tokens.subList(i,tokens.size()));
        try {
            tokens = tokens.subList(i, getCorrectedIndex(tokens.subList(i,tokens.size()))+i);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("SyntaxError: Expected ')' at line number:"+token.linenumber);
        }
        ArrayList<Integer> index = getIndex(tokens);
        if (index.isEmpty()) {
            node.arg.add(parseExpression(tokens));
        }else {
            for (int z =0;z <= index.size();z++) {
                if (z == 0){
                    int k = index.get(z);
                    node.arg.add(parseExpression(tokens.subList(0,k)));
                }else if(z != index.size()){
                    int k = index.get(z);
                    node.arg.add(parseExpression(tokens.subList(k-1,k)));
                }else {
                    int k = index.getLast();
                    node.arg.add(parseExpression(tokens.subList(k+1,getCorrectedIndex(tokens.subList(k+1,tokens.size())) + (k+1) )));
                }
            }
        }
        array[0] = node;
        return array;
    }

    private int getCorrectedIndex(List<Lexer.Token> tokens) {
        Stack<String> parens = new Stack<>();
        for (int z = 0; z<tokens.size();z++){
            if (tokens.get(z).type == Lexer.TokenType.LPAREN){
                parens.push("(");
            }else if (tokens.get(z).type == Lexer.TokenType.RPAREN){
                try {
                    parens.pop();
                }catch (EmptyStackException e){
                    return z;
                }
            }
        }
        return -100;
    }

    private ArrayList<Integer> getIndex(List<Lexer.Token> tokens) {
        ArrayList<Integer> index = new ArrayList<>();
        Stack<String> parens = new Stack<>();
        int j = 0;
        for (Lexer.Token check: tokens){
            if (check.type == Lexer.TokenType.COMMA && parens.isEmpty()){
                index.add(j);
            }else if (check.type == Lexer.TokenType.RPAREN){
                try {
                    parens.pop();
                }catch (EmptyStackException _){}
            } else if (check.type == Lexer.TokenType.LPAREN) {
                parens.push("(");
            }
            j++;
        }
        return index;
    }

    private AST AccesserNode(Lexer.Token token, List<Lexer.Token> tokens, int i)throws IOException, ClassNotFoundException {
        AccessNode node = new AccessNode();
        node.Super = token.lexeme;
        SymbolNode node1 = new SymbolNode();
        SymbolNode node2 = new SymbolNode();
        node1.name = node.Super;
        node1.type = SymbolTables.VARIABLE_TYPE;
        node1.parentIndex = ParentIndex;
        node2.name = node.Super;
        node2.parentIndex = SymbolTables.ALL_CHILD;
        node2.type = SymbolTables.CLASS_TYPE;
        int j = table.findNode(node1,9);
        int j2 = table.findNode(node2,9);
        j = j + j2 + 1;
        identifierNode(token);
        int temp = ParentIndex;
        ParentIndex = j;
        ActualParentIndex = table.Table.get(j).childIndex.getFirst();
        node.var = parseExpression(tokens.subList(i+2,tokens.size()));
        ParentIndex = temp;
        node.selfIndex = j;
        return node;
    }

    private AST Return(List<Lexer.Token> tokens, int i)throws IOException, ClassNotFoundException {
        ReturnNode node = new ReturnNode();
        node.returnValue = parseExpression(tokens.subList(i+1,tokens.size()));
        return node;
    }

    private AST StringNode(Lexer.Token token) {
        LiteralNode node = new LiteralNode();
        node.value = token.lexeme;
        pool.addConstant(node.value);
        return node;
    }

    private AST importNode(List<Lexer.Token> tokens, Lexer.Token token, int i) throws IOException, ClassNotFoundException {
        ImportNode node = new ImportNode();
        node.path = Objects.requireNonNull(consume(Lexer.TokenType.STRING, tokens.get(1), "SyntaxError: File path is not defined for importing at line number:" + token.linenumber)).lexeme;
        String filepath = projectDirectory + "//" + node.path + ".nl";
        table.importTable(filepath,projectDirectory);
        pool.addConstant(node.path);
        return node;
    }

    private AST functionNode(List<Lexer.Token> tokens, Lexer.Token token, int i)throws IOException, ClassNotFoundException {
        FunctionNode node = new FunctionNode();
        consume(Lexer.TokenType.IDENTIFIER,tokens.get(i+1),"SynatxError: Expected name for function at line number:"+token.linenumber);
        node.name = tokens.get(i+1).lexeme;
        node.arguments = new ArrayList<>();
        Lexer.Token tok = new Lexer.Token();
        tok.type = Lexer.TokenType.COMMA;
        Lexer.Token tok2 = new Lexer.Token();
        tok2.type = Lexer.TokenType.RPAREN;
        SymbolNode funcNode = new SymbolNode();
        funcNode.scope.addAll(nextScope);
        funcNode.type = SymbolTables.FUNCTION_TYPE;
        funcNode.name = node.name;
        funcNode.parentIndex = ParentIndex;
        node.ParentIndex = ParentIndex;
        funcNode.childIndex = new ArrayList<>();
        table.addObject(funcNode);
        table.attachChildToParent(ParentIndex,table.Table.size()-1);
        int temporary = ParentIndex;
        ParentIndex = table.Table.size()-1;
        i = i+3;
        List<Lexer.Token> subList = null;
        boolean condtion = true;
        while (condtion){
            if (tokens.indexOf(tok) != -1) {
                subList = tokens.subList(i, (tokens.indexOf(tok)));
            }else if(tokens.indexOf(tok2) != -1){
                subList = tokens.subList(i,(tokens.indexOf(tok2)));
                i = 1000;
                condtion = false;
            }else if(tokens.indexOf(tok2) == -1){
                throw new Error("SyntaxError: Expected ')' at line number:"+token.linenumber);
            }
            HashMap<String,String> arg = new HashMap<>();
            if (subList.size() > 1) {
                String key = Objects.requireNonNull(consume(Lexer.TokenType.IDENTIFIER, subList.get(0), "SyntaxError: Expected type of argument at line number:" + subList.getFirst().linenumber)).lexeme;
                consume(Lexer.TokenType.COLON, subList.get(1), "SyntaxError: Expected ':' after declaring the type of argument at line number:" + subList.getFirst().linenumber);
                String value = Objects.requireNonNull(consume(Lexer.TokenType.IDENTIFIER, subList.size() != 2 ? subList.get(2) : tok2, "SyntaxError: Expected name for argument at line number:" + subList.getFirst().linenumber)).lexeme;
                node.arguments.add(arg);
                SymbolNode argNode = new SymbolNode();
                argNode.childIndex = null;
                argNode.type = SymbolTables.VARIABLE_TYPE;
                argNode.name = value;
                argNode.parentIndex = ParentIndex;
                table.addObject(argNode);
                table.attachChildToParent(ParentIndex,table.Table.size()-1);
                arg.put(key, value);
            }
            if(i != 1000){
                i += 4;
            }else {
                i = tokens.size()-2;
            }
            tokens = tokens.subList(i,tokens.size());
            i = 0;
        }
        int temp = braceTarget;
        braceTarget = braces.size();
        node.expr = new ArrayList<>();
        Stack<String> scope = nextScope;
        nextScope = new Stack<>();
        braces.push(Objects.requireNonNull(consume(Lexer.TokenType.LBRACE, tokens.getFirst(), "SyntaxError: Expected '{' at line number:" + tokens.getFirst().linenumber)).lexeme);
        while(braceTarget != braces.size()){
            List<Lexer.Token> nextToken = lexer.nextExpressionToken();
            AST node3 = parseExpression(nextToken);
            if(node3 != null){
                node.expr.add(node3);
            }
        }
        nextScope = scope;
        braceTarget = temp;
        ParentIndex = temporary;
        return node;
    }

    private boolean checkForBinary(List<Lexer.Token> tokens,int i) {
        boolean condition1 = checkItem(tokens.get(i+1), Arrays.stream(Lexer.TokenType.values()).toList().subList(lexer.indexForSep,lexer.indexForCondtion-1));
        boolean condition2 = checkItem(tokens.get(i+1), Arrays.stream(Lexer.TokenType.values()).toList().subList(Arrays.stream(Lexer.TokenType.values()).toList().indexOf(Lexer.TokenType.LESS_EQUAL), Arrays.stream(Lexer.TokenType.values()).toList().indexOf(Lexer.TokenType.COMMENT)));
        return condition2||condition1;
    }

    private AST Boolnode(Lexer.Token token) {
        BoolNode node = new BoolNode();
        node.value = token.lexeme;
        pool.addConstant(Objects.equals(node.value, "true") ?"1":"0");
        return node;
    }

    private Lexer.Token consume(Lexer.TokenType type, Lexer.Token token, String ErrorMessage){
        if (token.type == type){
            return token;
        }else if (!ErrorMessage.isEmpty()){
            throw new Error(ErrorMessage);
        }
        return null;
    }

    private boolean checkItem(Lexer.Token type, List<Lexer.TokenType> tokenTypes){
        for (Lexer.TokenType tokenType:tokenTypes){
            if (tokenType == type.type){
                return true;
            }

        }
        return false;
    }

    private AST Classnode(List<Lexer.Token> tokens, Lexer.Token token, int i)throws IOException, ClassNotFoundException{
        ClassNode classNode = new ClassNode();
        classNode.name = Objects.requireNonNull(consume(Lexer.TokenType.IDENTIFIER, tokens.get(i + 1), "SyntaxError:Expected class name at line number:" + token.linenumber)).lexeme;
        braces.push(Objects.requireNonNull(consume(Lexer.TokenType.LBRACE, tokens.get(i + 2), "SyntaxError: Expected '{' at line number:" + token.linenumber)).lexeme);
        classNode.expr = new ArrayList<>();
        SymbolNode classSymbol = new SymbolNode();
        classSymbol.name = classNode.name;
        classSymbol.type = SymbolTables.CLASS_TYPE;
        classSymbol.scope.addAll(nextScope);
        classSymbol.parentIndex = ParentIndex;
        classNode.ParentIndex = ParentIndex;
        classSymbol.childIndex = new ArrayList<>();
        table.addObject(classSymbol);
        table.attachChildToParent(ParentIndex,table.Table.size()-1);
        List<Lexer.Token> previousTokens = tokens;
        int temp2 = ParentIndex;
        ParentIndex = table.Table.size()-1;
        Stack<String> scope = nextScope;
        nextScope = new Stack<>();
        while (true){
            List<Lexer.Token> tokenList = lexer.nextExpressionToken();
            if (tokenList == null && !braces.isEmpty()){
                throw new Error("SyntaxError: Expected '}' at line number:"+previousTokens.getFirst().linenumber);
            }
            previousTokens = tokenList;
            AST node2 = parseExpression(tokenList);
            if (node2 != null) {
                classNode.expr.add(node2);
            }
            if(braces.isEmpty()){
                break;
            }
        }
        nextScope = scope;
        ParentIndex = temp2;
        return classNode;
    }

    private AST BinaryNode(AST value1, Lexer.Token token,AST value2) {
        BinaryExpressionNode node = new BinaryExpressionNode();
        node.value1 = value1;
        node.operation = token.lexeme;
        node.value2 = value2;
        return node;
    }

    private AST identifierNode(Lexer.Token token) {
        IdentifierNode node = new IdentifierNode();
        node.value = token.lexeme;
        node.parentIndex = ParentIndex;
        SymbolNode symbolNode = new SymbolNode();
        symbolNode.name = node.value;
        symbolNode.parentIndex = ParentIndex;
        SymbolNode symbolNode1 = new SymbolNode();
        SymbolNode symbolNode2 = new SymbolNode();
        symbolNode1.name = node.value;
        symbolNode1.parentIndex = ParentIndex;
        symbolNode2.name = node.value;
        symbolNode2.parentIndex = ParentIndex;
        symbolNode.type = SymbolTables.CLASS_TYPE;
        symbolNode1.type = SymbolTables.VARIABLE_TYPE;
        symbolNode2.type = SymbolTables.FUNCTION_TYPE;
        if (table.checkAvailability(symbolNode) && table.checkAvailability(symbolNode1) && table.checkAvailability(symbolNode2)){
            throw new Error("SymbolNotFoundError: Symbol named '"+symbolNode.name+"' not found at line number:"+token.linenumber);
        }
        return node;
    }

    private AST Declarationnode(List<Lexer.Token> tokens, Lexer.Token token, int i)throws IOException, ClassNotFoundException{
        DeclarationNode node = new DeclarationNode();
        node.name = Objects.requireNonNull(consume(Lexer.TokenType.IDENTIFIER, tokens.get(i + 1), "SyntaxError: Expected name for declaration at line number:" + token.linenumber)).lexeme;
        SymbolNode var = new SymbolNode();
        var.parentIndex = ParentIndex;
        node.ParentIndex = ParentIndex;
        var.type = SymbolTables.VARIABLE_TYPE;
        var.name = node.name;
        for (int j = 0; j < nextScope.size(); j++) {
            var.scope.add(nextScope.get(j));
        }
        table.addObject(var);
        table.attachChildToParent(ParentIndex,table.Table.size()-1);
        int temp = ParentIndex;
        ParentIndex = table.Table.size()-1;
        if(consume(Lexer.TokenType.EQUAL,tokens.get(i+2),"SyntaxError: Expected '=' or new line for declaration") != null) {
            List<Lexer.Token> value = tokens.subList(i + 3, tokens.size());
            node.exp = parseExpression(value);
            if (node.exp == null) {
                throw new Error("SyntaxError: Expected value for variable '" + node.name + "' at line number:" + token.linenumber);
            }
        }
        ParentIndex = temp;
        return node;
    }

    private AST Numbernode(List<Lexer.Token> tokens,Lexer.Token token,int i)throws IOException, ClassNotFoundException{
        NumberNode num = new NumberNode();
        try {
            num.value = Objects.requireNonNull(consume(Lexer.TokenType.NUMBER, token, "")).lexeme;
            if (tokens.size()<= i+1){
                pool.addConstant(num.value);
                return num;
            }
            if(consume(Lexer.TokenType.DOT,tokens.get(i+1),"") != null) {
                try {
                    String str1 = Objects.requireNonNull(consume(Lexer.TokenType.NUMBER, token, "")).lexeme;
                    String str2 = Objects.requireNonNull(consume(Lexer.TokenType.NUMBER, tokens.get(i + 2), "")).lexeme;
                    num.value = str1 + "." + str2;

                }catch (Exception _){}
            }
            pool.addConstant(num.value);
        }catch (Exception e){
            ArrayList<Lexer.Token> tokens1 = new ArrayList<>();
            Lexer.Token token1 = new Lexer.Token();
            token1.type = Lexer.TokenType.IDENTIFIER;
            token1.lexeme = Objects.requireNonNull(consume(Lexer.TokenType.NUMBER, token, "")).lexeme;
            token1.linenumber = token.linenumber;
            tokens1.add(token1);
            return parseExpression(tokens1);
        }
        return num;
    }

    private AST Scopenode(List<Lexer.Token> tokens,Lexer.Token token,int i)throws IOException, ClassNotFoundException{
        ScopeNode scope = new ScopeNode();
        scope.scope = token.lexeme;
        nextScope.push(token.lexeme);
        int temp = braceTarget;
        braceTarget = braces.size();
        braces.push(Objects.requireNonNull(consume(Lexer.TokenType.LBRACE, tokens.get(i + 1), "SyntaxError: Expected '{' at line number:" + token.linenumber)).lexeme);
        List<AST> result = new ArrayList<>();
        while (braceTarget != braces.size()) {
            List<Lexer.Token> nextExpr = lexer.nextExpressionToken();
            AST parsed = parseExpression(nextExpr);
            if (parsed != null) {
                result.add(parsed);
            }
        }
        nextScope.pop();
        braceTarget = temp;
        scope.node = result;
        return scope;
    }

    private AST IFnode(List<Lexer.Token> tokens, Lexer.Token token, int i, Stack<String> braces)throws IOException, ClassNotFoundException {
        List<Lexer.Token> nextExpr = null;
        int temp = braceTarget;
        braceTarget = braces.size();
        IFNode node = new IFNode();
        node.expr = new ArrayList<>();
        SymbolNode newScope = new SymbolNode();
        newScope.type = SymbolTables.OTHER_SCOPE_TYPE;
        newScope.name = String.valueOf(table.Table.size());
        newScope.scope = null;
        newScope.parentIndex = ParentIndex;
        newScope.childIndex = new ArrayList<>();
        nextScope = new Stack<>();
        table.addObject(newScope);
        table.attachChildToParent(ParentIndex,table.Table.size()-1);
        int temporary = ParentIndex;
        ParentIndex = table.Table.size()-1;
        consume(Lexer.TokenType.LPAREN,tokens.get(i+1),"SyntaxError: Expected '(' at line number:"+ token.linenumber);
        consume(Lexer.TokenType.RPAREN,tokens.get(tokens.size() - 3),"SyntaxError: Expected ')' at line number:"+ token.linenumber);
        node.condition = parseExpression(tokens.subList(i+2, tokens.size() - 3));
        braces.push(Objects.requireNonNull(consume(Lexer.TokenType.LBRACE, tokens.get(tokens.size() - 2), "SyntaxError: Expected '{' at line number:" + token.linenumber)).lexeme);
        while (braceTarget != braces.size()) {
             nextExpr = lexer.nextExpressionToken();
             AST parsed = parseExpression(nextExpr);
             if (parsed != null) {
                 node.expr.add(parsed);
             }
        }
        node.ElifCondition = new ArrayList<>();
        node.elifexpr = new ArrayList<>();
        ParentIndex = temporary;
        braceTarget = temp;
        while (true) {
            boolean Continue = false;
            if (nextExpr.get(1).type == Lexer.TokenType.ELIF || lexer.peekAtLine(nextExpr.getFirst().linenumber+1).getFirst().type == Lexer.TokenType.ELIF) {
                tokens = nextExpr.subList(1,nextExpr.size());
                SymbolNode newScope2 = new SymbolNode();
                newScope2.type = SymbolTables.OTHER_SCOPE_TYPE;
                newScope2.name = String.valueOf(table.Table.size());
                newScope2.parentIndex = ParentIndex;
                newScope2.scope = null;
                newScope2.childIndex = new ArrayList<>();
                nextScope = new Stack<>();
                table.addObject(newScope2);
                table.attachChildToParent(ParentIndex,table.Table.size()-1);
                int temporary2 = ParentIndex;
                ParentIndex = table.Table.size()-1;
                if ( lexer.peekAtLine(nextExpr.getFirst().linenumber+1).getFirst().type == Lexer.TokenType.ELIF){
                    tokens = lexer.peekAtLine(nextExpr.getFirst().linenumber+1);
                    lexer.nextExpressionToken();
                }
                temp = braceTarget;
                braceTarget = braces.size();
                consume(Lexer.TokenType.LPAREN,tokens.get(i+1),"SyntaxError: Expected '(' at line number:"+ token.linenumber);
                consume(Lexer.TokenType.RPAREN,tokens.get(tokens.size() - 3),"SyntaxError: Expected ')' at line number:"+ token.linenumber);
                node.ElifCondition.add(parseExpression(tokens.subList(2, tokens.size() - 3)));
                List<AST> pars = null;
                braces.push(Objects.requireNonNull(consume(Lexer.TokenType.LBRACE, tokens.get(tokens.size() - 2), "SyntaxError: Expected '{' at line number:" + tokens.getFirst().linenumber)).lexeme);
                while (braceTarget != braces.size()) {
                    nextExpr = lexer.nextExpressionToken();
                    AST parsed = parseExpression(nextExpr);
                    if (parsed != null) {
                        pars = new ArrayList<>();
                        pars.add(parsed);
                    }
                }
                node.elifexpr.add(pars);
                ParentIndex = temporary2;
                braceTarget = temp;
                Continue = true;
            }else if (nextExpr.get(1).type == Lexer.TokenType.ELSE || lexer.peekAtLine(nextExpr.getFirst().linenumber+1).getFirst().type == Lexer.TokenType.ELSE) {
                tokens = nextExpr.subList(1,nextExpr.size());
                SymbolNode newScope2 = new SymbolNode();
                newScope2.type = SymbolTables.OTHER_SCOPE_TYPE;
                newScope2.name = String.valueOf(table.Table.size());
                newScope2.scope = null;
                newScope2.parentIndex = ParentIndex;
                newScope2.childIndex = new ArrayList<>();
                nextScope = new Stack<>();
                table.addObject(newScope2);
                table.attachChildToParent(ParentIndex,table.Table.size()-1);
                int temporary2 = ParentIndex;
                ParentIndex = table.Table.size()-1;
                if ( lexer.peekAtLine(nextExpr.getFirst().linenumber+1).getFirst().type == Lexer.TokenType.ELSE){
                    tokens = lexer.peekAtLine(nextExpr.getFirst().linenumber+1);
                    lexer.nextExpressionToken();
                }
                temp = braceTarget;
                braceTarget = braces.size();
                node.elseexpr = new ArrayList<>();
                braces.push(Objects.requireNonNull(consume(Lexer.TokenType.LBRACE, tokens.get(tokens.size() - 2), "SyntaxError: Expected '{' at line number:" + tokens.getFirst().linenumber)).lexeme);
                while (braceTarget != braces.size()) {
                    nextExpr = lexer.nextExpressionToken();
                    AST parsed = parseExpression(nextExpr);
                    if (parsed != null) {
                        node.elseexpr.add(parsed);
                    }
                }
                braceTarget = temp;
                ParentIndex = temporary2;
            }
            if (!Continue){
                break;
            }
        }
        return node;
    }

}