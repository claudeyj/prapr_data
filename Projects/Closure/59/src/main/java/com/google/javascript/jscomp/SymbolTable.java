/*
 * Copyright 2011 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.javascript.jscomp;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.jstype.JSType;
import com.google.javascript.rhino.jstype.SimpleSlot;
import com.google.javascript.rhino.jstype.StaticReference;
import com.google.javascript.rhino.jstype.StaticScope;
import com.google.javascript.rhino.jstype.StaticSlot;
import com.google.javascript.rhino.jstype.StaticSourceFile;
import com.google.javascript.rhino.jstype.StaticSymbolTable;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * A symbol table for people that want to use Closure Compiler as an indexer.
 *
 * Contains an index of all the symbols in the code within a compilation
 * job. The API is designed for people who want to visit all the symbols, rather
 * than people who want to lookup a specific symbol by a certain key.
 *
 * We can use this to combine different types of symbol tables. For example,
 * one class might have a {@code StaticSymbolTable} of all variable references,
 * and another class might have a {@code StaticSymbolTable} of all type names
 * in JSDoc comments. This class allows you to combine them into a unified
 * index.
 *
 * Most passes build their own "partial" symbol table that implements the same
 * interface (StaticSymbolTable, StaticSlot, and friends). Individual compiler
 * passes usually need more or less metadata about the certainty of symbol
 * information. Building a complete symbol table with all the necessary metadata
 * for all passes would be too slow. However, as long as these "partial" symbol
 * tables implement the proper interfaces, we should be able to add them to this
 * symbol table to make it more complete.
 *
 * If clients want fast lookup, they should build their own wrapper around
 * this symbol table that indexes symbols or references by the desired lookup
 * key.
 *
 * @see #addSymbolsFrom For more information on how to write plugins for this
 *    symbol table.
 *
 * @author nicksantos@google.com (Nick Santos)
 */
public final class SymbolTable
    implements StaticSymbolTable<SymbolTable.Symbol, SymbolTable.Reference> {

  /**
   * All symbols in the program, uniquely identified by the node where
   * they're declared.
   */
  private final Map<Node, Symbol> symbols = Maps.newHashMap();

  /**
   * All scopes in the program, uniquely identified by the node where
   * they're declared.
   */
  private final Map<Node, SymbolScope> scopes = Maps.newHashMap();

  /**
   * Clients should get a symbol table by asking the compiler at the end
   * of a compilation job.
   */
  SymbolTable() {}

  @Override
  public Iterable<Reference> getReferences(Symbol symbol) {
    return Collections.unmodifiableCollection(symbol.references.values());
  }

  @Override
  public Iterable<Symbol> getAllSymbols() {
    return Collections.unmodifiableCollection(symbols.values());
  }

  @Override
  public StaticScope<JSType> getScope(Symbol slot) {
    return slot.scope;
  }

  /**
   * Make sure all the symbols and references in {@code otherSymbolTable}
   * are in this symbol table.
   *
   * Uniqueness of symbols and references is determined by the associated
   * node.
   *
   * If multiple symbol tables are mixed in, we do not check for consistency
   * between symbol tables. The first symbol we see dictates the type
   * information for that symbol.
   */
  <S extends StaticSlot<JSType>, R extends StaticReference<JSType>>
  void addSymbolsFrom(StaticSymbolTable<S, R> otherSymbolTable) {
    for (S otherSymbol : otherSymbolTable.getAllSymbols()) {
      SymbolScope myScope = createScopeFrom(
          otherSymbolTable.getScope(otherSymbol));

      StaticReference<JSType> decl = otherSymbol.getDeclaration();
      if (decl == null) {
        continue;
      }

      Node declNode = decl.getNode();

      Symbol mySymbol = symbols.get(declNode);
      if (mySymbol == null) {
        mySymbol = new Symbol(
            otherSymbol.getName(),
            otherSymbol.getType(),
            otherSymbol.isTypeInferred(),
            myScope);
        symbols.put(declNode, mySymbol);
        myScope.ownSymbols.put(mySymbol.getName(), mySymbol);

        mySymbol.setDeclaration(new Reference(mySymbol, declNode));
      }

      for (R otherRef : otherSymbolTable.getReferences(otherSymbol)) {
        Node otherRefNode = otherRef.getNode();
        if (!mySymbol.references.containsKey(otherRefNode)) {
          mySymbol.references.put(
              otherRefNode, new Reference(mySymbol, otherRefNode));
        }
      }
    }
  }

  /**
   * Given a scope from another symbol table, returns the {@code SymbolScope}
   * rooted at the same node. Creates one if it doesn't exist yet.
   */
  private SymbolScope createScopeFrom(StaticScope<JSType> otherScope) {
    Node otherScopeRoot = otherScope.getRootNode();
    SymbolScope myScope = scopes.get(otherScopeRoot);
    if (myScope == null) {
      StaticScope<JSType> otherScopeParent = otherScope.getParentScope();

      // If otherScope is a global scope, and we already have a global scope,
      // then something has gone seriously wrong.
      //
      // Not all symbol tables are rooted at the same global node, and
      // we do not want to mix and match symbol tables that are rooted
      // differently.

      if (otherScopeParent == null) {
        // The global scope must be created before any local scopes.
        Preconditions.checkState(scopes.isEmpty());
      }

      myScope = new SymbolScope(
          otherScopeRoot,
          otherScopeParent == null ? null : createScopeFrom(otherScopeParent),
          otherScope.getTypeOfThis());
      scopes.put(otherScopeRoot, myScope);
    }
    return myScope;
  }

  public static final class Symbol extends SimpleSlot {
    // Use a linked hash map, so that the results are deterministic
    // (and so the declaration always comes first).
    private final Map<Node, Reference> references = Maps.newLinkedHashMap();

    private final SymbolScope scope;

    private Reference declaration = null;

    Symbol(String name, JSType type, boolean inferred, SymbolScope scope) {
      super(name, type, inferred);
      this.scope = scope;
    }

    @Override
    public Reference getDeclaration() {
      return declaration;
    }

    /** Sets the declaration node. May only be called once. */
    void setDeclaration(Reference ref) {
      Preconditions.checkState(this.declaration == null);
      this.declaration = ref;
      references.put(ref.getNode(), ref);
    }
  }

  public static final class Reference implements StaticReference<JSType> {
    private final Symbol symbol;
    private final Node node;

    Reference(Symbol symbol, Node node) {
      this.symbol = symbol;
      this.node = node;
    }

    @Override
    public Symbol getSymbol() {
      return symbol;
    }

    @Override
    public Node getNode() {
      return node;
    }

    @Override
    public StaticSourceFile getSourceFile() {
      return node.getStaticSourceFile();
    }
  }

  public static final class SymbolScope implements StaticScope<JSType> {
    private final Node rootNode;
    private final SymbolScope parent;
    private final JSType typeOfThis;
    private final Map<String, Symbol> ownSymbols = Maps.newHashMap();

    SymbolScope(
        Node rootNode,
        @Nullable SymbolScope parent,
        JSType typeOfThis) {
      this.rootNode = rootNode;
      this.parent = parent;
      this.typeOfThis = typeOfThis;
    }

    @Override
    public Node getRootNode() {
      return rootNode;
    }

    @Override
    public SymbolScope getParentScope() {
      return parent;
    }

    @Override
    public Symbol getSlot(String name) {
      Symbol own = getOwnSlot(name);
      if (own != null) {
        return own;
      }

      return parent == null ? null : parent.getSlot(name);
    }

    @Override
    public Symbol getOwnSlot(String name) {
      return ownSymbols.get(name);
    }

    @Override
    public JSType getTypeOfThis() {
      return typeOfThis;
    }
  }
}
