from dataclasses import dataclass
from typing import *
from cael_runtime import singleton


@singleton()
class Foo:
    pass


@dataclass()
class Bar:
    i0: str


@dataclass(kw_only=True)
class Baz:
    property: Bar


zero: Any = 0

barObj: Any = Bar("test")

bazObj: Any = Baz(property=Bar("bazTest"))

def something(i0: Foo, /) -> Any:
    return None


test: Any = something(barObj)

def recordFun(*, arg1: Any, arg2: Any) -> Any:
    return arg1


test2: Any = recordFun(arg1="test", arg2=1)

