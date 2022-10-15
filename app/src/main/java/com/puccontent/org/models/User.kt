package com.puccontent.org.models

data class User(
    var name:String = "",
    var email:String = "",
    var userToken:String = "",
    var lastLoginTime:Long = 0
)
