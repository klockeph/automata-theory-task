grammar Regexp;

options { output=AST;backtrack=true;}
tokens{
CONCATENATION;
}

@header {
package de.tum.in.afl;
}

@lexer::header {
package de.tum.in.afl;
}

init      : r EOF;
r         : r1 (OR^ r1)*;
r1        : (a=r2 -> $a) (r2+ -> ^(CONCATENATION r2+))?;
r2      :         LPAR r RPAR STAR -> ^(STAR r)
        |         ID
        |         EPSILON
        |         EMPTYSET
        |         LPAR r RPAR      -> r
        ;

OR: '|';
STAR: '*';
EPSILON: '@';
EMPTYSET: '/';
LPAR: '(';
RPAR: ')';

ID      :         'a'..'z'|'A'..'Z'  ;
WS       :        (' '|'\n'|'\t'|'\r')+ { skip(); };