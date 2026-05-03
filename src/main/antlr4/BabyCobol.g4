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
    ;

displayStmt
    : DISPLAY atomic+ (WITH NO ADVANCING)? DOT
    ;

acceptStmt
    : ACCEPT ID+ DOT
    ;

addStmt
    : ADD atomic+ TO atomic givingClause? DOT
    ;

divideStmt
    : DIVIDE atomic INTO atomic+ givingRemainderClause? DOT

mulStmt
    : MULTIPLY atomic BY atomic+ givingClause? DOT
    ;

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
MULTIPLY : 'MULTIPLY';
BY       : 'BY';
TO             : 'TO';
GIVING         : 'GIVING';
DIVIDE         : 'DIVIDE';
INTO           : 'INTO';
REMAINDER      : 'REMAINDER';


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
DOT    : '.';
LPAREN : '(';
RPAREN : ')';

// VALUES
INT      : [0-9]+;
PIC_CHAR : [9AXZSV];
ID       : [A-Z][A-Z0-9-]*;
STRING   : '"' (~["\r\n])* '"';

WS : [ \t\r\n]+ -> skip;