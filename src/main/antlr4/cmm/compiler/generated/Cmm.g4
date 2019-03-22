grammar Cmm;

/* LEXER */

WHITESPACE
    : [ \n\t\r]+ -> skip
    ;

SEMICOLON
    : ';'
    ;
TYPE
    : 'num'
    ;
IDENTIFIER
    : [a-zA-Z_][a-zA-Z0-9_]*
    ;
NUMBER
    : [0-9]+('.'[0-9]*)?
    ;
CHARACTER
    : [a-zA-Z] | ' ' | '\n' | '_' 
    | 'ä' | 'ö' | 'ü' 
    | 'Ä' | 'Ö' | 'Ü' 
    ;
ASSIGN
    : '='
    ;

/* PARSER */
program
    : programPart+
    ;

programPart
    : variable_declaration SEMICOLON
    | function_definition
    ;

function_definition
    : function_header function_body
    ;

function_header
    : ret=ret_type functionName=IDENTIFIER '(' (generic_variable_declaration (',' generic_variable_declaration)*)? ')'
	;

ret_type
    : TYPE
    | 'void'
    ;

generic_variable_declaration
    : TYPE variableName=IDENTIFIER
    ;
parameter_list
    : (generic_variable_declaration (',' generic_variable_declaration)*)
    ;



function_body
    : '{' statements=statementList '}'
    ;

block
    : '{' statements=statementList '}'
    ;

statement
	: variable_declaration SEMICOLON
	| function_call SEMICOLON
	| assign_operation SEMICOLON
	| branch
	| loop
    | returnstatement SEMICOLON
    ;

returnstatement
    : 'return' returnValue=expression
    ;


statementList
    : statements=statement*;

variable_declaration
    : dec=generic_variable_declaration #vardec
    | dec=generic_variable_declaration ASSIGN val=expression #vardecassign
    | 'const' dec=generic_variable_declaration ASSIGN val=expression #constdec
    ;

function_call
    : functionName=IDENTIFIER '(' (expression_list)? ')'
    ;

expression_list
    : (expressions+=expression (',' expressions+=expression)*)
    ;

assign_operation
    : variableName=IDENTIFIER ASSIGN expr=expression
    ;

branch
    : 'if' '(' condition=expression ')' onTrue=block ('else' onFalse=block)?
    ;

loop
    : 'loop' '(' condition=expression ')' onTrue=block
    ;

expression
    : left=expression operator='/' right=expression #Division
    | left=expression operator='*' right=expression #Multiplication
    | left=expression operator='-' right=expression #Minus
    | left=expression operator='+' right=expression #Plus
    | left=expression operator=('<' | '>' | '<=' | '>=') right=expression #Relational
    | '!' expr=expression #Not
    | left=expression operator=('==' | '!=')right=expression #Equality
    | left=expression operator=('&&' | '||') right=expression #Conjunction
    | '(' expr=expression ')' #Parenthesis
    | number=NUMBER #Number
    | variableName=IDENTIFIER #Variable
    | function_call #FunctionCallExpression
    ;
