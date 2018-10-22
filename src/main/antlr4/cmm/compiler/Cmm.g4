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
    | 'char'
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
    : TYPE functionName=IDENTIFIER '(' parameters=parameter_list ')' 
	;

generic_variable_declaration
    : TYPE variableName=IDENTIFIER
    ;
parameter_list
    : declarations+=generic_variable_declaration (',' declarations+=generic_variable_declaration)*
    |
    ;

function_body
    : '{' statements=statementList 'return' returnValue=expression SEMICOLON '}'
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
    ;

statementList
    : statement*;

variable_declaration
    : generic_variable_declaration #vardec
    | generic_variable_declaration ASSIGN val=expression #vardecassign
    | 'const' dec=generic_variable_declaration ASSIGN val=expression #constdec
    ;

function_call
    : functionName=IDENTIFIER '(' arguments=expression_list
    ;

expression_list
    : expressions+=expression (',' expressions+=expression)*
    |
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
    : left=expression '/' right=expression #Division
    | left=expression '*' right=expression #Multiplication
    | left=expression '-' right=expression #Minus
    | left=expression '+' right=expression #Plus
    | left=expression operator=('<' | '<=' | '>' | '>=') right=expression #Relational
    | left=expression '&&' right=expression #And
    | left=expression '||' right=expression #Or
    | '!' expr=expression #Not
    | '(' expr=expression ')' #Parenthesis
    | number=NUMBER #Number
    | character=CHARACTER #Character
    | variableName=IDENTIFIER #Variable
    | function_call #FunctionCallExpression
    ;
