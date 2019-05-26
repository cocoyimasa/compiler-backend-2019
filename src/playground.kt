package main
enum class Test{
    CLASS1, FIELD1, METHOD1
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
}