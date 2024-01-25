# Conceptually, struct defines both a `fun` and a `pat fun`
# The `fun` produces an object that can be matched by the `pat fun`

struct Foo
struct Bar(Foo)
struct Baz { foo = Foo }

pat let Quz =
  | Foo
  | Bar
  | Baz

struct None
struct Some(_)

pat fun Option(T) => None | Some(T)

# A `pat fun` used without arguments is treated as though we bound _ to all arguments

let generic = match None
  | None => "none"
  | Some(Int) => "int"
  | Some(string : String) => string

fun f1(Foo) => "foo"

let matchFun = match f1
  | (Foo) -> String => "foo"
  | (Bar) -> String => "bar" # Our system should be able to tell that this is impossible
  | (Baz) -> String => "baz"

fun f2
  | (Foo) => "foo"
  | (Bar(_)) => "bar"
  | (Baz { _ = Foo }) => "baz"

fun f3 # This is the same as f2
  | (Foo) => "foo"
  | (Bar) => "bar"
  | (Baz) => "baz"

fun f4
  | { foo = Foo } => "foo"
  | { bar = Bar(_) } => "bar"
  | { bar = Baz { _ = Foo } } => "baz"

fun f5 # This is the same as f4
  | { foo = Foo } => "foo"
  | { bar = Bar } => "bar"
  | { bar = Baz } => "baz"

fun f6() => None

fun f7 { } => None

fun f8 (Foo) | (Bar) => "a"

fun f9
  | (Foo) | (Bar) => "a"
  | (Baz) | (None) => "b"

let l1 = Foo

let l2 = match l1
  | Foo => "foo"
  | Bar(_) => "bar"
  | Baz { _ = Foo } => "baz"

let anon = fun
  | (Foo) => "foo"
  | (Bar) => "bar"
  | (Baz) => "baz"

# Pattern operators:
# the `:` operator is the "matches" operator: the name on the lhs is bound iff the pattern on the rhs is matched
# the `|` operator is the "or" operator: it matches if its lhs or its rhs matches
# the `&` operator is the "and" operator: it matches if its lhs and its rhs match
# the `!` operator is the "not" operator: it matches if its rhs does not match
