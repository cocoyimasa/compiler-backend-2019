package main

import java.io.RandomAccessFile
import java.io.File

enum class S{
    // 1.heap instructions
    LOAD, STORE, NEW,
    // 2.stack instructions
    PUSH, POP, PUSHF, POPF, PUSHS, POPS, PUSHB, POPB,
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
    fun visit(node: Node){
        for(elem in node.elems){
            elem.accept(this)
        }
    }
    fun visit(exp: Expression): IR {
        var ir = IR()

        when(exp.value.type){
            "boolean" -> ir = IR("BCONST", exp.value.str)
            "int" -> ir = IR("ICONST", exp.value.str)
            "float" -> ir = IR("FCONST", exp.value.str)
            "string" -> ir = IR("STRING", exp.value.str)
            "identifier" -> ir = IR("LOAD", exp.value.str)
            else -> ir = IR("LOADA", exp.value.str)
        }

        irCodeList.add(ir)

        return ir
    }
    fun visit(st: Statement): IR{
        var ir = IR()

        for(elem in st.elems){
            elem.accept(this)
        }
        return ir
    }
    fun visit(assignStatement: AssignStatement){
        val varName = assignStatement.varName
        val varType = assignStatement.varType
        val exp = assignStatement.exp

        exp.accept(this)

        if(varType == null){
            irCodeList.add(IR("STORE", varName.str))
        }
        else{
            irCodeList.add(IR("STORE", varName.str, varType?.str))
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
        var ir = IR()
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
            ">" -> ir = IR("GT")
            ">=" -> ir = IR("GE")
            "<" -> ir = IR("LT")
            "<=" -> ir = IR("LE")
            "==" -> ir = IR("EQ")
            "!=" -> ir = IR("NEQ")
            "||" -> ir = IR("OR")
            "&&" -> ir = IR("AND")
        }
        irCodeList.add(ir)
        return ir
    }
    fun visit(lambdaExpression: LambdaExpression){
        var ir = IR()
        irCodeList.add(ir)
    }
    fun visit(declaration: Declaration){
        var ir = IR()
        irCodeList.add(ir)
    }
    fun visit(functionStatement: FunctionStatement){
        var ir = IR()
        irCodeList.add(ir)
    }

    fun visit(functionCall: FunctionCall){
        val funcName = functionCall.funcName.str
        val params = functionCall.params

        for(p in params){
            p.accept(this)
        }
        val ir = IR("CALL", funcName)
        irCodeList.add(ir)
    }

    fun visit(block: Block){
        val sts = block.statements
        sts.forEach {
            st -> st.accept(this@CodeGenerator)
        }
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
        if (!file.exists())
            file.createNewFile()
        val randomAccessFile : RandomAccessFile = RandomAccessFile(file, "rw")
        randomAccessFile.seek(0)
        for(ir in irCodeList){
            randomAccessFile.writeBytes("$ir\n")
        }
        randomAccessFile.close()
    }
}