grammar Cmm;

ASSIGN
	: '='
	;

SEMICOLON
	: ';'
	;

NAME_START_CHAR
	: ('a'..'z') | 'ä' | 'ö' | 'ü'
	| ('A'..'Z') | 'Ä' | 'Ö' | 'Ü'
	| '_'
	;

NAME_CHAR
	: NAME_START_CHAR
	| ('0'..'9')
	;

identifier
	: (NAME_START_CHAR)(NAME_CHAR)*
	;

CONST_KEYWORD
	: 'const'
	;

IF_KEYWORD
	: 'if'
	;

TYPE
	: 'int'
	| 'float'
	| 'char'
	;

TYPE_VOID
	: 'void'
	;

SEPARATOR
	: ','
	;

PARAMETER_BEGIN
	: '('
	;

PARAMETER_END
	: ')'
	;

BLOCK_BEGIN
	: '{'
	;

BLOCK_END
	: '}'
	;

RETURN_KEYWORD
	: 'return'
	;

/* VARIABLES*/
generic_variable_declaration
	: TYPE identifier
	;

constant_variable_definition
	: CONST_KEYWORD generic_variable_declaration ASSIGN expression
	;

variable_initialization
	: generic_variable_declaration ASSIGN expression
	;

variable_declaration
	: generic_variable_declaration SEMICOLON
	| constant_variable_definition SEMICOLON
	;


/* FUNCTIONS */
parameter
	: generic_variable_declaration
	;

parameter_list
	: parameter
	| parameter SEPARATOR parameter_list
	;

parameter_block
	: PARAMETER_BEGIN PARAMETER_END
	| PARAMETER_BEGIN parameter_list PARAMETER_END
	;

function_definition
	: TYPE identifier parameter_block block
	| TYPE_VOID identifier parameter_block block
	;

// BEGIN TODO
function_call
	:
	;
// END TODO

/* BLOCKS */
block
	: BLOCK_BEGIN statement_list BLOCK_END
	;

/* IF */
if
	: IF_KEYWORD PARAMETER_BEGIN condition PARAMETER_END block
	;

// BEGIN TODO
condition
	:
	;
// END TODO

/* STATEMENTS */
statement_list
	: statement
	| statement statement_list 
	;

statement
	: variable_declaration
	| function_call SEMICOLON
	| assign_operation SEMICOLON
	| if
	| loop
	| RETURN_KEYWORD SEMICOLON
	;

/* PROGRAM */
program_statement
	: variable_declaration
	| function_definition
// BEGIN TODO
	| struct_def
	| array_declaration
// END TODO
	; 

program_list
// BEGIN TODO
	: program_statement
	| program_list program_statement
// END TODO
	;

program
	: program_list
	;
