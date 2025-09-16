package Compiler;

import java.util.*;

public class Lexer {

    String code;
    Scanner scanner;
    int linenumber = 1;

    public static class Token{
        TokenType type;
        String lexeme;
        int linenumber;

        @Override
        public String toString() {
            return "Type:" + type.toString() + " Lexeme:"+ lexeme+" Line Number:"+linenumber;
        }

        @Override
        public boolean equals(Object obj) {
            return ((Token)obj).type == this.type;
        }
    }
    enum TokenType {
        LET,IF,ELSE,ELIF,IMPORT,NEW,WHILE,CLASS,STATIC,PUBLIC,PRIVATE,RETURN,FUNC,TRUE,FALSE,NULL,IDENTIFIER,
        NUMBER,STRING,PLUS,MINUS,
        STAR,SLASH,MODULO,GREATER,LESS,EQUAL,AND,OR,NOT,LPAREN,RPAREN,LBRACE,RBRACE,
        COMMA,DOT,COLON,NEW_LINE,LESS_EQUAL,GREATER_EQUAL,EQ_EQUAL,NOT_EQUAL,INCREAMENT,DECREAMENT,COMMENT
    }
    public int indexForSep = Arrays.stream(TokenType.values()).toList().indexOf(TokenType.PLUS);
    public int indexForCondtion = Arrays.stream(TokenType.values()).toList().indexOf(TokenType.OR);
    String[] lexeme = {
            "let","if","else","elif","import","new","while","class","static","public","private","return","func","true","false","null","","","",
            "+","-","*","/","%",">","<","=","&&","||","!","(",")","{","}",",",".",":","\n"
    };
    String[] conditionalLexeme = {
      "<=",">=","==","!=", "++", "--","//"
    };

    public Lexer(String content){
        this.code = content;
        scanner = new Scanner(this.code);
    }

    public ArrayList<Token> nextExpressionToken(){
        ArrayList<Token> tokens = new ArrayList<>();
        if(scanner.hasNextLine()){
            String expr = scanner.nextLine();
            boolean cond = true;
            while (cond){
                String word;
                boolean needTrim = true;
                if(expr.contains(" ") && !expr.contains(String.valueOf('"'))) {
                    word = expr.substring(0, expr.indexOf(" "));
                    expr = expr.substring(expr.indexOf(" ") + 1);
                } else if (expr.contains(String.valueOf('"'))) {
                    int startIndex = expr.indexOf('"');
                    if (startIndex == 0){
                        needTrim = false;
                        word = expr.substring(0,expr.substring(1).indexOf('"')+2);
                        expr = expr.substring(expr.substring(1).indexOf('"')+2);
                    }else {
                        word = expr.substring(0, startIndex);
                        expr = expr.substring(startIndex);
                    }
                }else{
                    word = expr;
                    cond = false;
                }
                word = word.replace("\t","");
                if (needTrim){
                    word = word.trim();
                }
                if (((checkStr(word) > -1 || checkStr(word.trim()) > -1) && !word.trim().isEmpty()) ){
                        addToken(TokenType.values()[checkStr(word)], word, linenumber, tokens);
                }else if(!word.trim().isEmpty()){
                    if (checkStr(word) == -2){
                        boolean insideString = false;
                        ArrayList<Integer> index = new ArrayList<>();
                        for (int i = 0; i < word.length(); i++) {
                            String o = String.valueOf(word.toCharArray()[i]);
                            if (Arrays.stream(lexeme).toList().contains(o) && !(o.equals(String.valueOf('"')) || insideString)){
                                    index.add(i);
                            }else if (o.equals(String.valueOf('"'))){
                                insideString = !insideString;
                            }

                        }
                        ArrayList<String> newWords = new ArrayList<>();
                        newWords.add(word);
                        if (!index.isEmpty()) {
                            newWords = getNewWords(index, word, cond);
                        }
                        for (int k = 0;k<newWords.size();k++) {
                            String s = newWords.get(k);

                            if(Arrays.stream(lexeme).toList().contains(s) && ((k + 1) < newWords.size() && Arrays.stream(lexeme).toList().contains(newWords.get(k + 1)))){
                                String s1 = s + newWords.get(k+1);
                                if (checkList(s1, Arrays.stream(conditionalLexeme).toList()) != -1){
                                    if (s1.equals("//")){
                                        addToken(TokenType.NEW_LINE,"\n",linenumber,tokens);
                                        return tokens;
                                    }
                                    s = s1;
                                    k++;
                                }
                            }

                            if (checkStr(s) > -1){
                                addToken(TokenType.values()[checkStr(s)],s,linenumber,tokens);
                            } else if (s.contains(String.valueOf('"'))) {
                                addToken(TokenType.STRING, s.substring(s.indexOf('"')+1, s.substring(s.indexOf('"')+1).indexOf('"')+1), linenumber, tokens);
                            } else if (s.matches(".*\\d.*") && !(s.matches(".*[a-zA-Z].*"))) {
                                addToken(TokenType.NUMBER, s, linenumber, tokens);
                            }else if (Arrays.stream(conditionalLexeme).toList().contains(s)){
                                addToken(TokenType.values()[checkList(s, List.of(conditionalLexeme))+ lexeme.length],conditionalLexeme[checkList(s, List.of(conditionalLexeme))],linenumber,tokens);
                            } else {
                                addToken(TokenType.IDENTIFIER, s, linenumber, tokens);
                            }
                        }
                    }else {
                        if (word.contains(String.valueOf('"'))) {
                            addToken(TokenType.STRING, word.substring(word.indexOf('"')+1, word.substring(word.indexOf('"') + 1).indexOf('"')+1), linenumber, tokens);
                        } else if (word.matches(".*\\d.*") && !(word.matches(".*[a-zA-Z].*"))) {
                            addToken(TokenType.NUMBER, word, linenumber, tokens);
                        } else {
                            addToken(TokenType.IDENTIFIER, word, linenumber, tokens);
                        }
                    }
                }
            }
            addToken(TokenType.NEW_LINE,"\n",linenumber,tokens);
        }else {
            return null;
        }
        linenumber++;
        return tokens;
    }

    public ArrayList<Token> peekAtLine(int linenumber){
        linenumber = linenumber -1;
        if (!(linenumber >= code.split("\\R").length || linenumber <0)) {
            String expr = code.split("\\R")[linenumber];
            ArrayList<Token> tokens = new ArrayList<>();
            boolean cond = true;
            while (cond) {
                String word;

                if (expr.contains(" ")) {
                    word = expr.substring(0, expr.indexOf(" ")).trim();
                    expr = expr.substring(expr.indexOf(" ") + 1);
                } else {
                    word = expr.trim();
                    cond = false;
                }
                word = word.replace("\t", "");
                if (checkStr(word) > -1 && !word.trim().isEmpty()) {
                    addToken(TokenType.values()[checkStr(word)], word, linenumber, tokens);
                } else if (!word.trim().isEmpty()) {
                    if (checkStr(word) == -2) {
                        ArrayList<Integer> index = new ArrayList<>();
                        for (int i = 0; i < word.length(); i++) {
                            String o = String.valueOf(word.toCharArray()[i]);
                            if (Arrays.stream(lexeme).toList().contains(o)) {
                                index.add(i);
                            }

                        }

                        ArrayList<String> newWords = getNewWords(index, word, cond);
                        for (int k = 0; k < newWords.size(); k++) {
                            String s = newWords.get(k);

                            if (Arrays.stream(lexeme).toList().contains(s) && ((k + 1) < newWords.size() && Arrays.stream(lexeme).toList().contains(newWords.get(k + 1)))) {
                                String s1 = s + newWords.get(k + 1);
                                if (Arrays.stream(conditionalLexeme).toList().contains(s1)) {
                                    if (s1.equals("//")) {
                                        addToken(TokenType.NEW_LINE, "\n", linenumber, tokens);
                                        return tokens;
                                    }
                                    s = s1;
                                    k++;
                                }
                            }

                            if (checkStr(s) > -1) {
                                addToken(TokenType.values()[checkStr(s)], s, linenumber, tokens);
                            } else if (s.contains(String.valueOf('"'))) {
                                addToken(TokenType.STRING, s.substring(s.indexOf('"') + 1, s.substring(s.indexOf('"') + 1).indexOf('"') + 1), linenumber, tokens);
                            } else if (s.matches(".*\\d.*") && !(s.matches(".*[a-zA-Z].*"))) {
                                addToken(TokenType.NUMBER, s, linenumber, tokens);
                            } else if (Arrays.stream(conditionalLexeme).toList().contains(s)) {
                                addToken(TokenType.values()[checkList(s, Arrays.stream(conditionalLexeme).toList()) + lexeme.length], conditionalLexeme[checkList(s, List.of(conditionalLexeme))], linenumber, tokens);
                            } else {
                                addToken(TokenType.IDENTIFIER, s, linenumber, tokens);
                            }
                        }
                    } else {

                        if (word.contains(String.valueOf('"'))) {
                            addToken(TokenType.STRING, word.substring(word.indexOf('"') + 1, word.substring(word.indexOf('"') + 1).indexOf('"') + 1), linenumber, tokens);
                        } else if (word.matches(".*\\d.*") && !(word.matches(".*[a-zA-Z].*"))) {
                            addToken(TokenType.NUMBER, word, linenumber, tokens);
                        } else {
                            addToken(TokenType.IDENTIFIER, word, linenumber, tokens);
                        }
                    }
                }
            }
            addToken(TokenType.NEW_LINE, "\n", linenumber, tokens);
            return tokens;
        }else {
            return addToken(TokenType.NEW_LINE, "\n", linenumber, new ArrayList<>());
        }
    }

    public int checkList(String s, List<String> list){
        int i = 0;
        for (String s1:list){
            if (s1.equals(s)){
                return i;
            }
            i++;
        }
        return -1;
    }

    private ArrayList<String> getNewWords(ArrayList<Integer> index,String word,boolean cond){
        ArrayList<String> newWords = new ArrayList<>();

        for (int j = 0;j<=index.size();j++){
            if (j != index.size()){
                String wordsub = word.substring(j == 0? 0:index.get(j-1) + 1,index.get(j));
                if (!wordsub.isEmpty()){
                    newWords.add(wordsub);
                }
                newWords.add(String.valueOf(word.charAt(index.get(j))));
            }else {
                String wordsub = word.substring(index.get(j-1)+1);
                if (!wordsub.isEmpty()){
                    newWords.add(wordsub);
                }
            }
        }
        return newWords;
    }

    private ArrayList<Token> addToken(TokenType tokenType,String lexeme,int linenumber,ArrayList<Token> tokens){
        Token token = new Token();
        token.lexeme= lexeme;
        token.type= tokenType;
        token.linenumber=linenumber;
        tokens.add(token);
        return tokens;
    }

    private int checkStr(String word){
        for (int i = 0;i<lexeme.length;i++) {
            if (word.contains(lexeme[i]) && !(word.equals(lexeme[i])) && !Objects.equals(lexeme[i], "") && i>=indexForSep){
                return -2;
            }
            if (Objects.equals(lexeme[i], word)) {
                return i;
            }
        }
        return -1;
    }
}