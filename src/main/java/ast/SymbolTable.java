package ast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable {
    private Map<String, List<Symbol>> symbols = new LinkedHashMap<>();

    public void addSymbol(Symbol symbol) {
        String key = symbol.getName().toLowerCase();
        symbols.computeIfAbsent(key, k -> new ArrayList<>()).add(symbol);
    }

    /**
     * returns the first symbol with the given name, or null if none exists
     * this for unique lookups where duplicates are not expected
     */
    public Symbol getSymbol(String name) {
        List<Symbol> list = symbols.get(name.toLowerCase());
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    /**
     * returns all symbols with the given name
     */
    public List<Symbol> getSymbolsByName(String name) {
        return symbols.getOrDefault(name.toLowerCase(), new ArrayList<>());
    }

    /**
     * returns all symbols in the table (flattened)
     */
    public List<Symbol> getAllSymbols() {
        List<Symbol> all = new ArrayList<>();
        for (List<Symbol> list : symbols.values()) {
            all.addAll(list);
        }
        return all;
    }

    /**
     * returns the underlying map for iteration that needs access to all entries
     */
    public Map<String, List<Symbol>> getSymbols() {
        return symbols;
    }

    public boolean contains(String name) {
        return symbols.containsKey(name.toLowerCase());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Symbol Table ---\n");
        for (List<Symbol> list : symbols.values()) {
            for (Symbol symbol : list) {
                sb.append(symbol.toString()).append("\n");
            }
        }
        sb.append("--------------------");
        return sb.toString();
    }
}
