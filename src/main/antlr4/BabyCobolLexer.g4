lexer grammar BabyCobolLexer;

// MAIN KEYWORDS
IDENTIFICATION : 'IDENTIFICATION';
PROGRAM_ID     : 'PROGRAM-ID' -> pushMode(PROGRAM_ID_START_MODE);
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
INT      : [0-9]+;
PIC_CHAR : [9AXZSV];
ID       : [a-zA-Z][a-zA-Z0-9-]*;
STRING   : '"' (~["\r\n])* '"';

WS : [ \t\r\n]+ -> skip;

mode PROGRAM_ID_START_MODE;

WS_PID_START  : [ \t\r\n]+ -> skip;
PID_DOT_FIRST : '.' -> popMode, pushMode(PROGRAM_ID_VALUE_MODE);

mode PROGRAM_ID_VALUE_MODE;

PID_NAME      : ~[.\r\n]+;
PID_DOT_LAST  : '.' -> popMode;
