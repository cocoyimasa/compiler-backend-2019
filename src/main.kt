package main

fun main(){
    /*
    * fun main(){
    *   if(10>1){
    *       if(100<2){
    *
    *       }
    *       println("Hello World")
    *       testA:string = "test 1000"
    *       println(testA)
    *   }
    *   var testObj = new User()
    *   println(testObj)
    * }
    * class User{
    * field name:int = 10
    * constructor(val:int){
    *   name = val
    * }
    * method testMethod(val:int){
    *   name = 100
    *   print(val)
    * }
    * */
    var node = Node(Token("program"))
    node.elems.add(FunctionStatement(
            funcName= Token("main"),
            params= arrayListOf<Argument>(),
            returnType= Token("void"),
            block= Block(
                    statements = arrayListOf(
                            IfStatement(
                                    condition = BinaryExpression(
                                            leftExp = Expression(Token("10", "int")),
                                            operator = Token(">"),
                                            rightExp = Expression(Token("1", "int"))
                                    ),
                                    block = Block(
                                            statements = arrayListOf(
                                                    IfStatement(
                                                            condition = BinaryExpression(
                                                                    leftExp = Expression(Token("1", "int")),
                                                                    operator = Token("<"),
                                                                    rightExp = Expression(Token("2", "int"))
                                                            ),
                                                            block = Block(statements = arrayListOf(
                                                                    FunctionCall(
                                                                            funcName = Token("println", "identifier"),
                                                                            params = arrayListOf(Expression(Token("Hello Inner If", "string")))
                                                                    )
                                                            ))
                                                    ),
                                                    FunctionCall(
                                                            funcName = Token("println", "identifier"),
                                                            params = arrayListOf(Expression(Token("Hello World", "string")))
                                                    ),
                                                    AssignStatement(
                                                            varName = Token("testA", "identifier"),
                                                            varType = Token("string", "keyword"),
                                                            exp = Expression(Token("test 1000", "string"))
                                                    ),
                                                    FunctionCall(
                                                            funcName = Token("println", "identifier"),
                                                            params = arrayListOf(Expression(Token("testA", "identifier")))
                                                    )
                                            )
                                    )
                            ),
                            // var testObj = new User()
                            AssignStatement(
                                    varName=Token("testObj"),
                                    varType = Token("User"),
                                    exp = NewObjectExp(
                                            className = Token("User"),
                                            params = arrayListOf()
                                    )
                            ),
                            // println(testObj)
                            FunctionCall(
                                    funcName = Token("println", "identifier"),
                                    params = arrayListOf(Expression(Token("testObj", "identifier")))
                            )
                    )
            ),
            retSt= ReturnStatement()
    ))

    node.elems.add(UserDefinedClassSt(
            pkgName = "",
            className=Token("User"),
            superClass= Token(""),
            superInterfaces = arrayListOf<Token>(),
            fields = arrayListOf<FieldStatement>(
                    FieldStatement(
                            className = Token("User"),
                            fieldName = Token("name"),
                            fieldType = Token("int"),
                            initValue = Expression(Token("10","int"))
                    )
            ),
            constructors = arrayListOf<MethodStatement>(
                    MethodStatement(
                            className= Token("User"),
                            accessLevel= Token(""),
                            modifier= arrayListOf<Token>(),
                            funcName= Token("test"),
                            params= arrayListOf<Argument>(
                                    Argument(
                                            type= Token("int"),
                                            name= Token("val")
                                    )
                            ),
                            returnType= Token("User"),
                            block= Block(
                                    statements = arrayListOf(
                                            AssignStatement(
                                                    varName=Token("name"),
                                                    varType = Token("int"),
                                                    exp = Expression(Token("val","identifier"))
                                            )
                                    )
                            ),
                            retSt= ReturnStatement()
                    )
            ),
            methods= arrayListOf<MethodStatement>(
                    MethodStatement(
                            className= Token("User"),
                            accessLevel= Token(""),
                            modifier= arrayListOf<Token>(),
                            funcName= Token("testMethod"),
                            params= arrayListOf<Argument>(
                                    Argument(
                                            type= Token("int"),
                                            name= Token("val")
                                    )
                            ),
                            returnType= Token("int"),
                            block= Block(
                                    statements = arrayListOf(
                                            AssignStatement(
                                                    varName=Token("name"),
                                                    varType = Token("int"),
                                                    exp = Expression(Token("100", "int"))
                                            ),
                                            // println(val)
                                            FunctionCall(
                                                    funcName = Token("println", "identifier"),
                                                    params = arrayListOf(Expression(Token("val", "identifier")))
                                            )
                                    )
                            ),
                            retSt= ReturnStatement()
                    )
            )
    ))
    var codeGenerator = CodeGenerator()
    node.accept(codeGenerator = codeGenerator)

//    println(codeGenerator.toString())
    codeGenerator.toFile()

    var vm : VirtualMachine = VirtualMachine()
    vm.start()



}