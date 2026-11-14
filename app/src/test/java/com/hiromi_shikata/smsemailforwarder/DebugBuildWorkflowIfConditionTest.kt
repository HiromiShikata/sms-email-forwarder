package com.hiromi_shikata.smsemailforwarder

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class DebugBuildWorkflowIfConditionTest {

    private data class Case(val eventName: String, val ref: String, val expected: Boolean)

    private val cases = listOf(
        Case(eventName = "push", ref = "refs/heads/main", expected = true),
        Case(eventName = "push", ref = "refs/heads/feature-x", expected = false),
        Case(eventName = "pull_request", ref = "refs/pull/1/merge", expected = false),
        Case(eventName = "push", ref = "refs/tags/v1", expected = false),
    )

    @Test
    fun `decode release keystore step if condition evaluates to expected boolean per event and ref`() {
        val condition = decodeReleaseKeystoreIfCondition(File("../.github/workflows/debug_build.yml").readLines())
        val actual = cases.map { testCase ->
            val contexts = mapOf(
                "github.event_name" to testCase.eventName,
                "github.ref" to testCase.ref,
            )
            val result = GitHubExpressionEvaluator(contexts, condition).evaluate()
            Triple(testCase.eventName, testCase.ref, isTruthy(result))
        }
        assertEquals(cases.map { Triple(it.eventName, it.ref, it.expected) }, actual)
    }
}

private fun decodeReleaseKeystoreIfCondition(lines: List<String>): String {
    val stepStart = lines.indexOfFirst { it.trim() == "- name: Decode release keystore" }
    require(stepStart >= 0) { "Decode release keystore step not found" }
    val stepLines = lines.drop(stepStart + 1).takeWhile { !it.startsWith("      - ") }
    val ifLine = stepLines.firstOrNull { it.startsWith("        if:") }
    requireNotNull(ifLine) { "Decode release keystore step has no step-level if" }
    return ifLine.removePrefix("        if:").trim()
}

private fun tokenize(source: String): List<String> {
    val matches = Regex("'(?:[^']|'')*'|==|&&|\\|\\||\\(|\\)|[A-Za-z_][A-Za-z0-9_.]*|\\s+")
        .findAll(source)
        .toList()
    require(matches.joinToString("") { it.value } == source) { "unsupported syntax in expression: $source" }
    return matches.map { it.value }.filter { it.isNotBlank() }
}

private fun equalsGitHub(left: Any?, right: Any?): Boolean =
    if (left is String && right is String) left.equals(right, ignoreCase = true) else left == right

private fun isTruthy(value: Any?): Boolean = when (value) {
    null -> false
    is Boolean -> value
    is String -> value.isNotEmpty()
    else -> error("unsupported value in expression: $value")
}

private class GitHubExpressionEvaluator(private val contexts: Map<String, Any?>, source: String) {

    private val tokens = tokenize(source)
    private var position = 0

    fun evaluate(): Any? {
        val value = parseOr()
        check(position == tokens.size) { "unexpected token ${tokens[position]} in expression" }
        return value
    }

    private fun parseOr(): Any? {
        var value = parseAnd()
        while (peek() == "||") {
            position++
            val right = parseAnd()
            value = if (isTruthy(value)) value else right
        }
        return value
    }

    private fun parseAnd(): Any? {
        var value = parseEquality()
        while (peek() == "&&") {
            position++
            val right = parseEquality()
            value = if (isTruthy(value)) right else value
        }
        return value
    }

    private fun parseEquality(): Any? {
        var value = parseOperand()
        while (peek() == "==") {
            position++
            val right = parseOperand()
            value = equalsGitHub(value, right)
        }
        return value
    }

    private fun parseOperand(): Any? {
        val token = tokens.getOrNull(position) ?: error("unexpected end of expression")
        position++
        return when {
            token == "(" -> parseOr().also { expect(")") }
            token.startsWith("'") -> token.substring(1, token.length - 1).replace("''", "'")
            token == "true" -> true
            token == "false" -> false
            token == "null" -> null
            else -> {
                require(contexts.containsKey(token)) { "unknown context $token" }
                contexts.getValue(token)
            }
        }
    }

    private fun expect(token: String) {
        check(peek() == token) { "expected $token at token $position" }
        position++
    }

    private fun peek(): String? = tokens.getOrNull(position)
}
