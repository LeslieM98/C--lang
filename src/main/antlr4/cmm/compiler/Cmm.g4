Grammar C--;

assign
	: '='
	;

semicolon
	: ';'
	;

identifier
	: (name_start_char)(name_char)*
	;

name_start_char
	: ('a'..'z') | 'ä' | 'ö' | 'ü'
	| ('A'..'Z') | 'Ä' | 'Ö' | 'Ü'
	| '_'
	;

name_char
	: name_start_char
	| ('0'..'9')
	;

const_keyword
	: 'const'
	;

type
	: 'int'
	| 'float'
	| 'char'
	;

type_void:
	: 'void'
	;

separator
	: ','
	;

parameter_begin:
	: '('
	;

parameter_end:
	: ')'
	;

block_begin:
	: '{'
	;

block_end:
	: '}'
	;

/* VARIABLES*/
CONSTANT_VARIABLE_DEFINITION
	: const_keyword GENERIC_VARIABLE_DECLARATION assign EXPRESSION
	;

VARIABLE_INITIALIZATION
	: GENERIC_VARIABLE_DECLARATION assign EXPRESSION
	;

GENERIC_VARIABLE_DECLARATION
	: type identifier
	;

VARIABLE_DECLARATION
	: GENERIC_VARIABLE_DECLARATION semicolon
	| CONSTANT_VARIABLE_DEFINITION semicolon
	;


/* FUNCTIONS */
PARAMETER
	: GENERIC_VARIABLE_DECLARATION
	;

PARAMETER_LIST
	: PARAMETER
	| PARAMETER separator PARAMETER_LIST
	;

PARAMETER_BLOCK
	: parameter_begin parameter_end
	| parameter_begin PARAMETER_LIST parameter_end
	;

FUNCTION_DEFINITION
	: type identifier PARAMETER_BLOCK BLOCK
	| type_void identifier PARAMETER_BLOCK BLOCK
	;

// BEGIN TODO
FUNCTION_CALL
	:
	;
// END TODO

/* BLOCKS */
BLOCK
	: block_begin STATEMENT_LIST block_end
	;

/* IF */
IF
	: if_keyword parameter_begin CONDITION parameter_end BLOCK
	;

// BEGIN TODO
CONDITION
	:
	;
// END TODO

/* STATEMENTS */
STATEMENT_LIST
	: STATEMENT
	| STATEMENT_LIST STATEMENT
	;

STATEMENT
	: VARIABLE_DECLARATION
	| FUNCTION_CALL semicolon
	| ASSIGN_OPERATION semicolon
	| IF
	| LOOP
	| RETURN semicolon
	;

/* GLOBALS */
GLOB_STMNT
	: VARIABLE_DECLARATION
	| FUNCTION_DEFINITION
// BEGIN TODO
	| STRUCT_DEF
	| ARR_DEC
// END TODO
	; 

PROGRAM_LIST
// BEGIN TODO
	: PROGRAM_STATEMENT
	| PROGRAM_LIST PROGRAM_STATEMENT
// END TODO
	;

PROGRAM
	: PROGRAM_LIST
	;
