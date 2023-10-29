module SubModule is
  struct Foo
  struct Bar(String)
  struct Baz { property : Bar }
end

open SubModule

type Quz =
  | Foo
  | Bar
  | Baz

protocol Test is
  dec testMethod { self : Self } : String
end

extension Foo : Test is
  let testMethod { self = Self } = "Foo"
end

protocol List is
  dec length(Self) : Int
end

extension Foo : List is
  let length(Self) = 0
end

dec zero : Int
let zero = Foo.length()

let foo = Foo.testMethod {}

let barObj = Bar("test")

let bazObj = Baz { property = Bar("bazTest") }


dec something(Quz) : String | Nothing # Can be inferred
let something(Foo) = Nothing
let something(Bar(string)) = string
let something(Baz { property = Bar(string) }) = string

let test = function(barObj)

dec recordFun { arg1 : String, arg2 : Int } : String
let recordFun { arg1 = arg1, arg2 = arg2 } = arg1

let test2 = recordFun { arg1 = "test", arg2 = 1 }

let test3 =
  match test2
    | "test" => "test"
    | _ => "other"

# Keywords: struct type dec let impl for is end match module open
# Punctuation: { } ( ) [ ] , : | => =
# Operators: + - * / % ! ? < > <= >= == != && ||
# Literals: 1 1.0 "string" 'c' true false
# IDENTIFIERs: [a-zA-Z_][a-zA-Z0-9_]*
# Comments: # to end of line

# Below is a BNF grammar for the language described above
# It is not used by the parser, but is here for reference

# program = { moduleMember }
# moduleMember =
#   | moduleDecl
#   | typeDecl
#   | protocolDecl
#   | extensionDecl
#   | bareStructDecl
#   | tupleStructDecl
#   | recordStructDecl
#   | bareDecDecl
#   | tupleDecDecl
#   | recordDecDecl
#   | bareLetDecl
#   | tupleLetDecl
#   | recordLetDecl
# moduleDecl = "module" IDENTIFIER "is" { moduleMember } "end"
# typeDecl = "type" IDENTIFIER "=" type
# protocolDecl = "protocol" IDENTIFIER "is" { protocolMember } "end"
# protocolMember = bareDecDecl | tupleDecDecl | recordDecDecl
# extensionDecl = "extension" type "is" { extensionMember } "end"
# extensionMember = bareLetDecl | tupleLetDecl | recordLetDecl | bareDecDecl | tupleDecDecl | recordDecDecl
# bareStructDecl = "struct" IDENTIFIER
# tupleStructDecl = "struct" IDENTIFIER "(" type { "," type } ")"
# recordStructDecl = "struct" IDENTIFIER "{" IDENTIFIER ":" type { "," IDENTIFIER ":" type } "}"
# bareDecDecl = "dec" IDENTIFIER ":" type
# tupleDecDecl = "dec" IDENTIFIER "(" type { "," type } ")" ":" type
# recordDecDecl = "dec" IDENTIFIER "{" IDENTIFIER ":" type { "," IDENTIFIER ":" type } "}" ":" type
# bareLetDecl = "let" IDENTIFIER "=" expression
# tupleLetDecl = "let" IDENTIFIER "(" IDENTIFIER { "," IDENTIFIER } ")" "=" expression
# recordLetDecl = "let" IDENTIFIER "{" IDENTIFIER ":" IDENTIFIER { "," IDENTIFIER ":" IDENTIFIER } "}" "=" expression
# expression =
#   | literal
#   | "(" expression ")"
#   | bareCallExpr
#   | tupleCallExpr
#   | recordCallExpr
#   | bareExtensionCallExpr
#   | tupleExtensionCallExpr
#   | recordExtensionCallExpr
#   | arithmeticExpr
#   | comparisonExpr
#   | logicalExpr
#   | matchExpr
# literal =
#   | INT_LITERAL
#   | FLOAT_LITERAL
#   | STRING_LITERAL
# bareCallExpr = IDENTIFIER
# tupleLetCallExpr = IDENTIFIER "(" expression { "," expression } ")"
# recordLetCallExpr = IDENTIFIER "{" IDENTIFIER ":" expression { "," IDENTIFIER ":" expression } "}"
# bareExtensionCallExpr = expression "." IDENTIFIER "(" expression ")"
# tupleExtensionCallExpr = expression "." IDENTIFIER "(" expression { "," expression } ")"
# recordExtensionCallExpr = expression "." IDENTIFIER "{" IDENTIFIER ":" expression { "," IDENTIFIER ":" expression } "}"
# arithmeticExpr = expression "+" expression | expression "-" expression | expression "*" expression | expression "/" expression | expression "%" expression
# comparisonExpr = expression "<" expression | expression ">" expression | expression "<=" expression | expression ">=" expression | expression "==" expression | expression "!=" expression
# logicalExpr = expression "&&" expression | expression "||" expression
# matchExpr = "match" expression { "|" pattern "=>" expression } "end"
# pattern =
#   | IDENTIFIER
#   | literal
#   | "(" pattern ")"
#   | tupleStructPattern
#   | recordStructPattern
# tupleStructPattern = IDENTIFIER "(" pattern { "," pattern } ")"
# recordStructPattern = IDENTIFIER "{" IDENTIFIER ":" pattern { "," IDENTIFIER ":" pattern } "}"
# type =
#   | IDENTIFIER
#   | unionType
#   | "(" type ")"
# unionType = type "|" type
