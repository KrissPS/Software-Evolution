grammar BabyCobol;

program
    : identification data? procedure? EOF
    ;

identification
    : IDENTIFICATION DIVISION DOT PROGRAM_ID DOT ID+ DOT
    ;

data
    : DATA DIVISION DOT dataEntry*
    ;

// INT EQUALS LEVEL
dataEntry
    : INT ID dataClause* DOT
    ;

dataClause
    : (pictureClause | likeClause) occursClause?
    ;

pictureClause
    : PICTURE IS pictureString
    ;

pictureString
    : pictureAtom+
    ;

pictureAtom
    : PIC_CHAR pictureRep?
    | INT pictureRep?
    ;

pictureRep
    : LPAREN INT RPAREN
    ;

likeClause
    : LIKE ID
    ;

occursClause
    : OCCURS INT TIMES
    ;

procedure
    : PROCEDURE DIVISION DOT statement*
    ;

statement
    : displayStmt
    | acceptStmt
    | addStmt
    | divideStmt
    | mulStmt
    | evaluateStmt
    | ifStmt
    | moveStmt
    ;

displayStmt
    : DISPLAY atomic+ (WITH NO ADVANCING)?
    ;

acceptStmt
    : ACCEPT ID+
    ;

addStmt
    : ADD atomic+ TO atomic givingClause?
    ;

divideStmt
    : DIVIDE atomic INTO atomic+ givingRemainderClause?
    ;

mulStmt
    : MULTIPLY atomic BY atomic+ givingClause?
    ;

// IF statement ----
ifStmt
    : IF booleanExpression THEN statement+ elseStmt? END?
    ;

elseStmt
    : ELSE statement+
    ;


// IF statement END ----

// MOVE statement ----
moveStmt
    : MOVE atomic TO ID+
    ;
// MOVE statement END ----



// EVALUATE statement ----
evaluateStmt
    : EVALUATE anyExpression alsoClause* whenClauseStatement+ END
    ;

alsoClause
    : ALSO anyExpression
    ;

whenClauseStatement
    : whenClause statement+
    ;

whenClause
    : WHEN whenSubject
    | WHEN OTHER
    ;

whenSubject
    : subWhenSubject (ALSO subWhenSubject)*
    ;

subWhenSubject
    : (anyExpression (THROUGH anyExpression)? )+
    ;

anyExpression
    : atomic
    | anyExpression ( PLUS | MINUS | STAR | FORWARDSLASH ) anyExpression
    | anyExpression ( EQUAL | LESSTHAN | GREATERTHAN | LTEQUALTO | GTEQUALTO ) anyExpression
    | MINUS anyExpression
    | LPAREN anyExpression RPAREN
    ;

booleanExpression
    : atomic
    | booleanExpression ( AND | OR ) booleanExpression
    | booleanExpression ( EQUAL | LESSTHAN | GREATERTHAN | LTEQUALTO | GTEQUALTO ) booleanExpression
    | NOT booleanExpression
    | LPAREN booleanExpression RPAREN
    ;


relationalOperator
    : EQUAL
    | LESSTHAN 
    | GREATERTHAN
    | LTEQUALTO
    | GTEQUALTO
    | ANGLEDBRACKETS
    ;

// EVALUATE statement END ----


givingClause
    : GIVING ID+
    ;

givingRemainderClause
    : GIVING ID+ remainderClause?
    ;

remainderClause
    : REMAINDER ID
    ;


atomic
    : ID
    | INT
    | STRING
    ;

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
MOVE           : 'MOVE';



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
ID       : [A-Z][A-Z0-9-]*;
STRING   : '"' (~["\r\n])* '"';

WS : [ \t\r\n]+ -> skip;