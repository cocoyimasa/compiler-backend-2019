ICONST 10
ICONST 1
GT
JMPF Label_If_False0
ICONST 1
ICONST 2
LT
JMPF Label_If_False1
STRING Hello Inner If
CALL println
LABEL Label_If_False1
STRING Hello World
CALL println
STRING test 1000
STORE testA string
LOAD testA
CALL println
LABEL Label_If_False0
