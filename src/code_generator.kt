package main

import java.io.RandomAccessFile
import java.io.File

enum class S{
    // 1.heap instructions
    LOAD, STORE, NEW,
    // 2.stack instructions, if no registers, pop in no sense
    PUSH, POP, PUSHF, POPF, PUSHS, POPS, PUSHB, POPB,PUSHA, POPA,
    // 3.constant instructions
    ICONST, FCONST, STRING, BCONST,
    // 4.label instructions
    LABEL,
    // 5.math instructions
    ADD, SUB, MUL, DIV,
    // 6.move
    MOV,
    // 7.jmp
    JMP, JMPF, JMPT, JE,
    // 8.logic
    NOT, NEG, EQ, UNEQ, LT, GT, LE, GE, AND, OR,
    // 9.function instructions
    FUNC, PARAM, RET, CALL,
    // 10.
    FIELD, METHOD,
}

class IR{
    var operator:String = ""
    var dest:String = "" // destination operand
    var src:String = ""// source operand
    var count:Int = 0
        get() {
            if (dest.equals("") && src.equals("")) {
                field = 0
            } else if (src.equals("")) {
                field = 1
            } else {
                field = 2
            }
            return field
        }
    constructor()
    // NOP
    // JMP Label
    // MOV A,B
    constructor(operator:String, dest:String = "", src:String = ""){
        this.operator = operator
        this.dest = dest
        this.src = src
    }

    fun isEmpty(): Boolean {
        return operator == ""
    }

    fun isSimpleValue(): Boolean{
        if(operator == "ICONST"
            || operator == "FCONST"
            || operator == "STRING"
            || operator == "LOAD"
        ){
            return true
        }
        return false
    }

    override fun toString(): String {
        when(count){
            0-> return operator
            1-> return "$operator $dest"
            2-> return "$operator $dest $src"
        }
        return "$operator $dest $src"
    }
}

@Generator("CodeGenerator")
class CodeGenerator{
    var id: Int = 0
    var irCodeList : ArrayList<IR> = ArrayList()
    var userDefinedClasses = mutableMapOf<String, UserDefinedClassSt>()

    fun label():String{
        return (id++).toString()
    }

    fun defineLabel(labelPrefix: String = "Label_X"): String {
        return "${labelPrefix}_${label()}"
    }

    fun visit(node: Node){
        for(elem in node.elems){
            elem.accept(this)
        }
        irCodeList.add(IR("LABEL", "END"))
    }
    fun visit(exp: Expression){
        var ir = IR()
        when(exp.value.type){
            "void" -> return // 不生成语句
            "boolean" -> ir = IR("PUSHB", exp.value.str)
            "int" -> ir = IR("PUSH", exp.value.str)
            "float" -> ir = IR("PUSHF", exp.value.str)
            "string" -> ir = IR("PUSHS", exp.value.str)
            "identifier" -> ir = IR("PUSHA", exp.value.str)
            else -> ir = IR("PUSHA", exp.value.str)
        }
        irCodeList.add(ir)
    }
    fun visit(st: Statement){
        for(elem in st.elems){
            elem.accept(this)
        }
    }
    fun visit(assignStatement: AssignStatement){
        // for example:
        // a : Int = func(10,100)
        // generate code:
        // PUSHA a
        // push 100
        // push 10
        // CALL func
        // func......
        // RET
        // PUSHA a(隐含操作：POP RetVal)

        // a : Object = new Class(10,10)
        // NEW CLASS
        // PUSH 10
        // PUSH 10
        // CALL CLASS::CLASS()
        //....
        // RET this
        // STORE a Int(隐含操作：POP RetVal)
        val varName = assignStatement.varName
        val varType = assignStatement.varType
        val exp = assignStatement.exp
        exp.accept(this)


        if(varType == null){
            irCodeList.add(IR("STORE", varName.str))
        }
        else{
            irCodeList.add(IR("STORE", varName.str, varType.str))
        }
    }
    fun visit(ifSt: IfStatement){
        val cond = ifSt.condition
        val block = ifSt.ifBlock

        val labelFalse = "Label_If_False${label()}"

        cond.accept(this)
        irCodeList.add(IR("JMPF", labelFalse))
        block.accept(this)
        irCodeList.add(IR("LABEL", labelFalse))
    }
    fun visit(ifElseSt: IfElseStatement){
        // CMP X Y
        // JMPF _L0
        // if body...
        // JMP _L1
        // CMP X1 Y1
        // JMPF _L0
        // else if body...
        // JMP _L1
        // LABEL _L0
        // else body...
        // LABEL _L1
        val labelTrue = "Label_If_True${label()}"
        val labelFalse = "Label_If_False${label()}"

        val ifSt = ifElseSt.ifSt
        val elseIfSts = ifElseSt.elseIfSts
        val elseBlock = ifElseSt.elseBlock

        ifSt.condition.accept(this)
        irCodeList.add(IR("JMPF", labelFalse))
        ifSt.ifBlock.accept(this)
        irCodeList.add(IR("JMP", labelTrue))

        if (elseIfSts != null) {
            for(st in elseIfSts){
                st.condition.accept(this)
                irCodeList.add(IR("JMPF", labelFalse))
                st.ifBlock.accept(this)
                irCodeList.add(IR("JMP", labelTrue))
            }
        }

        irCodeList.add(IR("LABEL", labelFalse))
        elseBlock.accept(this)
        irCodeList.add(IR("LABEL", labelTrue))
    }
    fun visit(forSt: ForStatement): IR{
        val ir = IR()
        return ir
    }
    fun visit(unaryExpression: UnaryExpression): IR{
        var ir: IR = IR()
        unaryExpression.exp.accept(this)
        when(unaryExpression.operator.str){
            "!" -> ir = IR("NOT")
            "-" -> ir = IR("NEG")
        }
        return ir
    }
    fun visit(binaryExpression: BinaryExpression): IR{
        var ir: IR = IR()
        binaryExpression.leftExp.accept(this)
        binaryExpression.rightExp.accept(this)
        when(binaryExpression.operator.str){
            "+" -> ir = IR("ADD")
            "-" -> ir = IR("SUB")
            "*" -> ir = IR("MUL")
            "/" -> ir = IR("DIV")
            ">" -> ir = IR("GT")
            ">=" -> ir = IR("GE")
            "<" -> ir = IR("LT")
            "<=" -> ir = IR("LE")
            "==" -> ir = IR("EQ")
            "!=" -> ir = IR("NEQ")
            "||" -> ir = IR("OR")
            "&&" -> ir = IR("AND")
            else -> ir = IR(binaryExpression.operator.str)
        }
        irCodeList.add(ir)
        return ir
    }
    fun visit(lambdaExpression: LambdaExpression){
        val ir = IR()
        irCodeList.add(ir)
    }
    fun visit(declaration: Declaration){
        val ir = IR("")
        irCodeList.add(ir)
    }

    fun visit(retSt: ReturnStatement){
        retSt.returnExp?.accept(this)
        val retType = retSt.returnType

        val ir = IR("RET")
        if(retType == "void"){
            ir.dest = "void"
        }
        irCodeList.add(ir)
    }

    fun visit(breakSt: BreakStatement){
        // 获取循环的end标签
        // getLoopEndLabel
        val label:String = ""
        val ir = IR("JMP", label)
        irCodeList.add(ir)
    }

    fun visit(argument: Argument){
        val argName = argument.name.str
        val argType = argument.type.str
        irCodeList.add(IR("PARAM", argName, argType))
    }

    fun visit(functionStatement: FunctionStatement){
        val funcName = functionStatement.funcName.str
        val args = functionStatement.arguments
        val block = functionStatement.block
        val retSt = functionStatement.retSt
        irCodeList.add(IR("LABEL",funcName))

        for(arg in args){
            arg.accept(this)
        }

        block.accept(this)
        retSt.accept(this)
    }

    fun visitFuncCall(functionCall: IFunctionCall ){
        val funcName = functionCall.funcName.str
        val params = functionCall.params

        //参数反向压栈
        val paramsR = arrayListOf<Expression>()
        paramsR.addAll(params)

        for(param in paramsR){
            param.accept(this)
        }

        val ir = IR("CALL", funcName)
        irCodeList.add(ir)
    }

    fun visit(functionCall: FunctionCall){
        visitFuncCall(functionCall)
    }

    fun visit(functionCall: FunctionCallExp){
        visitFuncCall(functionCall)
    }

    fun visit(block: Block){
        val sts = block.statements
        sts.forEach {
            st -> st.accept(this@CodeGenerator)
        }
    }

    fun visitConstructor(className: String,
                         fields: ArrayList<FieldStatement>,
                         ctor : MethodStatement? = null){
        if(ctor == null){
            irCodeList.add(IR("LABEL", "$className::$className"))
        }
        else{
            val args = ctor.arguments
            val len = args.size
            val sb = StringBuffer()
            for(arg in args){
                sb.append(arg.type.str + "_")
            }
            // className::className_len_type_
            irCodeList.add(IR("LABEL", "$className::${className}_${len}_$sb"))
        }
        irCodeList.add(IR("PARAM", "this", className))

        ctor?.let{
            val args = ctor.arguments

            for(arg in args){
                arg.accept(this)
            }
        }

        for(field in fields){
            field.accept(this)
        }

        ctor?.let{
            val block = ctor.block
            block.accept(this)
        }

        irCodeList.add(IR("PUSHA", "this")) // 加载this到当前栈上
        irCodeList.add(IR("RET"))
    }

    fun visit(userDefinedClassSt: UserDefinedClassSt){
        // LABEL className
        // LABEL className::className
        // PARAM this className (this = Map,此时对象成为一个Dict)
        // PARAM X TYPE
        // PARAM Y TYPE
        // PARAM Z TYPE
        // PUSH VAL1
        // FIELD className::NAME1 TYPE(this[NAME1] = VAL1)
        // PUSH VAL2
        // FIELD className::NAME2 TYPE(this[NAME2] = VAL2)
        // PUSH VAL3
        // FIELD className::NAME2 TYPE(this[NAME3] = VAL3)
        // LOAD X
        // STORE className::NAME1 TYPE
        // LOAD Y
        // STORE className::NAME2 TYPE
        // RET this
        // METHOD className::A
        // PARAM this
        val className = userDefinedClassSt.className.str
        val fields= userDefinedClassSt.fields
        val constructors = userDefinedClassSt.constructors
        val methods = userDefinedClassSt.methods
        irCodeList.add(IR("LABEL", className))
        // 生成 default constructor
        visitConstructor(className, fields)
        // 生成其他 constructor
        for(ctor in constructors){
            visitConstructor(className, fields, ctor)
        }

        for(method in methods){
            method.accept(this)
        }

    }

    fun visit(newObjectExp: NewObjectExp){
        val params = newObjectExp.params
        val className = newObjectExp.className.str
        //参数反向压栈
        val paramsR = arrayListOf<Expression>()
        paramsR.addAll(params)

        for(param in paramsR){
            param.accept(this)
        }
        val len = params.size
        val sb = StringBuffer()
        // TODO: bug: param可能类型是identifier
        for(param in params){
            sb.append(param.value.type + "_")
        }
        val tempObj = defineLabel("AnnoymousObject")
        irCodeList.add(IR("NEW", tempObj, className))
        irCodeList.add(IR("PUSHA", tempObj)) // 加载到当前栈上
        val ir = if(len == 0){
            IR("CALL", "$className::$className")
        }
        else{
            IR("CALL", "$className::${className}_${len}_$sb")
        }

        irCodeList.add(ir)
    }

    fun visit(methodStatement: MethodStatement){
        val className = methodStatement.className.str
        val funcName = methodStatement.funcName.str
        val args = methodStatement.arguments
        val block = methodStatement.block
        val retSt = methodStatement.retSt
        irCodeList.add(IR("LABEL","$className::$funcName"))

        irCodeList.add(IR("PARAM", "this", className))
        for(arg in args){
            arg.accept(this)
        }

        block.accept(this)
        retSt.accept(this)
    }

    fun visitField(obj:String, fieldStatement: FieldStatement){
        val className = fieldStatement.className.str
        val fieldName = fieldStatement.fieldName.str
        val fieldType = fieldStatement.fieldType.str
        val initValue = fieldStatement.initValue

        initValue?.accept(this)

        irCodeList.add(IR("FIELD", "$obj::$fieldName", fieldType))
    }
    fun visit(fieldStatement: FieldStatement){
        val className = fieldStatement.className.str
        val fieldName = fieldStatement.fieldName.str
        val fieldType = fieldStatement.fieldType.str
        val initValue = fieldStatement.initValue

        initValue?.accept(this)

        irCodeList.add(IR("FIELD", fieldName, fieldType))
    }

    override fun toString() : String{
        val sb : StringBuffer = StringBuffer("")

        for(ir in irCodeList){
            sb.append("$ir\n")
        }

        return sb.toString()
    }
    fun toFile(filename: String = "compiled.asm"){
        val file = File(filename)
        //指定文件不存在就创建同名文件
        if (file.exists()){
            file.delete()
            file.createNewFile()
        }
        else{
            file.createNewFile()
        }
        val randomAccessFile : RandomAccessFile = RandomAccessFile(file, "rw")
        randomAccessFile.seek(0)
        for(ir in irCodeList){
            randomAccessFile.writeBytes("$ir\n")
        }
        randomAccessFile.close()
    }
}