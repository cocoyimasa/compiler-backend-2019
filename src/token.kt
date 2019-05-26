package main

class Token{

    lateinit var str : String
    var type: String = "tag"

    constructor(str:String){
        this.str = str
    }

    constructor(str:String, type:String = "tag"){
        this.str = str
        this.type = type
    }

    override fun toString(): String {
        return str
    }
}