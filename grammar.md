# JsonPath Grammar

```
JsonPath      ::= RootPath | CurrentPath

RootPath      ::= '$' PathTail?
CurrentPath   ::= '@' PathTail?

PathTail      ::= (DotSegment | RecursiveSegment | BracketSegment)*

DotSegment        ::= '.' Identifier
RecursiveSegment  ::= '..' Identifier?
BracketSegment    ::= '[' ( Index | Slice | Union | Filter ) ']'

Index         ::= Number
Slice         ::= Number? ':' Number? (':' Number?)?
Union         ::= (Index | String | Identifier) (',' (Index | String | Identifier))*
Filter        ::= '?' '(' Expression ')'

Expression    ::= OrExpr
OrExpr        ::= AndExpr ( '||' AndExpr )*
AndExpr       ::= CompareExpr ( '&&' CompareExpr )*
CompareExpr   ::= Primary ( CompareOp Primary )?
Primary       ::= Literal | Identifier | FunctionCall | '@' PathTail? | '$' PathTail?

Literal       ::= String | Number | Boolean | Null
FunctionCall  ::= Identifier '(' (Expression (',' Expression)*)? ')'
```