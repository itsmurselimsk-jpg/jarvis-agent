package com.example

import com.example.jarvis.provider.JarvisAutonomousBrain
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisAutonomousBrainUnitTest {

    @Test
    fun testMathEvaluation() {
        val calc1 = JarvisAutonomousBrain.generateAutonomousResponse("calculate 25 * 4")
        assertTrue(calc1.contains("100"))

        val calc2 = JarvisAutonomousBrain.generateAutonomousResponse("500 ka 18%")
        assertTrue(calc2.contains("90"))

        val calc3 = JarvisAutonomousBrain.generateAutonomousResponse("square root of 144")
        assertTrue(calc3.contains("12"))
    }

    @Test
    fun testCodeGeneration() {
        val primeResponse = JarvisAutonomousBrain.generateAutonomousResponse("write python code for prime number")
        assertTrue(primeResponse.contains("def is_prime"))
        assertTrue(primeResponse.contains("```python"))

        val fibResponse = JarvisAutonomousBrain.generateAutonomousResponse("fibonacci series in python")
        assertTrue(fibResponse.contains("def fibonacci"))

        val htmlResponse = JarvisAutonomousBrain.generateAutonomousResponse("html website bana do")
        assertTrue(htmlResponse.contains("<!DOCTYPE html>"))
    }

    @Test
    fun testWritingTemplates() {
        val leaveResponse = JarvisAutonomousBrain.generateAutonomousResponse("sick leave application likh do")
        println("DEBUG LEAVE RESPONSE: $leaveResponse")
        assertTrue(leaveResponse.contains("Application for Sick Leave"))
        assertTrue(leaveResponse.contains("Yours faithfully"))

        val timeTableResponse = JarvisAutonomousBrain.generateAutonomousResponse("ek study time table bana do")
        assertTrue(timeTableResponse.contains("Daily Study Routine"))
        assertTrue(timeTableResponse.contains("Pomodoro"))
    }

    @Test
    fun testKnowledgeAndScience() {
        val blackHoleResponse = JarvisAutonomousBrain.generateAutonomousResponse("black hole kya hai samjhao")
        assertTrue(blackHoleResponse.contains("Event Horizon"))
        assertTrue(blackHoleResponse.contains("Singularity"))

        val aiResponse = JarvisAutonomousBrain.generateAutonomousResponse("ai kya hai")
        assertTrue(aiResponse.contains("Artificial Intelligence"))
        assertTrue(aiResponse.contains("Machine Learning"))
    }

    @Test
    fun testCreativeAndJokes() {
        val storyResponse = JarvisAutonomousBrain.generateAutonomousResponse("ek kahani sunao")
        assertTrue(storyResponse.contains("Clockmaker"))

        val jokeResponse = JarvisAutonomousBrain.generateAutonomousResponse("koi funny joke sunao")
        assertTrue(jokeResponse.contains("😂") || jokeResponse.contains("🤣"))

        val shayariResponse = JarvisAutonomousBrain.generateAutonomousResponse("ek shayari sunao")
        assertTrue(shayariResponse.contains("Manzil") || shayariResponse.contains("hauslon"))
    }

    @Test
    fun testConversationalComprehension() {
        val reply = JarvisAutonomousBrain.generateAutonomousResponse("aaj ka din kaisa rahega bhai")
        assertNotNull(reply)
        assertTrue(reply.length > 20)
    }
}
