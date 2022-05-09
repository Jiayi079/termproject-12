package parser;

import java.util.*;
import lexer.*;
import ast.*;

/**
 * The Parser class performs recursive-descent parsing; as a
 * by-product it will build the <b>Abstract Syntax Tree</b> representation
 * for the source program<br>
 * Following is the Grammar we are using:<br>
 * <pre>
 *  PROGRAM -> �program� BLOCK ==> program
 *
 *  BLOCK -> �{� D* S* �}�  ==> block
 *
 *  D -> TYPE NAME                    ==> decl
 *    -> TYPE NAME FUNHEAD BLOCK      ==> functionDecl
 *
 *  TYPE  ->  �int�
 *        ->  �boolean�
 *
 *  FUNHEAD  -> '(' (D list ',')? ')'  ==> formals<br>
 *
 *  S -> �if� E �then� BLOCK �else� BLOCK  ==> if
 *    -> �while� E BLOCK               ==> while
 *    -> �return� E                    ==> return
 *    -> BLOCK
 *    -> NAME �=� E                    ==> assign<br>
 *
 *  E -> SE
 *    -> SE �==� SE   ==> =
 *    -> SE �!=� SE   ==> !=
 *    -> SE �<�  SE   ==> <
 *    -> SE �<=� SE   ==> <=
 *
 *  SE  ->  T
 *      ->  SE �+� T  ==> +
 *      ->  SE �-� T  ==> -
 *      ->  SE �|� T  ==> or
 *
 *  T  -> F
 *     -> T �*� F  ==> *
 *     -> T �/� F  ==> /
 *     -> T �&� F  ==> and
 *
 *  F  -> �(� E �)�
 *     -> NAME
 *     -> <int>
 *     -> NAME '(' (E list ',')? ')' ==> call<br>
 *
 *  NAME  -> <id>
 *  </pre>
 */
public class Parser {

  private Token currentToken;
  private Lexer lex;
  private EnumSet<TokenTypes> relationalOps =
      EnumSet.of(TokenTypes.Equal, TokenTypes.NotEqual, TokenTypes.Less, TokenTypes.LessEqual);
  private EnumSet<TokenTypes> addingOps =
      EnumSet.of(TokenTypes.Plus, TokenTypes.Minus, TokenTypes.Or);
  private EnumSet<TokenTypes> multiplyingOps =
      EnumSet.of(TokenTypes.Multiply, TokenTypes.Divide, TokenTypes.And);

  /**
   * Construct a new Parser;
   *
   * @param sourceProgram - source file name
   * @throws Exception - thrown for any problems at startup (e.g. I/O)
   */
  public Parser(String sourceProgram) throws Exception {
    try {
      lex = new Lexer(sourceProgram);
      scan();
    } catch (Exception e) {
      System.out.println("********exception*******" + e.toString());
      throw e;
    }
  }

  public Lexer getLex() {
    return lex;
  }

  /**
   * Execute the parse command
   *
   * @return the AST for the source program
   * @throws Exception - pass on any type of exception raised
   */
  public AST execute() throws Exception {
    try {
      return rProgram();
    } catch (SyntaxError e) {
      e.print();
      throw e;
    }
  }

  /**
   * <pre>
   *  Program -> 'program' block ==> program
   *  </pre>
   *
   * @return the program tree
   * @throws SyntaxError - thrown for any syntax error
   */
  public AST rProgram() throws SyntaxError {
    // note that rProgram actually returns a ProgramTree; we use the
    // principle of substitutability to indicate it returns an AST
    AST t = new ProgramTree();
    expect(TokenTypes.Program);
    t.addKid(rBlock());
    return t;
  }

  /**
   * <pre>
   *  block -> '{' d* s* '}'  ==> block
   *  </pre>
   *
   * @return block tree
   * @throws SyntaxError - thrown for any syntax error
   *                     e.g. an expected left brace isn't found
   */
  public AST rBlock() throws SyntaxError {
    expect(TokenTypes.LeftBrace);
    AST t = new BlockTree();
    while (true) {  // get decls
      try {
        t.addKid(rDecl());
      } catch (SyntaxError e) {
        break;
      }
    }
    while (true) {  // get statements
      try {
        t.addKid(rStatement());
      } catch (SyntaxError e) {
        break;
      }
    }
    expect(TokenTypes.RightBrace);
    return t;
  }

  /**
   * <pre>
   *  d -> type name                 ==> decl
   *    -> type name funcHead block  ==> functionDecl
   *  </pre>
   *
   * @return either the decl tree or the functionDecl tree
   * @throws SyntaxError - thrown for any syntax error
   */
  public AST rDecl() throws SyntaxError {
    AST t, t1;
    t = rType();
    t1 = rName();
    if (isNextTok(TokenTypes.LeftParen)) { // function
      t = (new FunctionDeclTree()).addKid(t).addKid(t1);
      t.addKid(rFunHead());
      t.addKid(rBlock());
      return t;
    }
    t = (new DeclTree()).addKid(t).addKid(t1);
    return t;
  }

  /**
   * <pre>
   *  type  ->  'int'
   *  type  ->  'bool'
   *  </pre>
   *
   * @return either the intType or boolType tree
   * @throws SyntaxError - thrown for any syntax error
   */
  public AST rType() throws SyntaxError {
    AST t;
    if (isNextTok(TokenTypes.Int)) {
      t = new IntTypeTree();
      scan();
    } else {
      expect(TokenTypes.BOOLean);
      t = new BoolTypeTree();
    }
    return t;
  }

  /**
   * <pre>
   *  funHead  ->  '(' (decl list ',')? ')'  ==> formals
   *  note a funhead is a list of zero or more decl's
   *  separated by commas, all in parens
   *  </pre>
   *
   * @return the formals tree describing this list of formals
   * @throws SyntaxError - thrown for any syntax error
   */
  public AST rFunHead() throws SyntaxError {
    AST t = new FormalsTree();
    expect(TokenTypes.LeftParen);
    if (!isNextTok(TokenTypes.RightParen)) {
      do {
        t.addKid(rDecl());
        if (isNextTok(TokenTypes.Comma)) {
          scan();
        } else {
          break;
        }
      } while (true);
    }
    expect(TokenTypes.RightParen);
    return t;
  }

  /**
   * <pre>
   *      S -> 'if' e 'then' block 'else' block  ==> if
   *        -> 'while' e block  ==> while
   *        -> 'return' e  ==> return
   *        -> block
   *        -> name '=' e  ==> assign
   *  </pre>
   *
   * @return the tree corresponding to the statement found
   * @throws SyntaxError - thrown for any syntax error
   */
  public AST rStatement() throws SyntaxError {
    AST t;
    if (isNextTok(TokenTypes.If)) {
      scan();
      t = new IfTree();
      t.addKid(rExpr());
      expect(TokenTypes.Then);
      t.addKid(rBlock());
      expect(TokenTypes.Else);
      t.addKid(rBlock());
      return t;
    }
    if (isNextTok(TokenTypes.While)) {
      scan();
      t = new WhileTree();
      t.addKid(rExpr());
      t.addKid(rBlock());
      return t;
    }
    if (isNextTok(TokenTypes.Return)) {
      scan();
      t = new ReturnTree();
      t.addKid(rExpr());
      return t;
    }
    if (isNextTok(TokenTypes.LeftBrace)) {
      return rBlock();
    }
    t = rName();
    t = (new AssignTree()).addKid(t);
    expect(TokenTypes.Assign);
    t.addKid(rExpr());
    return t;
  }

  /**
   * <pre>
   *      e -> se
   *        -> se '==' se  ==> =
   *        -> se '!=' se  ==> !=
   *        -> se '<'  se  ==> <
   *        -> se '<=' se  ==> <=
   *  </pre>
   *
   * @return the tree corresponding to the expression
   * @throws SyntaxError - thrown for any syntax error
   */
  public AST rExpr() throws SyntaxError {
    AST t, kid = rSimpleExpr();
    t = getRelationTree();
    if (t == null) {
      return kid;
    }
    t.addKid(kid);
    t.addKid(rSimpleExpr());
    return t;
  }

  /**
   * <pre>
   *      se  ->  t
   *          ->  se '+' t  ==> +
   *          ->  se '-' t  ==> -
   *          ->  se '|' t  ==> or
   *  This rule indicates we should pick up as many <i>t</i>'s as
   *  possible; the <i>t</i>'s will be left associative
   *  </pre>
   *
   * @return the tree corresponding to the adding expression
   * @throws SyntaxError - thrown for any syntax error
   */
  public AST rSimpleExpr() throws SyntaxError {
    AST t, kid = rTerm();
    while ((t = getAddOperTree()) != null) {
      t.addKid(kid);
      t.addKid(rTerm());
      kid = t;
    }
    return kid;
  }

  /**
   * <pre>
   *      t  -> f
   *         -> t '*' f  ==> *
   *         -> t '/' f  ==> /
   *         -> t '&' f  ==> and
   *  This rule indicates we should pick up as many <i>f</i>'s as
   *  possible; the <i>f</i>'s will be left associative
   *  </pre>
   *
   * @return the tree corresponding to the multiplying expression
   * @throws SyntaxError - thrown for any syntax error
   */
  public AST rTerm() throws SyntaxError {
    AST t, kid = rFactor();
    while ((t = getMultOperTree()) != null) {
      t.addKid(kid);
      t.addKid(rFactor());
      kid = t;
    }
    return kid;
  }

  /**
   * <pre>
   *      f  -> '(' e ')'
   *         -> name
   *         -> <int>
   *         -> name '(' (e list ',')? ')' ==> call
   *  </pre>
   *
   * @return the tree corresponding to the factor expression
   * @throws SyntaxError - thrown for any syntax error
   */
  public AST rFactor() throws SyntaxError {
    AST t;
    if (isNextTok(TokenTypes.LeftParen)) { // -> (e)
      scan();
      t = rExpr();
      expect(TokenTypes.RightParen);
      return t;
    }
    if (isNextTok(TokenTypes.INTeger)) {  //  -> <int>
      t = new IntTree(currentToken);
      scan();
      return t;
    }
    t = rName();
    if (!isNextTok(TokenTypes.LeftParen)) {  //  -> name
      return t;
    }
    scan();     // -> name '(' (e list ',')? ) ==> call
    t = (new CallTree()).addKid(t);
    if (!isNextTok(TokenTypes.RightParen)) {
      do {
        t.addKid(rExpr());
        if (isNextTok(TokenTypes.Comma)) {
          scan();
        } else {
          break;
        }
      } while (true);
    }
    expect(TokenTypes.RightParen);
    return t;
  }

  /**
   * <pre>
   *      name  -> <id>
   *  </pre>
   *
   * @return the id tree
   * @throws SyntaxError - thrown for any syntax error
   */
  public AST rName() throws SyntaxError {
    AST t;
    if (isNextTok(TokenTypes.Identifier)) {
      t = new IdTree(currentToken);
      scan();
      return t;
    }
    throw new SyntaxError(currentToken, TokenTypes.Identifier);
  }

  AST getRelationTree() {  // build tree with current token's relation
    TokenTypes kind = currentToken.getKind();
    if (relationalOps.contains(kind)) {
      AST t = new RelOpTree(currentToken);
      scan();
      return t;
    } else {
      return null;
    }
  }

  private AST getAddOperTree() {
    TokenTypes kind = currentToken.getKind();
    if (addingOps.contains(kind)) {
      AST t = new AddOpTree(currentToken);
      scan();
      return t;
    } else {
      return null;
    }
  }

  private AST getMultOperTree() {
    TokenTypes kind = currentToken.getKind();
    if (multiplyingOps.contains(kind)) {
      AST t = new MultOpTree(currentToken);
      scan();
      return t;
    } else {
      return null;
    }
  }

  private boolean isNextTok(TokenTypes kind) {
    if ((currentToken == null) || (currentToken.getKind() != kind)) {
      return false;
    }
    return true;
  }

  private void expect(TokenTypes kind) throws SyntaxError {
    if (isNextTok(kind)) {
      scan();
      return;
    }
    throw new SyntaxError(currentToken, kind);
  }

  private void scan() {
    currentToken = lex.nextToken();
    if (currentToken != null) {
      currentToken.print();   // debug printout
    }
    return;
  }
}

class SyntaxError extends Exception {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  /**
   *
   */
  private Token tokenFound;
  private TokenTypes kindExpected;

  /**
   * record the syntax error just encountered
   *
   * @param tokenFound   is the token just found by the parser
   * @param kindExpected is the token we expected to find based on
   *                     the current context
   */
  public SyntaxError(Token tokenFound, TokenTypes kindExpected) {
    this.tokenFound = tokenFound;
    this.kindExpected = kindExpected;
  }

  void print() {
    System.out.println("Expected: " +
        kindExpected);
    return;
  }
}
