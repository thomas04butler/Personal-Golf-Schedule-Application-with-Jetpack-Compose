package com.example.coursework.models

data class GolfResult (
    val id: Long,
    val datePlayed: String,
    val courseName: String,
    val numberOfHoles: Int,
    val sumOfPar: Int,
    val sumOfScore: Int,
    val parValues: String,
    val scoreValues: String
)