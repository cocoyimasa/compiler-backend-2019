LABEL main
PUSH 10
PUSH 1
GT
JMPF Label_If_False0
PUSH 1
PUSH 2
LT
JMPF Label_If_False1
PUSHS Hello Inner If
CALL println
LABEL Label_If_False1
PUSHS Hello World
CALL println
PUSHS test 1000
STORE testA string
PUSHA testA
CALL println
LABEL Label_If_False0
NEW AnnoymousObject_2 User
PUSHA AnnoymousObject_2
CALL User::User
STORE testObj User
PUSHA testObj
CALL println
RET void
LABEL User
LABEL User::User
PARAM this User
PUSH 10
FIELD name int
PUSHA this
RET
LABEL User::User_1_int_
PARAM this User
PARAM val int
PUSH 10
FIELD name int
PUSHA val
STORE name int
PUSHA this
RET
LABEL User::testMethod
PARAM this User
PARAM val int
PUSH 100
STORE name int
PUSHA val
CALL println
RET void
LABEL END
