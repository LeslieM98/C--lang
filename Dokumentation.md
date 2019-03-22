# C-- Dokumentation
## Grundsätzliches
Die C-- Sprache beachtet keinen Leerraum. Für den Compiler ist es unerheblich, ob eine bestimmte Einrückung eingehalten wird, oder ob nach dem Abschluss eines Statements durch `;` ein Linefeed-Zeichen gesetzt wird. Natürlich hilft dies jedoch bei der Strukturierung des Programms und wird von den Entwicklern der Sprache wärmstens empfohlen.
Die Sprache setzt sich aus Statements zusammen, die in der Regel durch `;` abgeschlossen werden (vgl. C).

## Scopes
Ein Programm besteht aus mehreren Scopes. Diese wären `Global`, `Lokal`, `Temporär`. Diese besitzen jeweils unterschiedliche Eigenschaften.
 - Global: Funktions-, Variablen- und Konstantendefinitionen erlaubt, Default-scope eines Programms.
 - Lokal: Variablen- und Konstantendefinitionen erlaubt. Default Scope innerhalb von Funktionen.
 - Temporär: Variablen- und Konstantendefinition erlaubt. Mehrere temporäre Scopes können verschachtelt werden (Wird durch if-else-Statements und Schleifen aufgespannt). Variablen die in diesem Scope deklariert werden, haben eine höhere Chance weniger Speicher zu verbrauchen als andere Variablen (siehe ScopeManager Klasse).

## Typen
Die Sprache besitzt nur einen Datentyp (`num`) und zwei Rückgabetypen (`num`, `void`).
    
- `num` beschreibt einen ganzzahligen Wert wie er aus der Sprache C bekannt ist.
- `void` ist nur gültig als Rückgabetyp und beschreibt, dass die Funktion nichts zurück gibt.

## Literale  
Literale sind jegliche konstanten Zahlen die im Code auftauchen. Beispielsweise `13`.


## Konstanten
Konstanten sind Werte, die während dem Compileprozess zwischengespeichert werden. Konstanten sind scopeabhängig. Es können nur Konstanten genutzt werden die im gleichen oder in einem höheren Scope definiert wurden. Konstanten werden im späteren Kontext einfach nur substituiert. Konstanten haben keinen Typ, sie speichern nur den String der ihnen zugewiesen wird. Einer Konstanten muss im selben Statement der Wert zugewiesen werden.
 - Syntax: `const num <Identifier> = <Wert>;`

## Variablen
Variablen stellen veränderbare Speicherbereiche dar, die der Programmierer nutzen kann, um darin Literale zu speichern und diese in weiteren Rechnungen zu benutzen und im Gegensatz zu Konstanten auch überschreiben kann. Die Sichtbarkeit einer Variablen ist von dem Scope abhängig, in dem sie definiert wurde.
- Syntax: `num <Identifier> = <Wert>;`

## Standard Arithmetik
Es werden die arithmetischen Operationen `+`, `-`, `*`, `/` für ganzzahlige Datentypen unterstützt. Für die Operation `/` bedeutet dies jedoch, dass keine Rundung vorgenommen wird. Teilt man `5/3` so erhält man nicht, wie vielleicht erwartet, das korrekt gerundete Ergebnis `2`, sondern stets das nach unten abgerundete Ergebnis und damit in diesem Beispiel `1`.

## Boolsche Algebra  
Der Wert `0` entspricht einem Wahrheitswert von Falsch wohingegen alles andere als Wahr aufgefasst wird. Es werden die meisten herkömmlichen Boolschen Operationen unterstützt. In folgender Präzedenzreihenfolge.
 - `<`, `>`, `<=`, `>=` : Vergleichsoperatoren zweier Werte die in Wahr oder Falsch resultieren.
 - `!` : Negiert einen boolschen ausdruck
 - `==`, `!=` : Prüfung auf Gleichheit und Ungleichheit (funktioniert für Wahrheitswerte und Numerische Werte)
 - `&&`, `||` : Boolsche AND- und OR-Verknüpfung.

## Branch-Statements (If-Else)
Die Sprache C-- unterstützt die Formulierung von Code der nur unter vorherigen Bedingungen ausgeführt wird. Dazu wird ein If-Else-Konstrukt genutzt. Dabei ist es möglich, den Else-Zweig des Statements wegzulassen. Hierbei sind die Wahrheitswerte, die im Abschnitt *Boolsche Algebra* eingeführt wurden, für die `Bedingung` zu beachten.
- Syntax: `if(<Bedingung>) {<Statements>} [else {<Statements>}]`

## Loop-Statement (loop)
Das Loop-Statement sorgt dafür, dass ein bestimmter Codeblock so oft ausgeführt wird, bis die formulierte Bedingung den Wahrheitswert 0, also Falsch, liefert. Die Bedingung wird vor jedem Schleifeneintritt überprüft, sodass es auch möglich ist, dass die Schleife nicht ein einziges Mal durchlaufen wird.
- Syntax: `loop(<Bedingung>) {<Statements>}`

## Funktionen
Funktionen bestehen aus Kopf und Körper. Die Rückgabe eines Wertes erfolgt durch ein Return-Statement. Die Parameterliste besteht aus mehreren Deklarationen von Variablen mit Namen und Typ jeweils durch Komma getrennt. Void als Rückgabetyp beschreibt, dass die Funktion keine Daten zurückliefert.
 - Kopf: `<Rückgabetyp> <Identifier>(<ParameterListe>)`
 - Körper: `{<Statements>}`
 - Return-Statement: `return <Ausdruck>;`
  
Funktionen können aufgerufen werden.
 - Syntax: `<Identifier>(<AusdruckListe>)`


### Besondere Funktionen
- Die Sprache besitzt eine eingebaute Ausgabefunktion, die einen Wert auf der Kommandozeile ausgibt. Diese Funktion heißt `println()` und nimmt als Übergabeparameter ein Konstrukt, das zu einem Wert evaluiert werden kann. D.h. Es können z.B. boolsche Berechnungen durchgeführt werden, deren Ergebnisse als `0` oder als `1` auf der Konsole zu sehen sind. Alternativ können auch Literale oder Variablen/Konstanten und allgemein Ausdrücke ausgegeben werden.
- Die Funktion `main()` stellt eine Besonderheit dar. Wie in C ist sie der Einstiegspunkt des Programms.


# Aufgetretene Probleme

## Scopes
### Wie erkennt man ob eine Variable gültig ist?  
Mehrere Variablen mit gleichen Namen sind erlaubt. Unser Ansatz ist es, einen Scopeanager einzuführen, der sobald ein Scope betreten wird oder verlassen wird diese Änderungen erfasst und darauf reagieren kann. Dieser hat mehrere `get` Methoden, denen man nur den Identifier der gefragten Variable übergibt. Dieser Aufruf liefert ein Tupel zurück das beschreibt, ob es sich um eine Konstante oder Variable handelt, welcher Scope zugrunde liegt und jeweilige Informationen um auf diese Variable/Konstante zuzugreifen. 
- Der eigentliche Wert im Falle einer Konstanten, so dass sie im generierten code nur noch als eingebettete Immediate Werte vorkommen.
- Eine attributreferenz, sofern es sich um eine globale Variable handelt.
- Ein Locals Array Index falls es eine lokale Variable ist.
Es ist bei mehrfach Deklarationen jedoch nur möglich auf die Variable des innersten zugrunde liegenden Scopes zuzugreifen. 

## Funktionen
### Wie unterscheidet man mehrere Funktionen, bei erlaubtem Overloading?
Ein konkretes Beispiel hierzu wäre: Wie kann mein Compiler erkennen dass `num add(num a, num b)` und `num add(num a, num b, num c)` Grund auf verschiedene Funktionen sind?
Unser Ansatz ist es eine Klasse einzuführen, die Metadaten der Funktion speichert. In diesem Fall den Identifier, den Returntyp und die Parameter und deren Typen. Diese Metadaten beschreiben die Signatur der Methode und diese Signatur muss innerhalb eines Programms eindeutig sein.

Der ProgrammVisitor hat nun eine Liste von definierten Funktionen, die anfangs leer ist. Sobald eine deklarierte Funktion gefunden wird, wird sie dieser Liste hinzugefügt, sofern nicht bereits eine Funktion mit der gleichen Signatur in dieser Liste vorhanden ist.
