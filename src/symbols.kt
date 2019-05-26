package main

// lexical scope
// Symbol Table
@SymbolTable
class Scope{
    lateinit var name : String
    // (name,type)
    var current = mutableMapOf<String, String>()
    var inner = arrayListOf<Scope>()
    var parent : Scope? = null

    constructor()

    constructor(name: String){
        this.name = name
    }
    constructor(parent: Scope? = null, inner: ArrayList<Scope> = arrayListOf(),name : String = ""){
        this.parent = parent
        this.inner.addAll(inner)
        this.name = name
    }

    fun lookup(name:String): String? {
        if(current.containsKey(name)){
            return current[name]
        }
        else if(parent != null){
            return parent?.lookup(name)
        }
        else{
            println("$name is not defined")
            return null
        }
    }

}