struct Foo
struct Bar(String)
struct Baz { property : Bar }

dec zero : Int
let zero = 0

let barObj = Bar("test")

let bazObj = Baz { property = Bar("bazTest") }

dec something(Quz) : String | None # Can be inferred
let something(Foo) = None
# let something(Bar(string)) = string
# let something(Baz { property = Bar(string) }) = string

let test = something(barObj)

dec recordFun { arg1 : String, arg2 : Int } : String
let recordFun { arg1 = arg1, arg2 = arg2 } = arg1

let test2 = recordFun { arg1 = "test", arg2 = 1 }
