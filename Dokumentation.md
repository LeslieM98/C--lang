# C-- Dokumentation

## Scopes
Ein Programm besteht aus mehreren Scopes. Diese wären `Global`, `Lokal`, `Temporär`. Diese besitzen jeweils unterschiedliche Eigenschaften.
 - Global: Funktions-, Variablen- und Konstantendefinitionen erlaubt, Default-scope eines Programms.
 - Lokal: Variablen- und Konstantendefinitionen erlaubt. Default Scope innerhalb funktionen.
 - Temporär: Variablen- und Konstantendefinition erlaubt. Mehrere temporäre Scopes können verschachtelt werden (Wird durch if-else und loops aufgespannt). Variablen die in diesem Scope deklariert werden, haben eine höhere chance weniger speicher zu verbrauchen als andere Variablen. (Siehe scopemanager klasse)

## Typen
Die Sprache besitzt nur einen Daten Typ (`num`) und zwei Rückgabetypen (`num`, `void`).
    
- `num` beschreibt einen Gleitkommawert doppelter Genauigkeit.
- `void` ist nur gültig als Rückgabetyp und beschreibt, dass die Funktion nichts zurück gibt.

## Literale  
Literale sind jegliche konstanten Zahlen die im code auftauchen. Beispielsweise `12.93`, `13`

## Funktionen
Funktionen bestehen aus Kopf und Körper. Die rückgabe eines Wertes erfolgt durch ein Return-Statement. Die Parameterliste besteht aus mehreren deklarationen von variablen mit Namen und Typ jeweils mit Komma getrennt. Void als rückgabetyp beschreibt, dass die Funktion keine daten Rückgibt
 - Header: `<RückgabeTyp> <Identifier>(<ParameterListe>)`  
 - Körper: `{<Statements>}`
 - Return-Statement: `return <Ausdruck>;`  
  
Funktionen können aufgerufen werden.
 - Syntax: `<Identifier>(<Expressionlist>)`

## Konstanten
Konstanten sind werte, die während dem Compile-prozess zwischengespeichert werden. Konstanten sind Scopeabhängig. Es können nur konstanten genutzt werden die im gleichen oder in einem höheren Scope definiert wurden. Konstanten werden im späteren kontext einfach nur substituiert, konstanten haben keinen Typ, sie speichern nur den String der ihnen zugewiesen wird. Einer Konstanten muss im selben statement der wert zugewiesen werden.
 - Syntax: `const num <Identifier> = <Wert>;`

## Boolsche algebra  
Falsch bedeutet `0.0` Wahr bedeutet alles andere. Es werden die meisten herkömmlichen Boolschen operationen unterstützt. In folgender präzedenzreihenfolge.
 - `<`, `>`, `<=`, `>=` : Vergleichsoperatoren zweier werte die in Wahr oder Falsch resultieren.
 - `!` : Negiert einen boolschen ausdruck
 - `==`, `!=` : Prüfung auf #Gleichheit und Ungleichheit (funktioniert für wahrheitswerte und Numerische werte)
 - `&&`, `||` : Boolsche AND, OR verknüpfung.


# Aufgetretene Probleme

## Scopes
### Wie erkennt man ob eine Variable überhaupt gültig ist?  
Mehrere Variablen mit gleichen Namen sind erlaubt. Unser Ansatz ist es, einen Scopemanager einzuführen, sobald ein Scope betreten wird oder verlassen wird wird das dem scopemanager mitgeteilt. Dieser hat mehrere `get` Methoden, denen man nur den identifier der gefragten Variable mitgibt. Dort bekommt man ein Tupel zurück das beschreibt, ob es eine Konstante oder Variable ist, welcher Scope, und jeweilige informationen um auf diese Variable/Konstante zuzugreifen. 
- Der eigentliche Wert im falle einer konstanten, so dass sie im generierten code nurnoch als eingebettete immediate werte vorkommen.
- Eine attributreferenz sofern es sich um eine Globale variable handelt.
- Ein Locals Array index falls es eine Lokale variable ist.
Es ist jedoch nur möglich bei mehrfach deklaration in mehreren scopes die variable des most inner scopes zu verwenden. 

## Funktionen
### Wie unterscheidet man mehrere funktionen, bei erlaubtem Overloading?
Ein Konkretes Beispiel hierzu wäre: Wie kann mein Compiler erkennen dass `num add(num a, num b)` und `num add(num a, num b, num c)` grund auf verschiedene funktionen sind?
Unser ansatz ist es eine Klasse einzuführen, die Metadaten der funktion speichert. In diesem Fall den Identifier, den returntype und die parameter und deren typen.

Der programmvisitor hat nun eine liste von Definierten Funktionen, die anfangs leer ist. Sobald eine funktion deklariert wird wird sie dort hinzugefügt sofern nicht bereits eine funktion mit dem selben Identifier und den gleichen parametertypen existiert. 