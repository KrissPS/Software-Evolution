parser grammar BabyCobolParser;

options {
    tokenVocab=BabyCobolLexer;
}

program
    : identification data? procedure? EOF
    ;

identification
    : IDENTIFICATION DIVISION DOT PROGRAM_ID DOT ID+ DOT
    ;

data
    : DATA DIVISION DOT dataLine*
    ;

dataLine
    : dataEntry
    | copyStmt
    ;

// INT EQUALS LEVEL
dataEntry
    : INT ID dataClause* DOT
    ;

dataClause
    : (pictureClause | likeClause) occursClause?
    | occursClause
    ;

pictureClause
    : PICTURE IS? pictureValue
    ;

pictureValue
    : (ID | INT | PIC_CHAR | LPAREN | RPAREN)+
    ;

likeClause
    : LIKE ID
    ;

occursClause
    : OCCURS INT TIMES
    ;

procedure
    : PROCEDURE DIVISION usingProcedureClause? DOT sentence* paragraph*
    ;

usingProcedureClause
    : USING procedureParameter+
    ;

procedureParameter
    : BY REFERENCE ID
    | BY CONTENT atomic
    | BY VALUE atomic
    ;

paragraph
    : ID DOT sentence*
    ;

sentence
    : statement+ DOT
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
    | loopStmt
    | nextSentenceStmt
    | goToStmt
    | callStmt
    | alterStmt
    | signalStmt
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
    : relationalExpression ((AND | OR) contractedRelationalExpression)*
    ;

contractedRelationalExpression
    : relationalExpression
    | relationalOperator additiveExpression
    | additiveExpression
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
    : STOP
    ;

// STOP statement END ----


subtractStmt
    : SUBTRACT atomic+ FROM atomic+ givingClause?
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

copyStmt
    : COPY ID replacingClause?
    ;

replacingClause
    : REPLACING STRING BY STRING
    ;

loopStmt
    : LOOP loopElement* END
    ;

loopElement
    : varyingClause
    | whileClause
    | untilClause
    | statement
    ;

varyingClause
    : VARYING ID (FROM atomic)? (TO atomic)? (BY atomic)?
    ;

whileClause
    : WHILE anyExpression
    ;

untilClause
    : UNTIL anyExpression
    ;

atomic
    : ID
    | INT
    | DECIMAL
    | STRING
    | SPACES
    | HIGH_VALUES
    | LOW_VALUES
    ;

relationalOperator
    : EQUAL
    | LESSTHAN
    | GREATERTHAN
    | LTEQUALTO
    | GTEQUALTO
    | ANGLEDBRACKETS
    ;

nextSentenceStmt
    : NEXT SENTENCE
    ;

goToStmt
    : GO TO ID
    ;

callStmt
    : CALL ID usingCallClause?
    ;

usingCallClause
    : USING callArgument+
    ;

callArgument
    : BY REFERENCE ID
    | BY CONTENT atomic
    | BY VALUE atomic
    ;

alterStmt
    : ALTER ID TO PROCEED TO ID
    ;

signalStmt
    : SIGNAL ID ON ERROR
    | SIGNAL OFF ON ERROR
    ;