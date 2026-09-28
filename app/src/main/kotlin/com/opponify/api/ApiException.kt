package com.opponify.api
class ApiException(val status:Int,val code:String,override val message:String):RuntimeException(message)
