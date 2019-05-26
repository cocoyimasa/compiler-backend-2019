package main

import java.io.File
import java.util.*
import kotlin.collections.ArrayList

data class MemItem(var name : String = "",
                     var value: String = "",
                     var type : String = "") {}

class VirtualMachine{
    var programCounter : Int = 0;
    var symbolTable : Scope = Scope("global")
    // built-in type list
    var builtInTypes = arrayListOf<String>()
    var builtInFuncs = arrayListOf<String>()
    var userDefinedTypes = arrayListOf<String>()
    // memory
    var heap = mutableMapOf<String, MemItem>()
    var stack = mutableMapOf<String, MemItem>()
    // runtime stack
    var runtimeStack: Stack<MemItem> = Stack()

    var irArray: ArrayList<IR> = arrayListOf<IR>()
    var labelMap = mutableMapOf<String, Int>()

    constructor(){
        this.fromFile()
        print(this.irToString())
        this.findAllLabels()
    }

    fun start(){
        // init built-in type list
        val types = arrayOf("String", "Object")
        types.forEach {
            type -> run{
                builtInTypes.add(type)
            }
        }
        val funcs = arrayOf("println", "print", "read", "readLine")

        funcs.forEach {
            func -> run{
                builtInFuncs.add(func)
            }
        }

        this.run()
    }


    fun fromArray(_irArray: ArrayList<IR>){
        this.irArray.addAll(_irArray)
    }

    fun fromFile(filename : String = "compiled.asm"){
        val file = File(filename)
        //指定文件不存在就创建同名文件
        if (!file.exists())
            file.createNewFile()

        file.readLines().forEach {

            val arr = it.split(" ")
            var ir : IR? = null
            when(arr.size){
                1 -> ir = IR(arr[0])
                2 -> {
                    ir = IR(arr[0], arr[1])
                }
                3 -> {
                    ir = IR(arr[0], arr[1], arr[2])
                }
                else ->{
                    ir = IR(arr[0])
                }
            }
            if(!ir.isEmpty()){
                //字符串可能也是以空格隔开，这样会当做多个操作数。而不是一个整体
                //所以要特殊处理
                if(ir.operator == "PUSHS"
                    || ir.operator == "STRING"
                    || ir.operator == "POPS"
                ){
                    val len = ir.operator.length
                    val str = it.substring(len+1, it.length)
                    val op = ir.operator
                    ir = IR(
                        op,
                        str
                    )
                }
                else if(ir.operator.equals("STORE")){
                    val len = ir.operator.length
                    val typeLen = arr.last().length
                    val str = it.substring(len+1, it.length-1-typeLen)
                    val op = ir.operator
                    ir = IR(op, str, arr.last())
                }
                irArray.add(ir)
            }
        }
    }

    fun irToString() : String{
        val sb = StringBuffer("")

        for(ir in irArray){
            sb.append("${ir.toString()}\n")
        }

        return sb.toString()
    }

    fun findAllLabels(){
        var index = 0
        // 多行lambda expression需要前面加run才能运行
        irArray.forEach {
            ir->
            if (ir.operator == "LABEL") {
                val opr = ir.dest
                labelMap[opr] = index
            }
            index++
        }
        println(labelMap)
    }

    fun excuteBuiltinFunc(funcName : String) : String?{
        var res :String? = null
        if(funcName == "println"){
            val item = pop()
            println(item.value)
        }
        else if(funcName == "print"){
            val item = pop()
            print(item.value)
        }
        else if(funcName == "read"){
            res = readLine()
        }
        else if(funcName == "readLine"){
            res = readLine()
        }
        return res
    }

    fun isPrimitive(type:String): Boolean {
        if(type == "int"
            || type == "float"
            || type == "boolean"
        ){
            return true
        }
        return false
    }

    // load现在stack上查找变量，如果发现是引用类型，则去堆上取值
    fun load(ref: String): MemItem?{
        val item = stack[ref]

        if(item != null && isPrimitive(item.type)){
            return item
        }
        else if(item != null && !isPrimitive(item.type)){
            return heap[ref]
        }
        else{
            // error
            return null
        }
    }

    fun lookupFunc(func: String): Boolean {
        for(builtIn in builtInFuncs){
            if(builtIn == func){
                return true
            }
        }
        return false
    }

    fun lookupTypeList(type: String): Boolean {
        for(builtIn in builtInTypes){
            if(builtIn == type){
                return true
            }
        }
        for(userDefined in userDefinedTypes){
            if(userDefined == type){
                return true
            }
        }

        return false
    }

    fun newObject(ref: String, type: String){
        // 查看自定义类型和系统内置类型列表， 如果类型存在，则初始化对象成员变量，在堆上保存引用，如果不存在，报错
        val isType = lookupTypeList(type)

        if(isType){
            heap[ref] = MemItem(name=ref, type=type)
        }
        else{
            // error: required type is not defined
        }
    }

    // 把基础类型和引用类型在stack上做个索引
    fun store(ref: String, type: String, value: String){
        // 引用类型在堆上的创建和成员变量相同
        // 成员变量创建直接在堆上 对象名.成员变量
        // 而对于引用类型user = new User(name, age, sex)来说
        // 前面new指令已经在堆上创建了user.name, user.age, user.sex
        // 这样对象的实现可以很简单，不过就是个命名空间
        // 所以这里只需要存上user和user的类型即可
        // 需要访问field的时候，直接加上.fieldname去堆上找就行了。反正不可能重名，重名必报错。
        if(isPrimitive(type)){
            stack[ref] = MemItem(name=ref, type=type, value = value)
        }
        else{
            stack[ref] = MemItem(name=ref, type=type) // 引用类型只作一个类型索引
            heap[ref] = MemItem(name=ref, type=type, value = value) // 无值添加值，有值则更新
        }
    }

    fun push(value: String, type: String){
        val item = MemItem()
        item.value = value
        item.type = type
        runtimeStack.push(item)
    }

    fun push(item: MemItem){
        runtimeStack.push(item)
    }

    fun pop(): MemItem{
        return runtimeStack.pop()
    }

    fun callFunction(funcName: String){

    }

    fun callMethod(funcName: String, ref: String){
        // ref替换this指针，用来在堆上生成成员变量的引用
    }

    fun addFloat(temp1:MemItem, temp2: MemItem) : MemItem{
        return computeFloat(temp1, temp2, "+")
    }

    fun subFloat(temp1:MemItem, temp2: MemItem) : MemItem{
        return computeFloat(temp1, temp2, "-")
    }

    fun mulFloat(temp1:MemItem, temp2: MemItem) : MemItem{
        return computeFloat(temp1, temp2, "*")
    }

    fun divFloat(temp1:MemItem, temp2: MemItem) : MemItem{
        return computeFloat(temp1, temp2, "/")
    }

    fun computeInt(temp1:MemItem, temp2: MemItem, op:String) : MemItem{
        val item = MemItem()
        item.type = "int"

        var res = 0
        val a = temp2.value.toInt()
        val b = temp1.value.toInt()

        when(op){
            "+"-> res = a + b
            "-"-> res = a - b
            "*"-> res = a * b
            "/"-> res = a / b
        }

        item.value = res.toString()
        return item
    }

    fun computeFloat(temp1:MemItem, temp2: MemItem, op:String) : MemItem{
        val item: MemItem = MemItem()
        item.type = "float"

        var res = 0.0f
        val a: Float
        val b: Float

        a = if(temp1.type == ("float")){
            temp1.value.toFloat()
        }
        else{
            temp1.value.toInt().toFloat()
        }

        b = if(temp2.type == ("float")){
            temp2.value.toFloat()
        } else{
            temp2.value.toInt().toFloat()
        }

        when(op){
            "+"-> res = a + b
            "-"-> res = a - b
            "*"-> res = a * b
            "/"-> res = a / b
        }

        item.value = res.toString()
        return item
    }

    fun add(temp1:MemItem, temp2: MemItem) : MemItem{
        var item = MemItem()
        if(temp1.type == "int" && temp2.type == "int"){
            item = computeInt(temp1, temp2, "+")
        }
        else if(temp1.type == "string" || temp2.type == "string"){
            item.type = "string"
            item.value = "${temp1.value}${temp2.value}"
        }
        else{
            item = addFloat(temp1,temp2)
        }
        return item
    }

    fun sub(temp1:MemItem, temp2: MemItem) : MemItem{
        // 参数计算顺序相反，因为数据入栈是前面的数先入栈，所以出栈之后是第二个
        val item = if(temp1.type == "int" && temp2.type == "int"){
            computeInt(temp1, temp2, "-")
        } else{
            subFloat(temp2,temp1)
        }
        return item
    }

    fun mul(temp1:MemItem, temp2: MemItem) : MemItem{
        // 参数计算顺序相反，因为数据入栈是前面的数先入栈，所以出栈之后是第二个
        val item = if(temp1.type == "int" && temp2.type == "int"){
            computeInt(temp1, temp2, "*")
        } else{
            mulFloat(temp2,temp1)
        }
        return item
    }

    fun div(temp1:MemItem, temp2: MemItem) : MemItem{
        // 参数计算顺序相反，因为数据入栈是前面的数先入栈，所以出栈之后是第二个
        val item = if(temp1.type == "int" && temp2.type == "int"){
            computeInt(temp1, temp2, "/")
        } else{
            divFloat(temp2,temp1)
        }
        return item
    }

    fun eq(temp1:MemItem, temp2: MemItem) : Boolean {
        return temp1.type == temp2.type && temp1.value == temp2.value
    }

    fun uneq(temp1:MemItem, temp2: MemItem) : Boolean {
        return temp1.type != temp2.type || temp1.value != temp2.value
    }

    fun computeCompareExp(temp1:MemItem, temp2: MemItem, op: String) : Boolean{
        val a:Float
        val b:Float

        a = if(temp1.type == "float"){
            temp1.value.toFloat()
        }
        else{
            temp1.value.toInt().toFloat()
        }

        b = if(temp2.type == "float"){
            temp2.value.toFloat()
        }
        else{
            temp2.value.toInt().toFloat()
        }
        // 参数计算顺序相反，因为数据入栈是前面的数先入栈，所以出栈之后是第二个
        when(op){
            "<" -> return b < a
            "<=" -> return b <= a
            ">" -> return b > a
            ">=" -> return b >= a

        }
        return false
    }

    fun lt(temp1:MemItem, temp2: MemItem) : Boolean {
        return computeCompareExp(temp1, temp2, "<")
    }

    fun gt(temp1:MemItem, temp2: MemItem) : Boolean {
        return computeCompareExp(temp1, temp2, ">")
    }

    fun le(temp1:MemItem, temp2: MemItem) : Boolean {
        return computeCompareExp(temp1, temp2, "<=")
    }

    fun ge(temp1:MemItem, temp2: MemItem) : Boolean {
        return computeCompareExp(temp1, temp2, ">=")
    }

    fun andExp(temp1:MemItem, temp2: MemItem) : Boolean {
        val a : Boolean = temp1.value.toBoolean()
        val b : Boolean = temp2.value.toBoolean()

        return temp1.type == temp2.type && a && b
    }

    fun orExp(temp1:MemItem, temp2: MemItem) : Boolean {
        val a : Boolean = temp1.value.toBoolean()
        val b : Boolean = temp2.value.toBoolean()

        return temp1.type == temp2.type && a || b
    }

    fun notExp(temp:MemItem) : Boolean {
        val a : Boolean = temp.value.toBoolean()
        return !a
    }

    fun runDirective(ir: IR){

        when(ir.operator){
            //data instructions
            "ICONST" ->{
                val item = MemItem()
                item.value = ir.dest
                item.type = "int"
                runtimeStack.push(item)
            }
            "FCONST" ->{
                val item = MemItem()
                item.value = ir.dest
                item.type = "float"
                runtimeStack.push(item)
            }
            "BCONST" ->{
                val item = MemItem()
                item.value = ir.dest
                item.type = "boolean"
                runtimeStack.push(item)
            }
            "STRING" ->{
                val item = MemItem()
                item.value = ir.dest
                item.type = "string"
                runtimeStack.push(item)
            }
            "NEW" ->{
                val identifier = ir.dest
                val type = ir.src
                newObject(identifier, type)
            }
            "INIT" ->{
                val objName = ir.dest
                val className = ir.src
                callMethod("$className.$className", objName)
            }
            "STORE" ->{
                // var a = new A(10, 10)
                // NEW a A //将空对象暂存在stack里
                // PUSH init-params
                // INIT a A
                // PUSHA a

                // get a's value from stack
                // STORE identifier TYPE
                val identifier = ir.dest
                var type: String? = ir.src
                val item = pop()
                if(type.equals("")){
                    // 去符号表中查找类型
                    type = symbolTable.lookup(identifier)
                }
                // 这种表达式可以的
                type?.let { store(identifier, it, item.value) }
            }
            "LOAD" ->{
                // a
                // POPA a
                // LOAD a
                val identifier = ir.dest
                val item = load(identifier)
                if(item!= null){
                    push(item)
                }
                else{
                    // error:变量未定义
                    println("error:变量未定义")
                }
            }
            "PUSH" ->{
                val identifier = ir.dest
                push(identifier, "int")
            }
            "PUSHF" ->{
                val identifier = ir.dest
                push(identifier, "float")
            }
            "PUSHB" ->{
                val identifier = ir.dest
                push(identifier, "boolean")
            }
            "PUSHS" ->{
                val identifier = ir.dest
                push(identifier, "string")
            }
            "POP" ->{
                pop()
            }
            //
            "MOV" ->{

            }
            "ADD" ->{
                // ADD不能带参数，只有带类型的ADD才能带参数
                val temp1 = pop()
                val temp2 = pop()
                val res = add(temp1, temp2)
                push(res)
            }
            "ADDF" ->{
                val temp1 = MemItem(value = ir.dest, type="float")
                val temp2 = MemItem(value = ir.src, type="float")
                val res = addFloat(temp1, temp2)
                push(res)
            }
            "ADDS" ->{
                var res = MemItem(type = "string")
                if(ir.count == 2){
                    res.value = "${ir.dest}${ir.src}"
                }
                else if(ir.count == 1){
                    val temp1 = MemItem(value = ir.dest, type="string")
                    val temp2 = pop()
                    res = add(temp1, temp2)
                }
                push(res)
            }
            "SUB" ->{
                val temp1 = pop()
                val temp2 = pop()
                val res = sub(temp1, temp2)
                push(res)
            }
            "MUL" ->{
                val temp1 = pop()
                val temp2 = pop()
                val res = mul(temp1, temp2)
                push(res)
            }
            "DIV" ->{
                val temp1 = pop()
                val temp2 = pop()
                val res = div(temp1, temp2)
                push(res)
            }
            "EQ" ->{
                val temp1 = pop()
                val temp2 = pop()
                val res = eq(temp1, temp2)
                push(MemItem(value = res.toString(), type = "boolean"))
            }
            "UNEQ" ->{
                val temp1 = pop()
                val temp2 = pop()
                val res = eq(temp1, temp2)
                push(MemItem(value = res.toString(), type = "boolean"))
            }
            "LT" ->{
                val temp1 = pop()
                val temp2 = pop()
                val res = lt(temp1, temp2)
                push(MemItem(value = res.toString(), type = "boolean"))
            }
            "GT" ->{
                val temp1 = pop()
                val temp2 = pop()
                val res = gt(temp1, temp2)
                push(MemItem(value = res.toString(), type = "boolean"))
            }
            "LE" ->{
                val temp1 = pop()
                val temp2 = pop()
                val res = le(temp1, temp2)
                push(MemItem(value = res.toString(), type = "boolean"))
            }
            "GE" ->{
                val temp1 = pop()
                val temp2 = pop()
                val res = ge(temp1, temp2)
                push(MemItem(value = res.toString(), type = "boolean"))
            }
            "AND" ->{
                val temp1 = pop()
                val temp2 = pop()
                val res = andExp(temp1, temp2)
                push(MemItem(value = res.toString(), type = "boolean"))
            }
            "OR" ->{
                val temp1 = pop()
                val temp2 = pop()
                val res = orExp(temp1, temp2)
                push(MemItem(value = res.toString(), type = "boolean"))
            }
            "NOT" ->{
                val temp = pop()
                val res = notExp(temp)
                push(MemItem(value = res.toString(), type = "boolean"))
            }
            "LABEL" ->{
                // not operation
            }
            "JMP" ->{
                val label = ir.dest
                val pos : Int? = labelMap[label]
                if(pos != null){
                    programCounter = pos - 1
                }
            }
            "JMPF" ->{
                val label = ir.dest
                val pos : Int? = labelMap[label]
                val temp = pop()
                val res = temp.value.toBoolean()
                if(!res && pos != null){
                    programCounter = pos - 1
                }
            }
            "JMPT" ->{
                val label = ir.dest
                val pos : Int? = labelMap[label]
                val temp = pop()
                val res = temp.value.toBoolean()
                if(res && pos != null){
                    programCounter = pos - 1
                }
            }
            "CALL" ->{
                val funcName = ir.dest
                if(lookupFunc(funcName)){
                    excuteBuiltinFunc(funcName)
                }
            }

        }
        programCounter++
    }

    fun run(){
        println("<<<============KVM============>>>")
        while(programCounter < irArray.size){
            runDirective(irArray[programCounter])
        }
        println("<<<============KVM============>>>\nexit...")
    }
}