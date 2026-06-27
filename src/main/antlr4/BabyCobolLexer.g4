lexer grammar BabyCobolLexer;

// MAIN KEYWORDS
IDENTIFICATION : 'IDENTIFICATION';
PROGRAM_ID     : 'PROGRAM-ID';
DATA           : 'DATA';
PROCEDURE      : 'PROCEDURE';
DIVISION       : 'DIVISION';
ACCEPT         : 'ACCEPT';
ADD            : 'ADD';
MULTIPLY       : 'MULTIPLY';
BY             : 'BY';
TO             : 'TO';
GIVING         : 'GIVING';
DIVIDE         : 'DIVIDE';
INTO           : 'INTO';
REMAINDER      : 'REMAINDER';
EVALUATE       : 'EVALUATE';
ALSO           : 'ALSO';
END            : 'END';
WHEN           : 'WHEN';
OTHER          : 'OTHER';
THROUGH        : 'THROUGH';
IF             : 'IF';
THEN           : 'THEN';
ELSE           : 'ELSE';
AND            : 'AND';
OR             : 'OR';
NOT            : 'NOT';
SUBTRACT       : 'SUBTRACT';
FROM           : 'FROM';
MOVE           : 'MOVE';
PERFORM        : 'PERFORM';
STOP           : 'STOP';
LOOP    : 'LOOP';
VARYING : 'VARYING';
WHILE   : 'WHILE';
UNTIL   : 'UNTIL';
COPY      : 'COPY';
REPLACING : 'REPLACING';
NEXT    : 'NEXT';
SENTENCE : 'SENTENCE';
GO : 'GO';
CALL      : 'CALL';
USING     : 'USING';
REFERENCE : 'REFERENCE';
CONTENT   : 'CONTENT';
VALUE     : 'VALUE';

// DATA DIVISION FEAT
PICTURE : 'PICTURE';
IS      : 'IS';
LIKE    : 'LIKE';
OCCURS  : 'OCCURS';
TIMES   : 'TIMES';

// DISPLAY
DISPLAY   : 'DISPLAY';
WITH      : 'WITH';
NO        : 'NO';
ADVANCING : 'ADVANCING';

// SYMBOLS
DOT             : '.';
LPAREN          : '(';
RPAREN          : ')';
PLUS            : '+';
MINUS           : '-';
STAR            : '*';
FORWARDSLASH    : '/';
EQUAL           : '=';
LESSTHAN        : '<';
GREATERTHAN     : '>';
LTEQUALTO       : '<=';
GTEQUALTO       : '>=';
ANGLEDBRACKETS  : '<>';

// VALUES
DECIMAL   : [0-9]+ '.' [0-9]+;
INT      : [0-9]+;
ID       : '-'? [a-zA-Z][a-zA-Z0-9-]*;
PIC_CHAR : [9AXZSV];
STRING   : '"' (~["\r\n])* '"';

WS : [ \t\r\n]+ -> skip;

