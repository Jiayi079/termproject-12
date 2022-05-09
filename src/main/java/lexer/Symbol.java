package lexer;

import java.util.HashMap;
import java.util.Map;

/**
 * The Symbol class is used to store all user strings along with
 * an indication of the kind of strings they are; e.g. the id "abc" will
 * store the "abc" in name and Sym.Tokens.Identifier in kind
 */
public class Symbol {

  private String name;
  private TokenTypes kind;   // token kind of symbol

  public Symbol(String n, TokenTypes kind) {
    name = n;
    this.kind = kind;
  }

  // symbols contains all strings in the source program
  private static Map<String, Symbol> symbols = new HashMap<>();

  public String toString() {
    return name;
  }

  public TokenTypes getKind() {
    return kind;
  }

  /**
   * Return the unique symbol associated with a string.
   * Repeated calls to <tt>symbol("abc")</tt> will return the same Symbol.
   */
  public static Symbol symbol(String newTokenString, TokenTypes kind) {
    Symbol s = symbols.get(newTokenString);
    if (s == null) {
      if (kind == TokenTypes.BogusToken) {  // bogus string so don't enter into symbols
        return null;
      }
      //System.out.println("new symbol: "+u+" Kind: "+kind);
      s = new Symbol(newTokenString, kind);
      symbols.put(newTokenString, s);
    }
    return s;
  }

  public static void initSymbols() {
    Symbol.symbol("program", TokenTypes.Program);
    Symbol.symbol("int", TokenTypes.Int);
    Symbol.symbol("boolean", TokenTypes.BOOLean);
    Symbol.symbol("if", TokenTypes.If);
    Symbol.symbol("then", TokenTypes.Then);
    Symbol.symbol("else", TokenTypes.Else);
    Symbol.symbol("while", TokenTypes.While);
    Symbol.symbol("function", TokenTypes.Function);
    Symbol.symbol("return", TokenTypes.Return);
    Symbol.symbol("<id>", TokenTypes.Identifier);
    Symbol.symbol("<int>", TokenTypes.INTeger);
    Symbol.symbol("{", TokenTypes.LeftBrace);
    Symbol.symbol("}", TokenTypes.RightBrace);
    Symbol.symbol("(", TokenTypes.LeftParen);
    Symbol.symbol(")", TokenTypes.RightParen);
    Symbol.symbol(",", TokenTypes.Comma);
    Symbol.symbol("=", TokenTypes.Assign);
    Symbol.symbol("==", TokenTypes.Equal);
    Symbol.symbol("!=", TokenTypes.NotEqual);
    Symbol.symbol("<", TokenTypes.Less);
    Symbol.symbol("<=", TokenTypes.LessEqual);
    Symbol.symbol("+", TokenTypes.Plus);
    Symbol.symbol("-", TokenTypes.Minus);
    Symbol.symbol("|", TokenTypes.Or);
    Symbol.symbol("&", TokenTypes.And);
    Symbol.symbol("*", TokenTypes.Multiply);
    Symbol.symbol("/", TokenTypes.Divide);
    Symbol.symbol("//", TokenTypes.Comment);
  }
}

