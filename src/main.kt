package main

fun main(){
    /*
    * if(10>1){
    *   if(100<2){
    *
    *   }
    *   println("Hello World")
    *   testA:string = "test 1000"
    *   println(testA)
    * }
    * */
    var node = Node(Token("program"))
    node.elems.add(IfStatement(
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
        ))
    )

    var codeGenerator = CodeGenerator()
    node.accept(codeGenerator = codeGenerator)

//    println(codeGenerator.toString())
    codeGenerator.toFile()

    var vm : VirtualMachine = VirtualMachine()
    vm.start()

}