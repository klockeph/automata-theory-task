package de.tum.in.afl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;

public class NFA {

    static class State {
       final String label;

        State(String label) {
            this.label = label;
        }
    }

    static class Transition implements Comparable<Transition> {
        final State from;
        final Symbol symbol;
        final State to;

        Transition(State from, Symbol symbol, State to) {
            this.from = from;
            this.symbol = symbol;
            this.to = to;
        }

        @Override
        public boolean equals(Object other) {
            if(!(other instanceof Transition)) return false;
            Transition o = (Transition) other;
            return this.from.equals(o.from) && this.symbol.equals(o.symbol) && this.to.equals(o.to);
        }

        @Override
        public int compareTo(Transition other) {
            int res;
            res = Integer.compare(from.hashCode(), other.from.hashCode());
            if(res == 0) {
                res = Integer.compare(to.hashCode(), other.to.hashCode());
            }
            if(res == 0) {
                res = Integer.compare(symbol.hashCode(), other.symbol.hashCode());
            }
            return res;
        }
    }

    static abstract class Symbol {
        @Override
        public boolean equals(Object other) {
            if(this instanceof Epsilon && other instanceof Epsilon) return true;
            if(this instanceof Epsilon || other instanceof Epsilon) return false;
            if(this instanceof  Letter && other instanceof Letter)
                return ((Letter)this).value == ((Letter)other).value;
            return false;
        }

        static class Letter extends Symbol {
            final char value;
            Letter(char value) {
                this.value = value;
            }

            @Override
            public int hashCode() {
               return value;
            }
        }

        static class Epsilon extends Symbol {
            @Override
            public int hashCode() {
                return 1024;
            }
        }
    }

    public HashSet<State> states = new HashSet<>();
    public TreeSet<Transition> transitions = new TreeSet<>();
    public State initialState;
    public HashSet<State> finalStates = new HashSet<>();

    public static NFA fromSymbol(char c) {
        NFA nfa = new NFA();
        State start = new State("");
        State end = new State("");
        nfa.states.add(start);
        nfa.states.add(end);
        nfa.transitions.add(new Transition(start, new Symbol.Letter(c), end));
        nfa.initialState = start;
        nfa.finalStates.add(end);
        return nfa;
    }

    public static NFA fromEpsilon() {
        NFA nfa = new NFA();
        State start = new State("");
        nfa.states.add(start);
        nfa.finalStates.add(start);
        nfa.initialState = start;
        return nfa;
    }

    public static NFA empty() {
        NFA nfa = new NFA();
        State start = new State("");
        nfa.states.add(start);
        nfa.initialState = start;
        return nfa;
    }

    public void concat(NFA other) {
        for (var s : finalStates) {
            transitions.add(new Transition(s, new Symbol.Epsilon(), other.initialState));
        }
        states.addAll(other.states);
        transitions.addAll(other.transitions);
        this.finalStates = other.finalStates;
    }

    public void union(NFA other) {
        State newStart = new State("");
        transitions.add(new Transition(newStart, new Symbol.Epsilon(), this.initialState));
        transitions.add(new Transition(newStart, new Symbol.Epsilon(), other.initialState));

        this.initialState = newStart;

        states.add(newStart);
        states.addAll(other.states);
        transitions.addAll(other.transitions);
        finalStates.addAll(other.finalStates);
    }

    public void iteration() {
        State newStart = new State("");
        State newEnd = new State("");

        transitions.add(new Transition(newStart, new Symbol.Epsilon(), this.initialState));
        for(var s : finalStates) {
            transitions.add(new Transition(s, new Symbol.Epsilon(), newEnd));
            transitions.add(new Transition(s, new Symbol.Epsilon(), this.initialState));
        }

        this.states.add(newStart);
        this.states.add(newEnd);
        this.initialState = newStart;
        this.finalStates.clear();
        this.finalStates.add(newEnd);
    }

    public NFA removeEpsilons() {
        System.out.println("Removing epsilons");
        NFA nfa = new NFA();
        nfa.initialState = initialState;
        nfa.states.add(initialState);
        if(finalStates.contains(initialState)) {
            nfa.finalStates.add(initialState);
        }
        ArrayList<Transition> transitions_p = new ArrayList<>();
        ArrayList<Transition> worklist = new ArrayList<>();
        for(var t : transitions) {
            if(t.from == initialState) {
                worklist.add(t);
            }
        }

        while(worklist.size() > 0) {
            var t = worklist.remove(0);
            if(t.symbol instanceof Symbol.Letter) {
                nfa.states.add(t.to);
                nfa.transitions.add(t);
                if(finalStates.contains(t.to)) {
                    nfa.finalStates.add(t.to);
                }
                for(var t_ : transitions) {
                    if(t_.from != t.to) {
                        continue;
                    }
                    Transition newTransition = new Transition(t.to, t_.symbol, t_.to);
                    if((t_.symbol instanceof Symbol.Epsilon)) {
                        newTransition = new Transition(t.from, t.symbol, t_.to);
                    }
                    if(!(nfa.transitions.contains(newTransition))) {
                        worklist.add(newTransition);
                    }
                }
            }
            else { // t.to Epsilon t.from
                transitions_p.add(t);
                if(finalStates.contains(t.to)) {
                    nfa.finalStates.add(t.from);
                }
                for(var t_ :transitions) {
                    if(t_.from != t.to) {
                        continue;
                    }
                    Transition newTransition = new Transition(t.from, t_.symbol, t_.to);
                    if(!(nfa.transitions.contains(newTransition) && transitions_p.contains(newTransition))) {
                        worklist.add(newTransition);
                    }
                }
            }
        }
        return nfa;
    }

    public static NFA fromRegex(org.antlr.runtime.tree.CommonTree ast) {
        return epsilonNFAFromRegex(ast).removeEpsilons();
    }

    public static NFA epsilonNFAFromRegex(org.antlr.runtime.tree.CommonTree ast) {
        int token = ast.getToken().getType();

        NFA current = null;

        switch (token) {
            //multi-ary connectives
            case RegexpParser.OR:
                System.out.println("or");
                for (int i = 0; i < ast.getChildCount(); i++) {
                    NFA next = NFA.epsilonNFAFromRegex((org.antlr.runtime.tree.CommonTree) ast.getChild(i));
                    if(current == null) {
                        current = next;
                    }
                    else {
                        current.union(next);
                    }
                }
                break;
            case RegexpParser.CONCATENATION:
                System.out.println("concatenate");
                for (int i = 0; i < ast.getChildCount(); i++) {
                    NFA next = NFA.epsilonNFAFromRegex((org.antlr.runtime.tree.CommonTree) ast.getChild(i));
                    if(current == null) {
                        current = next;
                    }
                    else {
                        current.concat(next);
                    }
                }
                break;

            //unary operators
            case RegexpParser.STAR:
                System.out.println("star");
                current = NFA.epsilonNFAFromRegex((org.antlr.runtime.tree.CommonTree) ast.getChild(0));
                current.iteration();
                break;

            //atoms, no children
            case RegexpParser.ID:
                System.out.println(ast.getText());
                current = NFA.fromSymbol(ast.getText().charAt(0));
                break;
            case RegexpParser.EPSILON:
                System.out.println("epsilon");
                current = NFA.fromEpsilon();
                break;
            case RegexpParser.EMPTYSET:
                System.out.println("empty_set");
                current = NFA.empty();
                break;

            default:
                System.out.println("D'OH!");
                throw new AssertionError("Unknown Token in Regex!");
        }

        return current;
    }

    public String toDOT() {
        StringBuilder dot = new StringBuilder("digraph finite_state_machine {\n  rankdir=LR;\n");
        for (var s : states) {
            String shape = "circle";
            if(finalStates.contains(s)) {
                shape = "doublecircle";
            }
            dot.append("  node [shape = ").append(shape).append(", label=\"").append(s.label).append("\"] \"").append(s.hashCode()).append("\";\n");
        }
        dot.append("  node [shape = point] startstatename;\n");
        dot.append("  startstatename -> \"").append(initialState.hashCode()).append("\";\n");

        for (var t : transitions) {
            char label = '/'; // "\u03B5";
            if (t.symbol instanceof Symbol.Letter) {
                label = ((Symbol.Letter) t.symbol).value;
            }
            dot.append("  \"").append(t.from.hashCode()).append("\" -> \"").append(t.to.hashCode()).append("\" [label=\"").append(label).append("\"];\n");
        }
        dot.append("}");
        return dot.toString();
    }
}
