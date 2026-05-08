parser grammar BabyCobolParser;

options {
    tokenVocab=BabyCobolLexer;
}

program
    : identification data? procedure? EOF
    ;

identification
    : IDENTIFICATION DIVISION DOT PROGRAM_ID PID_DOT_FIRST PID_NAME PID_DOT_LAST
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
    : PICTURE IS PICTURE_VALUE
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
    | stopStmt
    | subtractStmt
    | moveStmt
    | performStmt
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
    : IF anyExpression THEN statement+ elseStmt? END?
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

// PERFORM statement ----
performStmt
    : PERFORM ID throughClause? timesClause?
    ;

throughClause
    : THROUGH ID
    ;

timesClause
    : atomic TIMES
    ;

// PERFORM statement END----


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
    : logicalExpression
    ;

logicalExpression
    : relationalExpression (( AND |OR ) relationalExpression )*
    ;

relationalExpression
    : additiveExpression (( EQUAL|LESSTHAN|GREATERTHAN|LTEQUALTO|GTEQUALTO|ANGLEDBRACKETS) additiveExpression )*
    ;

additiveExpression
    : multiplicativeExpression (( PLUS|MINUS)multiplicativeExpression)*
    ;

multiplicativeExpression
    : unaryExpression ((STAR |FORWARDSLASH ) unaryExpression)*
    ;

unaryExpression
    : (PLUS| MINUS | NOT)? primaryExpression
    ;

primaryExpression
    : atomic
    | LPAREN anyExpression RPAREN
    ;


// EVALUATE statement END ----

// STOP statement ----

stopStmt
    : STOP DOT
    ;

// STOP statement END ----


subtractStmt
    : SUBTRACT atomic+ FROM atomic+ givingClause? DOT
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

relationalOperator
    : EQUAL
    | LESSTHAN 
    | GREATERTHAN
    | LTEQUALTO
    | GTEQUALTO
    | ANGLEDBRACKETS
    ;