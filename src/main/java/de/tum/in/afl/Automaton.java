package de.tum.in.afl;

import java.util.*;
import java.util.stream.Collectors;

public class Automaton {

    private boolean isEpsilon = true;
    private boolean isDeterministic = false;
    private static int stateCount = 1;

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

    public static Automaton fromSymbol(char c) {
        Automaton nfa = empty();
        State start = nfa.initialState;
        State end = new State("" + stateCount++);
        nfa.states.add(end);
        nfa.finalStates.add(end);
        nfa.transitions.add(new Transition(start, new Symbol.Letter(c), end));
        return nfa;
    }

    public static Automaton fromEpsilon() {
        Automaton nfa = empty();
        nfa.finalStates.add(nfa.initialState);
        return nfa;
    }

    public static Automaton empty() {
        Automaton nfa = new Automaton();
        State start = new State("" + stateCount++);
        nfa.states.add(start);
        nfa.initialState = start;
        return nfa;
    }

    public static Automaton universal() {
        Automaton nfa = fromEpsilon();
        for(char c = 'A'; c <= 'Z'; c++) {
            nfa.transitions.add(new Transition(nfa.initialState, new Symbol.Letter(c), nfa.initialState));
        }
        for(char c = 'a'; c <= 'z'; c++) {
            nfa.transitions.add(new Transition(nfa.initialState, new Symbol.Letter(c), nfa.initialState));
        }
        return nfa;
    }

    public int run(String word) {
        return run(word, false);
    }

    public int run(String word, boolean stopAtMatch) {
        HashMap<State, HashSet<Transition>> cache = new HashMap<>();
        HashSet<State> currentStates = new HashSet<>();

        currentStates.add(initialState);

        for(int i = 0; i < word.length(); i++) {
            var nextStates = new HashSet<State>();

//            System.out.print("Step " + i + ": ");

            for(var s: currentStates) {
//                System.out.print(s.label + ", ");
                HashSet<Transition> trans;
                if(cache.containsKey(s)) {
                    trans = cache.get(s);
                } else {
                    trans = new HashSet<>();
                    for(var t: transitions) {
                        if(t.from == s) {
                            trans.add(t);
                        }
                    }
                    cache.put(s, trans);
                }
                for(var t : trans)  {
                    if(t.symbol instanceof Symbol.Epsilon) {
                        throw new AssertionError("Asserting non-Epsilon NFA! Did you forget to call removeEpsilons() ?");
                    }
                    if(t.symbol instanceof Symbol.Letter) {
                        if(((Symbol.Letter) t.symbol).value == word.charAt(i)) {
                            nextStates.add(t.to);
                            if(stopAtMatch && finalStates.contains(t.to)) {
                                return i;
                            }
                        }
                    }
                }
            }
//            System.out.println();
            currentStates = nextStates;
        }
        currentStates.retainAll(finalStates);
        if(currentStates.size() > 0) {
            return word.length();
        }
        else {
            return -1;
        }
    }

    public void concat(Automaton other) {
        for (var s : finalStates) {
            transitions.add(new Transition(s, new Symbol.Epsilon(), other.initialState));
        }
        states.addAll(other.states);
        transitions.addAll(other.transitions);
        this.finalStates = other.finalStates;
    }

    public void union(Automaton other) {
        State newStart = new State("" + stateCount++);
        transitions.add(new Transition(newStart, new Symbol.Epsilon(), this.initialState));
        transitions.add(new Transition(newStart, new Symbol.Epsilon(), other.initialState));

        this.initialState = newStart;

        states.add(newStart);
        states.addAll(other.states);
        transitions.addAll(other.transitions);
        finalStates.addAll(other.finalStates);
    }

    public void iteration() {
        State newStart = new State("" + stateCount++);
        State newEnd = new State("" + stateCount++);
        State newIter = new State("" + stateCount++);

        transitions.add(new Transition(newStart, new Symbol.Epsilon(), newIter));
        transitions.add(new Transition(newIter, new Symbol.Epsilon(), this.initialState));
        transitions.add(new Transition(newIter, new Symbol.Epsilon(), newEnd));
        for(var s : finalStates) {
            transitions.add(new Transition(s, new Symbol.Epsilon(), newIter));
        }

        this.states.add(newStart);
        this.states.add(newIter);
        this.states.add(newEnd);
        this.initialState = newStart;
        this.finalStates.clear();
        this.finalStates.add(newEnd);
    }

    public Automaton removeEpsilons() {
        Automaton nfa = new Automaton();
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
        nfa.isEpsilon = false;
        return nfa;
    }

    public Automaton toDFA() {
        if(isEpsilon) {
            throw new AssertionError("Can't convert Epsilon-NFA to DFA. Did you forget to call removeEpsilon() ?");
        }
        Automaton dfa = new Automaton();
        HashMap<HashSet<State>, State> stateMap = new HashMap<>();
        ArrayList<HashSet<State>> workList = new ArrayList<>();

        dfa.initialState = initialState;
        HashSet<State> nfaInitialSet = new HashSet<>();
        nfaInitialSet.add(initialState);
        stateMap.put(nfaInitialSet, initialState);

        workList.add(nfaInitialSet);
        while(!workList.isEmpty()) {
            HashSet<State> q = workList.remove(0);
            dfa.states.add(stateMap.get(q));
            for(var s : q) {
                if (finalStates.contains(s)) {
                    dfa.finalStates.add(stateMap.get(q));
                    break;
                }
            }

            HashMap<Symbol, HashSet<State>> qpp = new HashMap<>();
            for(var t : transitions) {
                if(!q.contains(t.from)) {
                    continue;
                }
                if(!qpp.containsKey(t.symbol)) {
                    qpp.put(t.symbol, new HashSet<>());
                }
                qpp.get(t.symbol).add(t.to);
            }
            for (var kvp : qpp.entrySet()) {
                if(!stateMap.containsKey(kvp.getValue())) {
                    State newState = new State(kvp.getValue().stream().map(x -> x.label).collect(Collectors.joining(",")));
                    stateMap.put(kvp.getValue(), newState);
                }
                if(!dfa.states.contains(stateMap.get(kvp.getValue()))) {
                    workList.add(kvp.getValue());
                }
                dfa.transitions.add(new Transition(stateMap.get(q), kvp.getKey(), stateMap.get(kvp.getValue())));
            }
        }
        dfa.isEpsilon = false;
        dfa.isDeterministic = true;
        return dfa;
    }

    public void toEditAutomaton(int editDistance) {
        HashMap<State, State[]> editStates = new HashMap<>();
        for(var s : states) {
            var a = new State[editDistance+1];
            a[0] = s;
            for(var i = 1; i <= editDistance; i++) {
                a[i] = new State(s.label + "|" + i);
            }
            editStates.put(s, a);
        }

        // copying across "layers":
        HashSet<Transition> newTransitions = new HashSet<>();
        for(var t : transitions) {
            State[] fromEdits = editStates.get(t.from);
            State[] toEdits = editStates.get(t.to);
            for(var i = 0; i <= editDistance; i++) {
                // copying inside layer
                if(i != 0) {
                    newTransitions.add(new Transition(fromEdits[i], t.symbol, toEdits[i]));
                }
                if(i != editDistance) {
                    // deletion:
                    newTransitions.add(new Transition(fromEdits[i], new Symbol.Epsilon(), toEdits[i+1]));
                    // replacement:
                    for(char a = 'a'; a <= 'z'; a++){
                        if(a != ((Symbol.Letter)t.symbol).value) {
                            newTransitions.add(new Transition(fromEdits[i], new Symbol.Letter(a), toEdits[i + 1]));
                        }
                    }
                    for(char a = 'A'; a <= 'Z'; a++) {
                        if(a != ((Symbol.Letter)t.symbol).value) {
                            newTransitions.add(new Transition(fromEdits[i], new Symbol.Letter(a), toEdits[i + 1]));
                        }
                    }
                }
            }
        }

        // insertions:
        for(var s : states) {
           State[] edits = editStates.get(s);
           for(int i = 0; i < editDistance; i++) {

               for(char a = 'a'; a <= 'z'; a++) {
                   newTransitions.add(new Transition(edits[i], new Symbol.Letter(a), edits[i + 1]));
               }
               for(char a = 'A'; a <= 'Z'; a++) {
                   newTransitions.add(new Transition(edits[i], new Symbol.Letter(a), edits[i + 1]));
               }
           }
        }

        for(var edits : editStates.entrySet()) {
            states.addAll(Arrays.asList(edits.getValue()));
            if(finalStates.contains(edits.getKey())) {
                finalStates.addAll(Arrays.asList(edits.getValue()));
            }
        }
        transitions.addAll(newTransitions);

        isDeterministic = false;
        isEpsilon = true;
    }

    public Automaton backwardsMatchAutomaton() {
        Automaton newNFA = new Automaton();
        newNFA.states = states;

        if(finalStates.size() != 1) {
            throw new AssertionError("The source automaton has more than one final state!");
        }

        // Only 1 final state, but can only iterate over HashSet
        for (var f : finalStates) {
           newNFA.initialState = f;
        }

        newNFA.finalStates.add(initialState);

        for(var t : transitions) {
            newNFA.transitions.add(new Transition(t.to, t.symbol, t.from));
        }

        return newNFA;
    }

    public static Automaton fromRegexWithPrefix(org.antlr.runtime.tree.CommonTree ast) {
        Automaton pattern = epsilonNFAFromRegex(ast);
        Automaton univ = universal();
        univ.concat(pattern);
        return univ.removeEpsilons();
    }

    public static Automaton fromRegex(org.antlr.runtime.tree.CommonTree ast) {
        return epsilonNFAFromRegex(ast).removeEpsilons();
    }

    public static Automaton epsilonNFAFromRegex(org.antlr.runtime.tree.CommonTree ast) {
        int token = ast.getToken().getType();

        Automaton current = null;

        switch (token) {
            //multi-ary connectives
            case RegexpParser.OR:
                for (int i = 0; i < ast.getChildCount(); i++) {
                    Automaton next = Automaton.epsilonNFAFromRegex((org.antlr.runtime.tree.CommonTree) ast.getChild(i));
                    if(current == null) {
                        current = next;
                    }
                    else {
                        current.union(next);
                    }
                }
                break;
            case RegexpParser.CONCATENATION:
                for (int i = 0; i < ast.getChildCount(); i++) {
                    Automaton next = Automaton.epsilonNFAFromRegex((org.antlr.runtime.tree.CommonTree) ast.getChild(i));
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
                current = Automaton.epsilonNFAFromRegex((org.antlr.runtime.tree.CommonTree) ast.getChild(0));
                current.iteration();
                break;

            //atoms, no children
            case RegexpParser.ID:
                current = Automaton.fromSymbol(ast.getText().charAt(0));
                break;
            case RegexpParser.EPSILON:
                current = Automaton.fromEpsilon();
                break;
            case RegexpParser.EMPTYSET:
                current = Automaton.empty();
                break;

            default:
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
