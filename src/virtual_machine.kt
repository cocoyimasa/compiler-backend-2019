package main

import java.io.File
import java.util.*
import kotlin.collections.ArrayList

fun isPrimitive(type:String): Boolean {
    if(type == "int"
            || type == "float"
            || type == "boolean"
    ){
        return true
    }
    return false
}
class ClassObject{
    var self = mutableMapOf<String, Any>()
    var className : String = ""
    constructor(className:String = ""){
        this.className = className
    }
    override fun toString(): String {
        val sb = StringBuffer("$className@[")
        for(item in self){
            sb.append(item.key+"-")
            sb.append(item.value.toString()+"-")
        }
        sb.append("]")
        return sb.toString()
    }
}

data class MemItem(var name : String = "",
                     var value: Any= "",
                     var type: String = "") {}

class StackFrame{
    lateinit var frameName :String
    var localVars = mutableMapOf<String, MemItem>()
    var stack : Stack<MemItem> = Stack()

    constructor(frameName:String = ""){
        this.frameName = frameName
    }

    fun push(item : MemItem){
        stack.push(item)
    }

    fun pop(): MemItem {
        return stack.pop()
    }

    fun clear(){
        stack.clear()
    }

    fun top(): MemItem{
        return stack.peek()
    }
    // overload operator []
    operator fun get(name : String) : MemItem? {
        return localVars[name]
    }
    operator fun set(name : String, value: MemItem){
        localVars[name] = value
    }
}

class VirtualMachine{
    var programCounter : Int = 0;
    var symbolTable : Scope = Scope("global")
    // built-in type list
    var builtInTypes = arrayListOf<String>()
    var builtInFuncs = arrayListOf<String>()
    // user-defined types
    var userDefinedTypes = arrayListOf<String>()
    var classInfoMap = mutableMapOf<String, UserDefinedClassSt>()
    // memory
    var heap = mutableMapOf<String, MemItem>()
    var stack = Stack<StackFrame>()
    // runtime stack
    var runtimeStack: Stack<MemItem> = Stack()
    var currentFrame = StackFrame("Global")

    // registers
    var retReg = 0

    var irArray: ArrayList<IR> = arrayListOf<IR>()
    var labelMap = mutableMapOf<String, Int>()

    constructor(){
        this.fromFile()
        print(this.irToString())
        this.findAllLabels()
    }

    fun start(){
        stack.push(StackFrame("__Main__"))
        programCounter = labelMap["main"] ?: 0
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

    // load现在stack上查找变量，如果发现是引用类型，则去堆上取值
    fun load(ref: String): MemItem?{
        val item = currentFrame[ref]

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
        // 类型检查应该放在编译期，这里直接开辟空间
        val classObject = ClassObject(type)
        store(ref, type, classObject)
    }

    // 把基础类型和引用类型在stack上做个索引
    fun store(ref: String, type: String, value: Any){
        if(isPrimitive(type)){
            currentFrame[ref] = MemItem(name=ref, type=type, value = value)
        }
        else{
            currentFrame[ref] = MemItem(name=ref, type=type) // 引用类型只作一个类型索引
            heap[ref] = MemItem(name=ref, type=type, value = value)
            // heap[ref] = Object{value=[a=3,b=2,c=1]}
            // 所以ref.b会被转化为heap[ref][b]
            // 方法调用ref.func会被转化为Class::func(ref,....)
        }
    }

    fun storeHeap(ref: String, type: String, value: Any){
        currentFrame[ref] = MemItem(name=ref, type=type) // 引用类型只作一个类型索引
        heap[ref] = MemItem(name=ref, type=type, value = value) // 无值添加值，有值则更新
    }

    fun push(value: String, type: String){
        val item = MemItem()
        item.value = value
        item.type = type
        currentFrame.push(item)
    }

    fun push(item: MemItem){
        currentFrame.push(item)
    }

    fun pop(): MemItem{
        return currentFrame.pop()
    }

    fun clearRuntimeStack(){
        return currentFrame.clear()
    }

    fun jump(addr: Int){
        programCounter = addr
    }

    fun jumpTrue(ir: IR){
        val label = ir.dest
        val pos = labelMap[label] ?: programCounter + 1
        val temp = pop()
        val res = (temp.value as String).toBoolean()
        if(res){
            jump(pos - 1)
        }
    }

    fun jumpFalse(ir: IR){
        val label = ir.dest
        val pos = labelMap[label] ?: programCounter + 1
        val temp = pop()
        val res = (temp.value as String).toBoolean()
        if(!res){
            jump(pos - 1)
        }
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
        val a = (temp2.value as String).toInt()
        val b = (temp1.value as String).toInt()

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

        a = (temp2.value as String).toFloat()

        b = (temp1.value as String).toFloat()

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

        a = (temp1.value as String).toFloat()

        b = (temp2.value as String).toFloat()
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
        val a : Boolean = (temp2.value as String).toBoolean()
        val b : Boolean = (temp1.value as String).toBoolean()

        return temp1.type == temp2.type && (a && b)
    }

    fun orExp(temp1:MemItem, temp2: MemItem) : Boolean {
        val a : Boolean = (temp2.value as String).toBoolean()
        val b : Boolean = (temp1.value as String).toBoolean()

        return temp1.type == temp2.type && (a || b)
    }

    fun notExp(temp:MemItem) : Boolean {
        val a : Boolean = (temp.value as String).toBoolean()
        return !a
    }

    fun callFunction(ir: IR){
        val funcName = ir.dest
        if(lookupFunc(funcName)){
            excuteBuiltinFunc(funcName)
        }
        else{
            // return时，pc在外面自己会++的，所以保存本语句的pc即可
            val retAddr = programCounter
            retReg = retAddr
            val pos = labelMap[funcName] ?: programCounter + 1
            jump(pos - 1)
            stack.push(currentFrame)
            currentFrame = StackFrame()
        }
    }

    fun param(ir: IR){
        val lastFrame = stack.peek()
        val paramVal = lastFrame.pop()
        val paramName = ir.dest
        val paramType = ir.src
        store(paramName, paramType, paramVal.value)
    }

    fun ret(ir: IR){
        val retType = ir.dest
        var retVal: MemItem? = null
        if(retType != "void"){
            retVal = pop()
        }

        // clear stack
        currentFrame = stack.pop()
        // push retVal
        retVal?.let { push(retVal) }
        // jump to return address
        if(currentFrame.frameName == "__Main__"){
            jump(labelMap["END"] ?: irArray.size - 1)
        }else{
            jump(retReg)
        }
    }

    fun field(ir: IR){
        val initVal = pop()
        val fieldName = ir.dest
        val fieldType = ir.src

        val thisRef = load("this") // heap[this] = heap[AnnoymousObj]
        var thisRefVal = thisRef?.value as ClassObject
        initVal.name = fieldName
        thisRefVal.self[fieldName] = initVal //MemItem类型
    }

    fun pushA(ir: IR){
        val identifier = ir.dest
        val item = load(identifier)
        if(item!= null){
            push(item)
        }
        else{
            println("加载 $identifier 失败...")
        }
    }

    fun mathOperation(func: (MemItem, MemItem)-> MemItem){
        val temp1 = pop()
        val temp2 = pop()
        val res = func(temp1, temp2)
        push(res)
    }

    fun logicOperation(func: (MemItem, MemItem)-> Boolean){
        val temp1 = pop()
        val temp2 = pop()
        val res = func(temp1, temp2)
        push(MemItem(value = res.toString(), type = "boolean"))
    }


    fun runDirective(ir: IR){
        when(ir.operator){
            //data instructions
            "ICONST" ->{
                val item = MemItem(value = ir.dest,type = "int")
                push(item)
            }
            "FCONST" ->{
                val item = MemItem(value =ir.dest, type="float")
                push(item)
            }
            "BCONST" ->{
                val item = MemItem(value = ir.dest, type = "boolean")
                push(item)
            }
            "STRING" ->{
                val item = MemItem(value = ir.dest, type = "string")
                push(item)
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
                var type: String = ir.src
                val item = pop()
                if(type.equals("")){
                    type = item.type
                }
                // 这种表达式可以的
                // type?.let { store(identifier, it, item.value) }
                store(identifier, type, item.value)
            }
            "LOAD" ->{
                // LOAD == PUSHA
                pushA(ir)
            }
            "PUSHA"->{
                // LOAD == PUSHA
                pushA(ir)
            }
            "PUSH" ->{
                val item = MemItem(value = ir.dest,type = "int")
                push(item)
            }
            "PUSHF" ->{
                val item = MemItem(value =ir.dest, type="float")
                push(item)
            }
            "PUSHB" ->{
                val item = MemItem(value = ir.dest, type = "boolean")
                push(item)
            }
            "PUSHS" ->{
                val item = MemItem(value = ir.dest, type = "string")
                push(item)
            }
            "POPA" ->{
                // POPA Ref ==STORE Ref
                val identifier = ir.dest
                var type: String = ir.src
                var item = pop()
                if(type.equals("")){
                    type = item.type
                }
                // 这种表达式可以的
                // type?.let { store(identifier, it, item.value) }
                store(identifier, type, item.value)
            }
            //
            "MOV" ->{

            }
            "ADD" ->{
                // ADD不能带参数，只有带类型的ADD才能带参数
                // 双冒号使用函数名做参数
                mathOperation(::add)
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
                mathOperation(::sub)
            }
            "MUL" ->{
                mathOperation(::mul)
            }
            "DIV" ->{
                mathOperation(::div)
            }
            "EQ" ->{
                logicOperation(::eq)
            }
            "UNEQ" ->{
                logicOperation(::uneq)
            }
            "LT" ->{
                logicOperation(::lt)
            }
            "GT" ->{
                logicOperation(::gt)
            }
            "LE" ->{
                logicOperation(::le)
            }
            "GE" ->{
                logicOperation(::ge)
            }
            "AND" ->{
                logicOperation(::andExp)
            }
            "OR" ->{
                logicOperation(::orExp)
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
                val pos = labelMap[label] ?: programCounter + 1
                jump(pos - 1)
            }
            "JMPF" ->{
                jumpFalse(ir)
            }
            "JMPT" ->{
                jumpTrue(ir)
            }
            "CALL" ->{
                callFunction(ir)
            }
            "PARAM" ->{
                param(ir)
            }
            "RET" -> {
                // get return value, if dest is void, no return, and don't pop
                ret(ir)
            }
            "FIELD"->{
                field(ir)
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