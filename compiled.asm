LABEL main
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
NEW AnnoymousObject_2 User
PUSHA AnnoymousObject_2
CALL User::User
STORE testObj User
LOAD testObj
CALL println
RET void
LABEL User
LABEL User::User
PARAM this User
ICONST 10
FIELD name int
PUSHA this
RET
LABEL User::User_1_int_
PARAM this User
PARAM val int
ICONST 10
FIELD name int
LOAD val
STORE name int
PUSHA this
RET
LABEL User::testMethod
PARAM this User
PARAM val int
ICONST 100
STORE name int
LOAD val
CALL println
RET void
LABEL END
ND
