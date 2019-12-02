package de.tum.in.afl;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class Main {

  public static org.antlr.runtime.tree.CommonTree regexpToTree(String file) throws Exception {
    //	ANTLRInputStream input = new ANTLRInputStream(file)
    CharStream input = new ANTLRFileStream(file);
    // Generate a lexer for reading the formula `input'
    RegexpLexer lexer = new RegexpLexer(input);
    // Generate from the lexer a token stream to be fed to the parser
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    // Generate the parser analyzing the token stream
    RegexpParser parser = new RegexpParser(tokens);
    // Finally parse the input and generate the tree
    RegexpParser.r_return r = parser.r();
    //get the AST encapsuled in r
    return (org.antlr.runtime.tree.CommonTree) r.tree;
  }

  public static void walkAST(org.antlr.runtime.tree.CommonTree ast) {
    int token = ast.getToken().getType();

    switch (token) {
      //multi-ary connectives
      case RegexpParser.OR:
        System.out.println("or");
        for (int i = 0; i < ast.getChildCount(); i++) {
          walkAST((org.antlr.runtime.tree.CommonTree) ast.getChild(i));
        }
        break;
      case RegexpParser.CONCATENATION:
        System.out.println("concatenate");
        for (int i = 0; i < ast.getChildCount(); i++) {
          walkAST((org.antlr.runtime.tree.CommonTree) ast.getChild(i));
        }
        break;

      //unary operators
      case RegexpParser.STAR:
        System.out.println("star");
        walkAST((org.antlr.runtime.tree.CommonTree) ast.getChild(0));
        break;

      //atoms, no children
      case RegexpParser.ID:
        System.out.println(ast.getText());
        break;
      case RegexpParser.EPSILON:
        System.out.println("epsilon");
        break;
      case RegexpParser.EMPTYSET:
        System.out.println("empty_set");
        break;

      default:
        System.out.println("D'OH!");
        break;
    }
  }

  public static String readFile(String filename) throws IOException {
    return Files.lines(Paths.get(filename)).collect(Collectors.joining(""));
  }

  public static void exercise1(String[] args) throws Exception {
    Automaton nfa = Automaton.fromRegexWithPrefix(regexpToTree(args[1]));
    int matchEnd = nfa.run(readFile(args[2]), true);
    System.out.print("Task 1: ");
    if(matchEnd == -1) System.out.println("not found");
    else System.out.println(matchEnd);
  }

  public static Automaton buildEditAutomaton(org.antlr.runtime.tree.CommonTree ast, int editDistance) {
    Automaton nfa = Automaton.fromRegex(ast);
//    System.out.println("Building DFA");
    nfa = nfa.toDFA();
//    System.out.println("Building Edit Automaton");
    nfa.toEditAutomaton(editDistance);
//    System.out.println("Removing Epsilons");
    // concat a final State that accepts epsilons to have only 1 final state
    nfa.concat(Automaton.fromEpsilon());

    nfa = nfa.removeEpsilons();
//    System.out.println("Adding Prefix");
    Automaton univ = Automaton.universal();
    univ.concat(nfa);
//    System.out.println("Removing Epsilons");
    univ = univ.removeEpsilons();
//    System.out.println("Running");
    return univ;
  }

  public static void exercise2(String[] args) throws Exception {
    var ast = regexpToTree(args[1]);
    int editDistance = Integer.parseInt(args[3]);
    Automaton nfa = buildEditAutomaton(ast, editDistance);
    int matchEnd = nfa.run(readFile(args[2]), true);
    System.out.print("Task 2: ");
    if(matchEnd == -1) System.out.println("not found");
    else System.out.println(matchEnd);
  }

  public static void printShortestMatchStartAndEnd(Automaton forward, Automaton backward, String input) {
    int minimalLength = Integer.MAX_VALUE;
    int start = -1;
    int end = -1;
    int offset = 0;
    while(!input.isEmpty()) {
      int matchEnd = forward.run(input, true);
      if(matchEnd == -1) {
        break;
      }
      if(matchEnd == 0) {
        System.out.println("" + offset + " - " + offset);
      }
//      System.out.println("input = " + input);
//      System.out.println("matchEnd = " + matchEnd);
      String reversedInput = new StringBuilder(input.substring(0, matchEnd)).reverse().toString();
//      System.out.println("reversedInput = " + reversedInput);
      int matchLength = backward.run(reversedInput, true);
      if(matchLength == -1) {
        System.out.println("This shouldn't happen: If forward automaton matched, backwards automaton must too");
        break;
      }

//      System.out.println("Match from " + (matchEnd - matchLength + offset) + " to " + (matchEnd + offset));
      if(matchLength < minimalLength) {
        minimalLength = matchLength;
        start = matchEnd - matchLength + offset + 1;
        end = matchEnd + offset;
      }

      input = input.substring(matchEnd);
      offset += matchEnd;
//      System.out.println("new input: " + input);
    }

    if(start == -1) {
      System.out.println("not found");
    }
    else {
      System.out.println("" + start + " - " + end);
    }
  }

  public static void exercise3(String[] args) throws Exception {
    Automaton nfa = Automaton.fromRegexWithPrefix(regexpToTree(args[1]));
    Automaton backwardsNfa = nfa.backwardsMatchAutomaton();

    String input = readFile(args[2]);

    System.out.print("Task 3: ");
    printShortestMatchStartAndEnd(nfa, backwardsNfa, input);
  }

  public static void exercise4(String[] args) throws Exception {
    var ast = regexpToTree(args[1]);
    int editDistance = Integer.parseInt(args[3]);
    Automaton nfa = buildEditAutomaton(ast, editDistance);
    Automaton backwardsNfa = nfa.backwardsMatchAutomaton();

    String input = readFile(args[2]);

    System.out.print("Task 4: ");
    printShortestMatchStartAndEnd(nfa, backwardsNfa, input);
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 4) {
      System.err.println("Use the following format: [task] [regex-file] [text-file] [edit-distance]");
      System.exit(-1);
    }

    int task = Integer.parseInt(args[0]);

    // TODO: Write solutions here.
    switch (task) {
      case 1:
          exercise1(args);
        break;

      case 2:
          exercise2(args);
        break;

      case 3:
          exercise3(args);
        break;

      case 4:
          exercise4(args);
        break;

      case 5:
        // DEBUGGING

        Automaton nfa = Automaton.fromRegex(regexpToTree(args[1])).removeEpsilons();
        nfa = nfa.toDFA();
        nfa.toEditAutomaton(1);
        nfa = nfa.removeEpsilons();
//        System.out.println(nfa.toDOT());
        System.out.println("abd -> " + nfa.run("abd"));
        System.out.println("bad -> " + nfa.run("bad"));
        System.out.println("abc -> " + nfa.run("abc"));
        System.out.println("bd -> " + nfa.run("bd"));
        break;

      default:
        System.out.println("Nothing implemented yet.");
        System.out.println("As a demo I am going to parse the regex given on the commandline.");
        org.antlr.runtime.tree.CommonTree ast = regexpToTree(args[1]);
        System.out.println(ast.toStringTree());
        walkAST(ast);
    }
  }
}
