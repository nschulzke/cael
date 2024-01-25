let generic = match None
  | None => "none"
  | Some(Int) => "int"
  | Some(string) => string

fun f1(Foo) => "foo"

let matchFun = match f1
  | (Foo) -> String => "foo"
  | (Bar) -> String => "bar"
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

# If we require that patterns always be exhaustive (which we should), then we can statically
# determine types.
