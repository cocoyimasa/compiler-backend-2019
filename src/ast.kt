package main

open class Node{
    lateinit var value : Token
    var elems = ArrayList<Node>()
    constructor()

    constructor(value:Token){
        this.value = value
    }

    open fun add(elem: Node){
        elems.add(elem)
    }

    open fun accept(codeGenerator: CodeGenerator){
        codeGenerator.visit(this)
    }

    override fun toString(): String {
        return "Node:$value"
    }
}

open class Statement(value:Token) : Node(value = value) {
    override fun toString(): String {
        return "Statement:$value"
    }
    override fun accept(codeGenerator: CodeGenerator){
        codeGenerator.visit(this)
    }
}
open class Expression(value:Token) : Node(value = value) {
    override fun toString(): String {
        return "Expression:$value"
    }
    override fun accept(codeGenerator: CodeGenerator){
        codeGenerator.visit(this)
    }
}

class UnaryExpression : Expression{
    lateinit var operator: Token
    lateinit var exp : Expression

    constructor(operator: Token, exp : Expression) : super(operator){
        this.operator = operator
        this.exp = exp
    }

    override fun toString(): String {
        return "UnaryExpression:$operator:$exp"
    }
    override fun accept(codeGenerator: CodeGenerator){
        codeGenerator.visit(this)
    }
}

class BinaryExpression(value:Token) : Expression(value=value){
    lateinit var leftExp: Expression
    lateinit var rightExp: Expression
    lateinit var operator: Token
    constructor(leftExp:Expression, operator:Token, rightExp:Expression) : this(operator) {
        this.leftExp = leftExp
        this.rightExp = rightExp
        this.operator = operator
    }
    override fun accept(codeGenerator: CodeGenerator){
        codeGenerator.visit(this)
    }
    override fun toString(): String {
        return "BinaryExpression:$leftExp:$operator:$rightExp"
    }
}

class LambdaExpression : Expression{
    lateinit var returnType : Token
    var arguments : ArrayList<Argument> = ArrayList()
    lateinit var block: Block
    // 无返回，则需生成空返回语句
    lateinit var retSt: Statement

    constructor(params: ArrayList<Argument>,
                returnType : Token,
                block: Block,
                retSt: Statement)
            : super(Token("LambdaExpression")) {
        this.arguments.addAll(params)
        this.returnType = returnType
        this.block = block
        this.retSt = retSt
    }
    override fun accept(codeGenerator: CodeGenerator){
        codeGenerator.visit(this)
    }

    override fun toString(): String {
        val sb : StringBuffer = StringBuffer()
        for(arg in arguments){
            sb.append("${arg.toString()},")
        }
        return "LambdaExpression:($sb)->$returnType:$block"
    }
}

class Block(value: Token) : Statement(value){
    var statements: ArrayList<Statement> = ArrayList()
//    var returns : ArrayList<ReturnStatement> = ArrayList()
    constructor(statements: ArrayList<Statement>) : this(Token("block")){
        this.statements.addAll(statements)
    }
    override fun accept(codeGenerator: CodeGenerator){
        codeGenerator.visit(this)
    }

    override fun toString(): String {
        val sb : StringBuffer = StringBuffer()
        for(st in statements){
            sb.append(st.toString())
        }

        return "Block:($sb)"
    }
}
// if
class IfStatement(value: Token) : Statement(value){
    lateinit var condition: Expression
    lateinit var ifBlock: Block

    constructor(condition: Expression, block: Block) : this(Token("if")){
        this.condition = condition
        this.ifBlock = block
    }
    override fun accept(codeGenerator: CodeGenerator){
        codeGenerator.visit(this)
    }
    override fun toString(): String {
        return "If:($condition)$ifBlock"
    }
}

//if-[else if]-else
class IfElseStatement(value: Token) : Statement(value){
    lateinit var ifSt : IfStatement
    var elseIfSts: ArrayList<IfStatement>? = null
    lateinit var elseBlock :Block
    constructor(ifSt: IfStatement,
                _elseIfSts: ArrayList<IfStatement>?,
                elseBlock :Block)
            : this(Token("if")){
        this.ifSt = ifSt
        _elseIfSts?.forEach {
            st -> this@IfElseStatement.elseIfSts?.add(st)
        }
        this.elseBlock = elseBlock
    }
    override fun accept(codeGenerator: CodeGenerator){
        codeGenerator.visit(this)
    }
    override fun toString(): String {

        val sb : StringBuffer = StringBuffer("")
        elseIfSts?.forEach { st -> sb.append(st.toString()) }

        return "If-Else:(${ifSt.toString()}):$sb:$elseBlock"
    }
}

class ForStatement(value: Token) : Statement(value){
    lateinit var initSt : Statement
    lateinit var endExp : Expression
    lateinit var loopExp : Expression
    lateinit var block : Block

    constructor(initSt: Statement, endExp : Expression,
                loopExp: Expression, block: Block) :
            this(Token("for")) {
        this.initSt = initSt
        this.endExp = endExp
        this.loopExp = loopExp
        this.block = block
    }
    override fun accept(codeGenerator: CodeGenerator){
        codeGenerator.visit(this)
    }

    override fun toString(): String {
        return "For:($initSt,$endExp,$loopExp):$block"
    }
}
//declaration, init-statement
class Declaration : Statement{
    lateinit var type : Token
    lateinit var name : Token
    var initExp : Expression? = null

    constructor():super(Token("declaration"))

    constructor(type : Token, name: Token, initExp : Expression?):this(){
        this.type = type
        this.name = name
        this.initExp = initExp
    }
    override fun accept(codeGenerator: CodeGenerator){
        codeGenerator.visit(this)
    }

    override fun toString(): String {
        return "Declaration:$type:$name=${initExp?.toString()}"
    }
}

class Argument : Node{
    lateinit var type : Token
    lateinit var name : Token

    constructor():super(Token("argument"))

    constructor(type : Token, name: Token):this(){
        this.type = type
        this.name = name
    }
    override fun accept(codeGenerator: CodeGenerator){
        codeGenerator.visit(this)
    }

    override fun toString(): String {
        return "Argument:$type:$name"
    }
}

class AssignStatement : Statement{
    lateinit var varName : Token
    var varType : Token? = null
    lateinit var exp: Expression
    constructor(varName: Token, varType:Token?, exp: Expression) : super(Token("return")){
        this.varName = varName
        this.varType = varType
        this.exp = exp
    }
    override fun accept(codeGenerator: CodeGenerator){
        codeGenerator.visit(this)
    }

    override fun toString(): String {
        return "Assign:($varName:${varType.toString()}=$exp)"
    }
}

class BreakStatement : Statement{
    lateinit var breakExp: Expression
    constructor(breakExp: Expression) : super(Token("return")){
        this.breakExp = breakExp
    }
    override fun accept(codeGenerator: CodeGenerator){
        codeGenerator.visit(this)
    }

    override fun toString(): String {
        return "Break:($breakExp)"
    }
}

class ReturnStatement : Statement{
    var returnExp: Expression? = null
    lateinit var returnType: String
    constructor(returnExp: Expression? = null, returnType: String = "void") : super(Token("return")){
        this.returnExp = returnExp
        this.returnType = returnType
    }
    override fun accept(codeGenerator: CodeGenerator){
        codeGenerator.visit(this)
    }

    override fun toString(): String {
        return "Return:($returnExp)"
    }
}

open class FunctionStatement : Statement{
    lateinit var funcName : Token
    lateinit var returnType : Token
    var arguments : ArrayList<Argument> = ArrayList()
    lateinit var block: Block
    // 如果函数没有return语句，需要加一条空的返回语句
    // 如果有，直接存上
    lateinit var retSt: Statement

    constructor(funcName: Token, params: ArrayList<Argument>,
                returnType : Token, block: Block,
                retSt: Statement)
            : super(Token("function")) {
        this.funcName = funcName
        this.arguments.addAll(params)
        this.returnType = returnType
        this.block = block
        this.retSt = retSt
    }

    override fun accept(codeGenerator: CodeGenerator){
        codeGenerator.visit(this)
    }

    override fun toString(): String {
        val sb : StringBuffer = StringBuffer()
        for(arg in arguments){
            sb.append("${arg.toString()},")
        }
        return "Function:$funcName:($sb)$returnType:$block"
    }
}

interface IFunctionCall{
    var funcName : Token
    var params : ArrayList<Expression>

    fun noParam() : Boolean;
}
// FunctionCall is a statement, and also it is an Expression
class FunctionCallExp : Expression, IFunctionCall{
    override var funcName : Token
    override var params : ArrayList<Expression> = ArrayList()
    constructor(funcName : Token,
                params : ArrayList<Expression>):
            super(Token("FunctionCall")){
        this.funcName = funcName
        this.params.addAll(params)
    }

    override fun noParam() : Boolean{
        return params.size == 0
    }

    override fun accept(codeGenerator: CodeGenerator){
        codeGenerator.visit(this)
    }

    override fun toString(): String {
        val sb : StringBuffer = StringBuffer()
        for(param in params){
            sb.append("$param,")
        }
        return "FunctionCall:$funcName($sb)"
    }
}

class FunctionCall : Statement, IFunctionCall{
    override var funcName : Token
    override var params : ArrayList<Expression> = ArrayList()
    constructor(funcName : Token,
                params : ArrayList<Expression>):
            super(Token("FunctionCall")){
        this.funcName = funcName
        this.params.addAll(params)
    }

    override fun noParam() : Boolean{
        return params.size == 0
    }

    override fun accept(codeGenerator: CodeGenerator){
        codeGenerator.visit(this)
    }

    override fun toString(): String {
        val sb : StringBuffer = StringBuffer()
        for(param in params){
            sb.append("${param.toString()},")
        }
        return "FunctionCall:$funcName($sb)"
    }
}

class FieldStatement : Statement{
    lateinit var className : Token
    lateinit var fieldName : Token
    lateinit var fieldType : Token
    var initValue : Expression? = null
    constructor(className : Token, fieldName : Token,
                fieldType : Token, initValue : Expression?)
            : super(Token("field")){
        this.fieldName = fieldName
        this.fieldType = fieldType
        this.initValue = initValue
    }

    override fun accept(codeGenerator: CodeGenerator){
        codeGenerator.visit(this)
    }

    override fun toString(): String {
        return "FieldStatement:$className." +
                "$fieldName:$fieldType=${initValue.toString()}"
    }
}

class MethodStatement : FunctionStatement{
    lateinit var className : Token
    lateinit var accessLevel : Token
    var modifier : ArrayList<Token> = arrayListOf()
    constructor(className : Token,
                accessLevel : Token,
                modifier: ArrayList<Token>,
                funcName: Token,
                params: ArrayList<Argument>,
                returnType : Token,
                block: Block,
                retSt: Statement)
            : super(funcName, params, returnType, block, retSt) {
        this.className = className
        this.accessLevel = accessLevel
        modifier.addAll(modifier)
    }

    override fun accept(codeGenerator: CodeGenerator){
        codeGenerator.visit(this)
    }

    override fun toString(): String {
        val sb = StringBuffer()
        modifier.forEach {
            sb.append("${it.str},")
        }
        return "MethodStatement:$className:$accessLevel:$sb:${super.toString()}"
    }
}

class UserDefinedClassSt : Statement{
    lateinit var pkgName : String
    lateinit var className : Token
    lateinit var superClass : Token
    var superInterfaces : ArrayList<Token> = arrayListOf()
    var fields : ArrayList<FieldStatement> = arrayListOf()
    var methods : ArrayList<MethodStatement> = arrayListOf()

    constructor():super(Token("class"))

    constructor(pkgName : String,
                className : Token,
                superClass : Token,
                superInterfaces : ArrayList<Token>,
                fields : ArrayList<FieldStatement>,
                methods : ArrayList<MethodStatement>
                ):this(){
        this.pkgName = pkgName
        this.className = className
        this.superClass = superClass
        this.superInterfaces.addAll(superInterfaces)
        this.fields.addAll(fields)
        this.methods.addAll(methods)
    }

    fun fullName(): String{
        return "$pkgName.$className"
    }

    override fun accept(codeGenerator: CodeGenerator){
        codeGenerator.visit(this)
    }

    override fun toString(): String {
        return "UserDefinedClassSt:$className:$superClass,$superInterfaces"
    }
}

class NewObjectExp : Expression{
    lateinit var className : Token
    var params : ArrayList<Expression> = arrayListOf()
    constructor(className : Token, params: ArrayList<Expression>):super(Token("object")){
        this.className = className
        this.params.addAll(params)
    }

    override fun accept(codeGenerator: CodeGenerator){
        codeGenerator.visit(this)
    }

    override fun toString(): String {
        val sb : StringBuffer = StringBuffer()
        for(param in params){
            sb.append("$param,")
        }
        return "NewObjectExp:($className:$sb)"
    }
}
