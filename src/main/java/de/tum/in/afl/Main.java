package de.tum.in.afl;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;

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

  public static void main(String[] args) throws Exception {
    if (args.length != 4) {
      System.err.println("Use the following format: [task] [regex-file] [text-file] [edit-distance]");
      System.exit(-1);
    }

    int task = Integer.parseInt(args[0]);

    // TODO: Write solutions here.
    switch (task) {
      case 1:
        break;

      case 2:
        break;

      case 3:
        break;

      case 4:
        break;

      case 5:
        // DEBUGGING
//        NFA a = NFA.fromSymbol('a');
//        NFA b = NFA.fromSymbol('b');
//        a.concat(b);
//        NFA c = NFA.fromSymbol('c');
//        a.union(c);
//        a.iteration();
//        System.out.println(a.removeEpsilons().toDOT());

        NFA nfa = NFA.fromRegex(regexpToTree(args[1]));
        NFA.State sf = null;
        for(var s : nfa.finalStates) {
          sf = s;
        }
        System.out.println(nfa.transitions.contains(new NFA.Transition(nfa.initialState, new NFA.Symbol.Letter('d'), sf)));
        System.out.println(nfa.toDOT());
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
