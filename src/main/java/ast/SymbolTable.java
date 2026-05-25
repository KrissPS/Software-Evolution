package ast;

import java.util.LinkedHashMap;
import java.util.Map;

public class SymbolTable {
    private Map<String, Symbol> symbols = new LinkedHashMap<>();

    public void addSymbol(Symbol symbol) {
        symbols.put(symbol.getName().toLowerCase(), symbol);
    }

    public Symbol getSymbol(String name) {
        return symbols.get(name.toLowerCase());
    }

    public Map<String, Symbol> getSymbols() {
        return symbols;
    }

    public boolean contains(String name) {
        return symbols.containsKey(name.toLowerCase());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Symbol Table ---\n");
        for (Symbol symbol : symbols.values()) {
            sb.append(symbol.toString()).append("\n");
        }
        sb.append("--------------------");
        return sb.toString();
    }
}
