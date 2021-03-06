package main
enum class Test{
    CLASS1, FIELD1, METHOD1
}

//似乎不该有这两个类，应该在编译期把未知符号解决掉
// namespace
class PackageStatement: Statement{
    lateinit var packageName : String
    constructor(pkgName: String): super(Token("package")){
        this.packageName = pkgName
    }

    override fun toString(): String {
        return "PackageStatement:$packageName"
    }
}

class ImportStatement: Statement{
    lateinit var packageName : String
    constructor(pkgName: String): super(Token("package")){
        this.packageName = pkgName
    }

    override fun toString(): String {
        return "ImportStatement:$packageName"
    }
}
fun main(){
    var x:Int? = null

    println("Hello world ${x.toString()}")

    var list : Array<String> = arrayOf("sss","111")
    var array: ArrayList<String> = arrayListOf("sss", "1111", "2222")

    println(array)

    var b:Int = 10
    println("$b.$b")

    println(Test.CLASS1)

    println(Test.CLASS1 == Test.CLASS1)

    var str : Any = "111"
    println(str)

    class A{
        fun X(){
            println("test A")
        }
    }
    val obj = A()
    obj.X()
}
/*
var sts = ArrayList<Statement>()
    sts.add(IfStatement(
        condition = BinaryExpression(
            leftExp = Expression(Token("100", "int")),
            operator = Token("<"),
            rightExp = Expression(Token("2", "int"))
        ),
        block = Block(
            ArrayList<Statement>()
        )))
    sts.add(FunctionCall(
        funcName = Token("println", "identifier"),
        params = arrayListOf(Expression(Token("Hello World", "string")))
    ))
    sts.add(AssignStatement(
        varName = Token("testA", "identifier"),
        varType = Token("string", "keyword"),
        exp = Expression(Token("test 1000", "string"))
    ))
    sts.add(FunctionCall(
        funcName = Token("println", "identifier"),
        params = arrayListOf(Expression(Token("testA", "identifier")))
    ))
* */