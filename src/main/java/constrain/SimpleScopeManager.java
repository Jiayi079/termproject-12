package constrain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import lexer.Symbol;
import lexer.TokenTypes;

public class SimpleScopeManager<T>{

  private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

  private Stack<Map<Symbol, T>> scopes = new Stack<>();

  public void beginScope(){
    scopes.push(new HashMap<>());
  }

  public void endScope(){
    scopes.pop();
  }

  public void put(Symbol key, T value) {
    T temp = scopes.peek().get(key);
    if(temp != null){
      System.out.println(temp);
    }
    scopes.peek().put(key, value);
  }

  public T get(Symbol key) {
    for(int i = scopes.size() - 1; i >= 0; i--){
      var currentScope = scopes.get(i);
      T obj = currentScope.get(key);
      if(obj != null){
        return obj;
      }
    }
    return null;
  }

  public String toString(){
    return gson.toJson(this);
  }

  // Demo!
  public static void main(String[] args) {
    var scopeManager = new SimpleScopeManager<Integer>();
    scopeManager.beginScope();
    scopeManager.put(new Symbol("a", TokenTypes.Identifier), 1);
    System.out.println(scopeManager);
    scopeManager.beginScope();
    scopeManager.put(new Symbol("b", TokenTypes.Identifier), 2);
    System.out.println(scopeManager);
    scopeManager.put(new Symbol("b", TokenTypes.Identifier), 3);
    System.out.println(scopeManager);
  }

}
