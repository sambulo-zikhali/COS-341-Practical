package spl.lexer;

import spl.model.Token;
import spl.model.TokenType;
import java.util.List;

public class LexerTest {
    public static void main(String[] args) {
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.tokenize("#x #y \nvoid #printsum ( #a #b ) { \n#result = add ( #a #b ) ; \nprint ( #result ) ; \nreturn \n} \n#x = 10 \n#y = 20 \n#printsum ( #x #y ) ; \nprint \"done\" ; ");

        for (Token t : tokens) {
            System.out.println(t);
        }
    }
}
