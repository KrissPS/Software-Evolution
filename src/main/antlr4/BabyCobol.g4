grammar BabyCobol;

program
    : identification data procedure EOF
    ;

identification
    : IDENTIFICATION DIVISION DOT PROGRAM_ID DOT ID DOT
    ;

data
    : DATA DIVISION DOT
    ;

procedure
    : PROCEDURE DIVISION DOT
    ;

IDENTIFICATION: 'IDENTIFICATION';
PROGRAM_ID: 'PROGRAM-ID';
DATA: 'DATA';
PROCEDURE: 'PROCEDURE';
DIVISION: 'DIVISION';
DOT: '.';
ID: [A-Z][A-Z0-9-]*;

WS: [ \t\r\n]+ -> skip;