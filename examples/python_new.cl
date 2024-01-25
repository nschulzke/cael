struct Foo
struct Bar(String)
struct Baz { property : Bar }

fun zero => Int
  | zero -> 0

let barObj = Bar("test")

let bazObj = Baz { property = Bar("bazTest") }

fun something(Quz) -> String | None # Can be inferred
  | something(Foo) => None
#  | something(Bar(string)) = string
#  | something(Baz { property = Bar(string) }) = string

let test = something(barObj)

fun recordFun { arg1 : String, arg2 : Int } -> String
  | recordFun { arg1 = arg1, arg2 = arg2 }  => arg1

let test2 = recordFun { arg1 = "test", arg2 = 1 }
