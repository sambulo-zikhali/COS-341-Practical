package spl.parser;

import java.util.List;

import spl.lexer.Lexer;
import spl.model.Token;

public class ParserTest {
    public static void main(String[] args) {
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.tokenize(
                "#x #y \nvoid #printsum ( #a #b ) { \n#result = add ( #a #b ) ; \nprint ( #result ) ; \nreturn \n} \n#x = 10 \n#y = 20 \n#printsum ( #x #y ) ; \nprint \"done\" ; ");
    }
}
