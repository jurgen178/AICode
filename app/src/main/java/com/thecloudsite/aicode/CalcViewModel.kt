/*
 * Copyright (C)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thecloudsite.aicode

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.text.NumberFormat
import java.util.*
import kotlin.math.*
import kotlin.text.RegexOption.IGNORE_CASE


enum class VariableArguments {
    PICK,
    ROLL,
    SUM,
    VAR,  // Varianz
    VALIDATE,
}

enum class ZeroArgument {
    DEPTH,
    CLEAR,
    PI,
    E,
    RAND,   // Random number between 0..1
}

enum class UnaryArgument {
    DROP,
    DUP,
    SQRT,
    SQ,
    INV,
    ABS,
    SIGN,
    SIN,
    SINH,
    COS,
    COSH,
    TAN,
    TANH,
    ARCSIN,
    ARCSINH,
    ARCCOS,
    ARCCOSH,
    ARCTAN,
    ARCTANH,
    VECTOR,   // Create vector element.
    INT,    // Integer part
    ROUND,  // Round to nearest int
    ROUND2, // Round to two digits
    ROUND4, // Round to four digits
    FRAC,
    FACTORIAL,
    SIZE,
    TOSTR,  // toStr
    LN,
    EX,
    LOG,
    ZX,
    NOT,
    COMPLEMENT,
    ERF,
}

enum class BinaryArgument {
    ADD,
    SUB,
    MULT,
    DIV,
    ARCTAN2,
    HYPOT,
    POW,
    MIN,
    MAX,
    SWAP,
    OVER,
    MATRIX, // Create matrix element.
    GETV, // Get vector element.
    MOD,  // modulo
    PER,  // Percent
    PERC, // Percent change
    AND,
    OR,
    XOR,
    SOLVE,
    BINOM,
}

enum class TernaryArgument {
    ROT,
    GETM, // Get matrix element.
    SETV, // Set vector element.
    ZinsMonat,
}

enum class QuadArgument {
    SETM, // Set matrix element.
    IFEQ, // if equal
    IFGE, // if greater or equal than
    IFGT, // if greater than
    IFLE, // if less or equal then
    IFLT, // if less then
}

const val VECTORMAX = 100
const val MATRIXMAX = 40

const val MEM_STORE_PREFIX = "mem_"


// List of chars that cannot be used as definitions.
val wordListRegex = "[\"':;(){}\\[\\]]".toRegex()

// (?s) dotall
val wordDefinitionRegex = Regex("(?s):\\s(.+?)\\s(?:.*?\\s)?;")

data class LoopValues(
    var start: CalcLine,
    var end: CalcLine,
    var inc: CalcLine,
)

data class CodeType
    (
    val code: String,
    val name: String,
)

data class CodeTypeJson
    (
    val key: String,
    val code: String,
    val name: String,
)

data class CalcLineJson
    (
    var desc: String = "",
    var value: Double? = 0.0,
    var lambda: Int = -1,
    var definition: String = "",
    var vector: List<Double>?,
    var matrix: List<List<Double>>?,
)

class CalcViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application
    private val calcRepository: CalcRepository = CalcRepository(application)
    private var aic: Int = 0
    private val varMap: MutableMap<String, CalcLine> = mutableMapOf()
    var symbol: String = ""
    var shiftLevel: Int = 0
    var shiftLevelBinary: Int = 0
    var calcData: LiveData<CalcData> = calcRepository.calcLiveData
    var radian = 1.0
    var separatorChar = ','
    var numberFormat: NumberFormat = NumberFormat.getNumberInstance()
    var sciFormat = false
    val codeMap: MutableMap<String, CodeType> = mutableMapOf()

    init {
        calcRepository.updateData(calcRepository.getData())
    }

    private fun getRegexOneGroup(text: String, regex: Regex): String? {
        val match = regex.matchEntire(text)
        if (match != null
            && match.groups.size == 2
            && match.groups[1] != null
        ) {
            // first group (groups[0]) is entire src
            // first capture is in groups[1]
            return match.groups[1]!!.value
        }

        return null
    }

    private fun getRegexTwoGroups(text: String, regex: Regex): Pair<String, String>? {
        val match = regex.matchEntire(text)
        if (match != null
            && match.groups.size == 3
            && match.groups[1] != null
            && match.groups[2] != null
        ) {
            // first group (groups[0]) is entire src
            // first capture is in groups[1]
            // second capture is in groups[2]
            return Pair(match.groups[1]!!.value, match.groups[2]!!.value)
        }

        return null
    }

    private fun getRegexThreeGroups(text: String, regex: Regex): Triple<String, String, String>? {
        val match = regex.matchEntire(text)
        if (match != null
            && match.groups.size == 4
            && match.groups[1] != null
            && match.groups[2] != null
            && match.groups[3] != null
        ) {
            // first group (groups[0]) is entire src
            // first capture is in groups[1]
            // second capture is in groups[2]
            // third capture is in groups[2]
            return Triple(match.groups[1]!!.value, match.groups[2]!!.value, match.groups[3]!!.value)
        }

        return null
    }

    fun function(code: String, codeMapKey: String = "") {
        val calcData = submitEditline(calcData.value!!)

        endEdit(calcData)

        // remove comments
        var codePreprocessed = code
            .replace("(?s)/[*].*?[*]/".toRegex(), " ")

            //.replace("(?s): [a-z]+ .*? ;".toRegex(IGNORE_CASE), " ")

            //.replace("//.*?(\n|$)".toRegex(), " ")

            // multiline (?m) accept the anchors ^ and $ to match at the start and end of each line
            // (otherwise they only match at the start/end of the entire string)
            .replace("(?m)//.*?$".toRegex(), " ")

        // resolve imports
        val regex = Regex("(?i)(?:\\s|^)import[.]\"(.+?)\"")
        val matches = regex.findAll(codePreprocessed)

        // process each import statement
        matches.forEach { matchResult ->
            val import = matchResult.groupValues[1]

            // do not import the own code
            if (!(codeMapKey.isNotEmpty()
                        && codeMap.containsKey(codeMapKey)
                        && import.equals(codeMap[codeMapKey]!!.name, true))
            ) {
                // Search all code entries for a matching name.
                val codeKey = codeMap.filterValues { codeValue ->
                    codeValue.name.equals(import, true)
                }

                when {
                    codeKey.size == 1 -> {

                        // Get all definitions in the import.
                        val importedCode = codeKey.values.first().code
                            .replace("(?s)/[*].*?[*]/".toRegex(), " ")
                            .replace("(?m)//.*?$".toRegex(), " ")
                        val matchesDefinition = wordDefinitionRegex.findAll(importedCode)
                        matchesDefinition.forEach { matchResultDefinition ->
                            val definition = matchResultDefinition.groupValues[0]
                            codePreprocessed += " $definition "
                        }

                        // Get special instructions from the import.
                        if (importedCode.contains(":loop", true)) {
                            codePreprocessed += " :loop "
                        }
                    }
                    codeKey.size > 1 -> {
                        calcData.errorMsg =
                            context.getString(R.string.calc_multiple_imports, import)
                        calcRepository.updateData(calcData)

                        // multiple imports exist, end loop
                        return
                    }
                    else -> {
                        calcData.errorMsg = context.getString(R.string.calc_unknown_import, import)
                        calcRepository.updateData(calcData)

                        // import missing, end loop
                        return
                    }
                }
            }
        }

        // Split by spaces not followed by even amount of quotes so only spaces outside of quotes are replaced.
        val words = codePreprocessed
            .split("\\s+(?=([^\"']*[\"'][^\"']*[\"'])*[^\"']*$)".toRegex())

        var success = true
        var validArgs = true
        var checkLoop = true

        // Label has optional 'do' (do.label1 instead of .label1) at the beginning
        // for better readability when used for while loop.
        // ?: = non capturing group
        // (?s) = dotall = . + \n
        val labelRegex = "^(?:do)?[.](.+?)$".toRegex(IGNORE_CASE)
        val whileIfRegex = "^(while|if)[.](\\w+)[.](.+?)$".toRegex(IGNORE_CASE)
        val loopRegex = "^loop[.](.+?)$".toRegex(IGNORE_CASE)
        val endloopRegex = "^end[.](.+?)$".toRegex(IGNORE_CASE)
        val gotoRegex = "^goto[.](.+?)$".toRegex(IGNORE_CASE)
        val stoRegex = "^sto[.](.+)$".toRegex(IGNORE_CASE)
        val rclRegex = "^rcl[.](.+)$".toRegex(IGNORE_CASE)
        val memstoRegex = "^memsto[.](.+)$".toRegex(IGNORE_CASE)
        val memrclRegex = "^memrcl[.](.+)$".toRegex(IGNORE_CASE)
        val memdelRegex = "^memdel[.](.+)$".toRegex(IGNORE_CASE)
        val commentRegex = "(?s)^[\"'](.+?)[\"']$".toRegex()
        val definitionRegex = "^[(](.+?)[)]$".toRegex()

        // Stores the index of the labels.
        val labelMap: MutableMap<String, Int> = mutableMapOf()

        // validate and store all .labels
        words.forEachIndexed { index, word ->
            val labelMatch = getRegexOneGroup(word, labelRegex)
            // is label?
            if (labelMatch != null) {
                val label = labelMatch.lowercase(Locale.ROOT)

                if (labelMap.containsKey(label)) {
                    calcData.errorMsg = context.getString(R.string.calc_duplicate_label, labelMatch)
                    calcRepository.updateData(calcData)

                    // duplicate label, end loop
                    return
                } else {
                    // Store label
                    labelMap[label] = index
                }
            } else {
                when (word.lowercase(Locale.ROOT)) {
                    // disable loop check
                    ":loop" -> {
                        checkLoop = false
                    }
                }
            }
        }

        // validate and store all while|if.labels
        val whileMap: MutableMap<String, Int> = mutableMapOf()
        words.forEachIndexed { index, word ->
            val whileIfMatch =
                getRegexThreeGroups(word, whileIfRegex)
            // is while or if?
            if (whileIfMatch != null) {
                val isWhile = whileIfMatch.first.equals("while", true)
                val compare = whileIfMatch.second
                val labelStr = whileIfMatch.third
                val label = labelStr.lowercase(Locale.ROOT)

                if (!compare.matches("eq|le|lt|ge|gt".toRegex())) {
                    calcData.errorMsg = if (isWhile) {
                        context.getString(R.string.calc_unknown_while_condition, compare)
                    } else {
                        context.getString(R.string.calc_unknown_if_condition, compare)
                    }
                    calcRepository.updateData(calcData)

                    // label missing, end loop
                    return
                }

                // check if while-labels are unique
                // if-labels are not checked
                if (isWhile) {
                    if (whileMap.containsKey(label)) {
                        calcData.errorMsg =
                            context.getString(R.string.calc_duplicate_while_label, labelStr)
                        calcRepository.updateData(calcData)

                        // duplicate label, end loop
                        return
                    } else {
                        // Store label
                        whileMap[label] = index
                    }
                }

                if (!labelMap.containsKey(label)) {
                    calcData.errorMsg = if (isWhile) {
                        context.getString(R.string.calc_missing_while_label, labelStr)
                    } else {
                        context.getString(R.string.calc_missing_if_label, labelStr)
                    }
                    calcRepository.updateData(calcData)

                    // label missing, end loop
                    return
                }
            }
        }

        // validate and store all loop.labels
        val loopMap: MutableMap<String, Int> = mutableMapOf()
        val endloopMap: MutableMap<String, Int> = mutableMapOf()
        val loopValuesMap: MutableMap<String, LoopValues> = mutableMapOf()

        words.forEachIndexed { index, word ->
            var labelStr =
                getRegexOneGroup(word, loopRegex)
            // is loop?
            if (labelStr != null) {
                val label = labelStr.lowercase(Locale.ROOT)

                // check if loop-labels are unique
                if (loopMap.containsKey(label)) {
                    calcData.errorMsg =
                        context.getString(R.string.calc_duplicate_loop_label, labelStr)
                    calcRepository.updateData(calcData)

                    // duplicate label, end loop
                    return
                } else {
                    // Store label
                    loopMap[label] = index
                }

                if (!loopMap.containsKey(label)) {
                    calcData.errorMsg =
                        context.getString(R.string.calc_missing_loop_label, labelStr)
                    calcRepository.updateData(calcData)

                    // label missing, end loop
                    return
                }
            }

            labelStr =
                getRegexOneGroup(word, endloopRegex)
            // is end of loop?
            if (labelStr != null) {
                val label = labelStr.lowercase(Locale.ROOT)

                // check if loop-labels are unique
                if (endloopMap.containsKey(label)) {
                    calcData.errorMsg =
                        context.getString(R.string.calc_duplicate_loop_label, labelStr)
                    calcRepository.updateData(calcData)

                    // duplicate label, end loop
                    return
                } else {
                    // Store label
                    endloopMap[label] = index
                }

                if (!endloopMap.containsKey(label)) {
                    calcData.errorMsg =
                        context.getString(R.string.calc_missing_loop_label, labelStr)
                    calcRepository.updateData(calcData)

                    // label missing, end loop
                    return
                }
            }
        }

        // validate and store all definitions
        // : name ... ;
        val definitionMap: MutableMap<String, Int> = mutableMapOf()
        var k = 0
        while (k < words.size) {
            // Search begin of definition.
            if (words[k] == ":") {
                k++
                var endIndex = -1
                var name = ""
                if (k < words.size) {
                    name = words[k]
                    if (name == ";") {
                        calcData.errorMsg = context.getString(R.string.calc_empty_definition)
                        calcRepository.updateData(calcData)

                        // empty definition, end loop
                        return
                    }
                    k++
                }
                val startIndex = k
                // Search end of definition.
                while (k < words.size) {
                    if (words[k] == ";") {
                        endIndex = k
                        break
                    }
                    k++
                }

                // Add definition name and start index to definition
                if (endIndex > 0) {
                    when {
                        wordListRegex.matches(name) -> {
                            calcData.errorMsg =
                                context.getString(R.string.calc_definition_is_keyword, name)
                            calcRepository.updateData(calcData)

                            // definition is keyword, end loop
                            return
                        }
                        definitionMap.containsKey(name) -> {
                            calcData.errorMsg =
                                context.getString(R.string.calc_definition_entry_exists, name)
                            calcRepository.updateData(calcData)

                            // already exists, end loop
                            return
                        }
                        else -> {
                            definitionMap[name] = startIndex
                        }
                    }
                } else {
                    calcData.errorMsg = context.getString(R.string.calc_incomplete_definition, name)
                    calcRepository.updateData(calcData)

                    // incomplete definition, end loop
                    return
                }
            }

            k++
        }

        val returnStack = Stack<Int>()
        var loopCounter: Int = 0
        val currentSize = calcData.numberList.size
        var i: Int = 0
        while (i < words.size) {

            val word = words[i++]

            // is import?
            if (word.startsWith("import", true)) {
                // skip to next word
                continue
            }

            // is label?
            if (getRegexOneGroup(word, labelRegex) != null) {
                // skip to next word
                continue
            }

            // Check for endless loop.
            if (checkLoop && (loopCounter > 10000 || (calcData.numberList.size - currentSize >= 1000))) {
                calcData.errorMsg = context.getString(R.string.calc_endless_loop)
                calcRepository.updateData(calcData)
                return
            }

            // While loop, if jump
            // while|if.[compare].[label]
            // while|if.gt.label1
            val whileIfMatch =
                getRegexThreeGroups(word, whileIfRegex)
            // is while?
            if (whileIfMatch != null) {
                loopCounter++

                // captured compare is in first
                // captured label is in second
                val isWhile = whileIfMatch.first.equals("while", true)
                val compare = whileIfMatch.second
                val labelStr = whileIfMatch.third
                val label = labelStr.lowercase(Locale.ROOT)

                // already checked in validation, kept as runtime test-case
                if (!labelMap.containsKey(label)) {
                    calcData.errorMsg = if (isWhile) {
                        context.getString(R.string.calc_missing_while_label, labelStr)
                    } else {
                        context.getString(R.string.calc_missing_if_label, labelStr)
                    }
                    calcRepository.updateData(calcData)

                    // label missing, end loop
                    return
                }

                val argsValid = calcData.numberList.size >= 2
                if (argsValid) {

                    // 2: op2
                    // 1: op1
                    val op1 = calcData.numberList.removeLast()
                    val op2 = calcData.numberList.removeLast()

                    when (compare.lowercase(Locale.ROOT)) {

                        "ge" -> {
                            if (op2.value >= op1.value) {
                                // jump to label
                                i = labelMap[label]!!
                            }
                        }
                        "gt" -> {
                            if (op2.value > op1.value) {
                                // jump to label
                                i = labelMap[label]!!
                            }
                        }
                        "le" -> {
                            if (op2.value <= op1.value) {
                                // jump to label
                                i = labelMap[label]!!
                            }
                        }
                        "lt" -> {
                            if (op2.value < op1.value) {
                                // jump to label
                                i = labelMap[label]!!
                            }
                        }
                        "eq" -> {
                            if (op2.value == op1.value) {
                                // jump to label
                                i = labelMap[label]!!
                            }
                        }
                        else -> {
                            // already checked in validation, kept as runtime test-case
                            calcData.errorMsg = if (isWhile) {
                                context.getString(R.string.calc_unknown_while_condition, compare)
                            } else {
                                context.getString(R.string.calc_unknown_if_condition, compare)
                            }

                            calcRepository.updateData(calcData)

                            // label missing, end loop
                            return
                        }
                    }

                    continue

                } else {
                    calcData.errorMsg = if (isWhile) {
                        context.getString(R.string.calc_invalid_while_args)
                    } else {
                        context.getString(R.string.calc_invalid_if_args)
                    }
                    calcRepository.updateData(calcData)

                    // invalid args, end instruction
                    return
                }
            }

            // loop.[label]
            val labelStr =
                getRegexOneGroup(word, loopRegex)
            // is loop?
            if (labelStr != null) {
                loopCounter++

                val label = labelStr.lowercase(Locale.ROOT)

                // already checked in validation, kept as runtime test-case
                if (!loopMap.containsKey(label)) {
                    calcData.errorMsg =
                        context.getString(R.string.calc_missing_loop_label, labelStr)
                    calcRepository.updateData(calcData)

                    // label missing, end loop
                    return
                }

                // Store start,end,inc of the loop.
                if (!loopValuesMap.containsKey(label)) {
                    val argsValid = calcData.numberList.size >= 3
                    if (argsValid) {
                        loopValuesMap[label] = LoopValues(
                            inc = calcData.numberList.removeLast(),
                            end = calcData.numberList.removeLast(),
                            start = calcData.numberList.removeLast(),
                        )
                    } else {
                        calcData.errorMsg = context.getString(R.string.calc_invalid_loop_args)
                        calcRepository.updateData(calcData)

                        // invalid args, end instruction
                        return
                    }
                }

                val loopValue = loopValuesMap[label]!!

                // Abbruchbedingung.
                if (loopValue.inc.value > 0.0 && loopValue.start.value >= loopValue.end.value
                    || loopValue.inc.value < 0.0 && loopValue.start.value <= loopValue.end.value
                ) {
                    // End of loop. Jump behind end loop label to continue.
                    i = endloopMap[label]!! + 1
                    loopValuesMap.remove(label)
                    continue
                }

                // Add loop value to be used within the loop.
                calcData.numberList.add(
                    CalcLine(
                        desc = loopValue.start.desc,
                        value = loopValue.start.value
                    )
                )

                // Update loop value.
                loopValue.start.value += loopValue.inc.value

                continue
            }

            // Goto
            // goto.[label]
            val gotoMatch =
                getRegexOneGroup(word, gotoRegex)
            if (gotoMatch != null) {
                loopCounter++
                val label = gotoMatch.lowercase(Locale.ROOT)

                if (!labelMap.containsKey(label)) {
                    calcData.errorMsg = context.getString(R.string.calc_missing_label, gotoMatch)
                    calcRepository.updateData(calcData)

                    // label missing, end instruction
                    return
                }

                // jump to label
                i = labelMap[label]!!
                continue
            }

            // End of loop.
            // end.[label]
            val endloopMatch =
                getRegexOneGroup(word, endloopRegex)
            if (endloopMatch != null) {
                loopCounter++
                val label = endloopMatch.lowercase(Locale.ROOT)

                if (!loopMap.containsKey(label)) {
                    calcData.errorMsg = context.getString(R.string.calc_missing_label, endloopMatch)
                    calcRepository.updateData(calcData)

                    // label missing, end instruction
                    return
                }

                // jump to label
                i = loopMap[label]!!
                continue
            }

            // { lambda }
            // Add the index of the lambda definition.
            if (word == "{") {
                loopCounter++

                calcData.numberList.add(
                    CalcLine(
                        desc = "",
                        value = Double.NaN,
                        lambda = i,
                        definition = "\uD83D\uDC7D@$i"
                    )
                )

                var lambdaEnd = false
                // Search end of definition.
                var j = i
                while (j < words.size) {
                    if (words[j++] == "}") {
                        lambdaEnd = true
                        break
                    }
                }

                if (!lambdaEnd) {
                    calcData.errorMsg = context.getString(R.string.calc_lambda_not_terminated)
                    calcRepository.updateData(calcData)

                    // terminator missing, end instruction
                    return
                }

                i = j
                continue
            }

            // definition function?
            val wordLwr = word.lowercase(Locale.ROOT)
            if (definitionMap.containsKey(wordLwr)) {
                loopCounter++

                returnStack.push(i)
                i = definitionMap[wordLwr]!!

                continue
            }

            // (definition)
            // Add the index of the definition.
            val definitionMatch =
                getRegexOneGroup(word, definitionRegex)
            if (definitionMatch != null) {
                loopCounter++

                val definition = definitionMatch.lowercase(Locale.ROOT)
                if (definitionMap.containsKey(definition)) {
                    loopCounter++

                    calcData.numberList.add(
                        CalcLine(
                            desc = "",
                            value = Double.NaN,
                            lambda = definitionMap[definition]!!,
                            definition = definitionMatch
                        )
                    )

                    continue
                }
            }

            // ()
            // Run the address of the current item.
            if (word == "()") {
                val op = calcData.numberList.removeLast()
                if (op.lambda >= 0) {
                    loopCounter++

                    returnStack.push(i)
                    i = op.lambda

                    continue
                } else {
                    calcData.errorMsg = context.getString(R.string.calc_missing_lambda)
                    calcRepository.updateData(calcData)

                    // lambda missing, end instruction
                    return
                }
            }

            // Vector/Matrix
            if (word == "[") {
                loopCounter++

                if (i < words.size && words[i] == "[") {
                    // Matrix
                    i++

                    var columnSize = 0
                    val rowList: MutableList<MutableList<Double>> = mutableListOf()
                    val doubleList: MutableList<Double> = mutableListOf()

                    var inVector = true

                    while (i < words.size) {
                        val nextWord = words[i++]
                        if (!inVector && nextWord == "]") {

                            calcData.numberList.add(
                                CalcLine(
                                    value = Double.NaN,
                                    matrix = Array(rowList.size) { r -> DoubleArray(rowList[0].size) { c -> rowList[r][c] } }
                                )
                            )

                            break
                        }

                        if (inVector && nextWord == "]") {
                            if (columnSize == 0) {
                                columnSize = doubleList.size
                            } else
                                if (columnSize != doubleList.size) {
                                    // Error
                                    calcData.errorMsg =
                                        context.getString(R.string.calc_error_matrix_different_row_length)
                                    calcRepository.updateData(calcData)

                                    return
                                }

                            rowList.add(doubleList.toMutableList())
                            doubleList.clear()

                            inVector = false
                            continue
                        }

                        if (!inVector && nextWord == "[") {

                            inVector = true
                            continue
                        }

                        if (inVector && nextWord.isNotEmpty()) {
                            try {
                                val value = numberFormat.parse(nextWord)!!
                                    .toDouble()

                                doubleList.add(value)
                            } catch (e: Exception) {
                                // Error
                                calcData.errorMsg =
                                    context.getString(R.string.calc_error_parsing_msg, nextWord)
                                calcRepository.updateData(calcData)

                                // invalid number missing
                                return
                            }
                        } else {
                            // Error
                            calcData.errorMsg =
                                context.getString(R.string.calc_error_parsing_matrix, nextWord)
                            calcRepository.updateData(calcData)

                            // invalid entry
                            return
                        }
                    }
                } else {
                    // Vector

                    val doubleList: MutableList<Double> = mutableListOf()
                    while (i < words.size) {
                        val nextWord = words[i++]
                        if (nextWord == "]") {
                            calcData.numberList.add(
                                CalcLine(
                                    value = Double.NaN,
                                    vector = DoubleArray(doubleList.size) { v -> doubleList[v] }
                                )
                            )

                            break
                        }

                        if (nextWord.isNotEmpty()) {
                            try {
                                val value = numberFormat.parse(nextWord)!!
                                    .toDouble()

                                doubleList.add(value)
                            } catch (e: Exception) {
                                // Error
                                calcData.errorMsg =
                                    context.getString(R.string.calc_error_parsing_msg, nextWord)
                                calcRepository.updateData(calcData)

                                // invalid number missing
                                return
                            }
                        }
                    }

                    if (i == words.size && words[i - 1] != "]") {
                        calcData.errorMsg =
                            context.getString(R.string.calc_error_missing_closing_vector_bracket)
                        calcRepository.updateData(calcData)

                        return
                    }
                }

                continue
            }

            // skip definition declarations :..;
            if (word == ":") {
                loopCounter++

                do {
                    i++
                } while (i < words.size && words[i] != ";")
                // skip ;
                i++

                continue
            }

            // Definition function or lambda was called, return to the calling word.
            if (word == ";" || word == "}") {
                loopCounter++

                if (returnStack.isNotEmpty()) {
                    i = returnStack.pop()
                }

                continue
            }

            // process words
            when (wordLwr) {

                // Math operations
                "sin" -> {
                    validArgs = opUnary(calcData, UnaryArgument.SIN)
                }
                "cos" -> {
                    validArgs = opUnary(calcData, UnaryArgument.COS)
                }
                "tan" -> {
                    validArgs = opUnary(calcData, UnaryArgument.TAN)
                }
                "arcsin" -> {
                    validArgs = opUnary(calcData, UnaryArgument.ARCSIN)
                }
                "arccos" -> {
                    validArgs = opUnary(calcData, UnaryArgument.ARCCOS)
                }
                "arctan" -> {
                    validArgs = opUnary(calcData, UnaryArgument.ARCTAN)
                }
                "arctan2" -> {
                    validArgs = opBinary(calcData, BinaryArgument.ARCTAN2)
                }
                "sinh" -> {
                    validArgs = opUnary(calcData, UnaryArgument.SINH)
                }
                "cosh" -> {
                    validArgs = opUnary(calcData, UnaryArgument.COSH)
                }
                "tanh" -> {
                    validArgs = opUnary(calcData, UnaryArgument.TANH)
                }
                "arcsinh" -> {
                    validArgs = opUnary(calcData, UnaryArgument.ARCSINH)
                }
                "arccosh" -> {
                    validArgs = opUnary(calcData, UnaryArgument.ARCCOSH)
                }
                "arctanh" -> {
                    validArgs = opUnary(calcData, UnaryArgument.ARCTANH)
                }
                "ln" -> {
                    validArgs = opUnary(calcData, UnaryArgument.LN)
                }
                "log" -> {
                    validArgs = opUnary(calcData, UnaryArgument.LOG)
                }
                "sq" -> {
                    validArgs = opUnary(calcData, UnaryArgument.SQ)
                }
                "sqrt" -> {
                    validArgs = opUnary(calcData, UnaryArgument.SQRT)
                }
                "inv" -> {
                    validArgs = opUnary(calcData, UnaryArgument.INV)
                }
                "abs" -> {
                    validArgs = opUnary(calcData, UnaryArgument.ABS)
                }
                "int" -> {
                    validArgs = opUnary(calcData, UnaryArgument.INT)
                }
                "mod" -> {
                    validArgs = opBinary(calcData, BinaryArgument.MOD)
                }
                "round" -> {
                    validArgs = opUnary(calcData, UnaryArgument.ROUND)
                }
                "round2" -> {
                    validArgs = opUnary(calcData, UnaryArgument.ROUND2)
                }
                "round4" -> {
                    validArgs = opUnary(calcData, UnaryArgument.ROUND4)
                }
                "frac" -> {
                    validArgs = opUnary(calcData, UnaryArgument.FRAC)
                }
                "rand" -> {
                    opZero(calcData, ZeroArgument.RAND)
                }
                "min" -> {
                    validArgs = opBinary(calcData, BinaryArgument.MIN)
                }
                "max" -> {
                    validArgs = opBinary(calcData, BinaryArgument.MAX)
                }
                "factorial", "!" -> {
                    validArgs = opUnary(calcData, UnaryArgument.FACTORIAL)
                }
                "tostr" -> {
                    validArgs = opUnary(calcData, UnaryArgument.TOSTR)
                }
                "sum" -> {
                    validArgs = opVarArg(calcData, VariableArguments.SUM)
                }
                "size" -> {
                    validArgs = opUnary(calcData, UnaryArgument.SIZE)
                }
                "var" -> {
                    validArgs = opVarArg(calcData, VariableArguments.VAR)
                }
                "per" -> {
                    validArgs = opBinary(calcData, BinaryArgument.PER)
                }
                "perc" -> {
                    validArgs = opBinary(calcData, BinaryArgument.PERC)
                }
                "binom" -> {
                    validArgs = opBinary(calcData, BinaryArgument.BINOM)
                }
                "erf" -> {
                    validArgs = opUnary(calcData, UnaryArgument.ERF)
                }

                // logic operations
                "and" -> {
                    validArgs = opBinary(calcData, BinaryArgument.AND)
                }
                "or" -> {
                    validArgs = opBinary(calcData, BinaryArgument.OR)
                }
                "xor" -> {
                    validArgs = opBinary(calcData, BinaryArgument.XOR)
                }
                "not" -> {
                    validArgs = opUnary(calcData, UnaryArgument.NOT)
                }
                "complement" -> {
                    validArgs = opUnary(calcData, UnaryArgument.COMPLEMENT)
                }

                // Stack operations
                "clear" -> {
                    opZero(calcData, ZeroArgument.CLEAR)
                }
                "depth" -> {
                    opZero(calcData, ZeroArgument.DEPTH)
                }
                "drop" -> {
                    validArgs = opUnary(calcData, UnaryArgument.DROP)
                }
                "dup" -> {
                    validArgs = opUnary(calcData, UnaryArgument.DUP)
                }
                "over" -> {
                    validArgs = opBinary(calcData, BinaryArgument.OVER)
                }
                "swap" -> {
                    validArgs = opBinary(calcData, BinaryArgument.SWAP)
                }
                "rot" -> {
                    validArgs = opTernary(calcData, TernaryArgument.ROT)
                }
                "pick" -> {
                    validArgs = opVarArg(calcData, VariableArguments.PICK)
                }
                "roll" -> {
                    validArgs = opVarArg(calcData, VariableArguments.ROLL)
                }

                // Solve
                "solve" -> {
                    validArgs = opBinary(calcData, BinaryArgument.SOLVE)
                }

                // Create/Get/Set Element
                "vector" -> {
                    validArgs = opUnary(calcData, UnaryArgument.VECTOR)
                }
                "matrix" -> {
                    validArgs = opBinary(calcData, BinaryArgument.MATRIX)
                }
                "getv" -> {
                    validArgs = opBinary(calcData, BinaryArgument.GETV)
                }
                "setv" -> {
                    validArgs = opTernary(calcData, TernaryArgument.SETV)
                }
                "getm" -> {
                    validArgs = opTernary(calcData, TernaryArgument.GETM)
                }
                "setm" -> {
                    validArgs = opQuad(calcData, QuadArgument.SETM)
                }

                // Conditional operations
                "if.eq" -> {
                    validArgs = opQuad(calcData, QuadArgument.IFEQ)
                }
                "if.ge" -> {
                    validArgs = opQuad(calcData, QuadArgument.IFGE)
                }
                "if.gt" -> {
                    validArgs = opQuad(calcData, QuadArgument.IFGT)
                }
                "if.le" -> {
                    validArgs = opQuad(calcData, QuadArgument.IFLE)
                }
                "if.lt" -> {
                    validArgs = opQuad(calcData, QuadArgument.IFLT)
                }

                // Arithmetic operations
                "+" -> {
                    validArgs = opBinary(calcData, BinaryArgument.ADD)
                }
                "-" -> {
                    validArgs = opBinary(calcData, BinaryArgument.SUB)
                }
                "*" -> {
                    validArgs = opBinary(calcData, BinaryArgument.MULT)
                }
                "/" -> {
                    validArgs = opBinary(calcData, BinaryArgument.DIV)
                }
                "hypot" -> {
                    validArgs = opBinary(calcData, BinaryArgument.HYPOT)
                }
                "^", "pow" -> {
                    validArgs = opBinary(calcData, BinaryArgument.POW)
                }

                // Formatting and number operations
                "", ":loop" -> {
                    // Skip empty lines and instructions.
                }
                ":radian" -> {
                    radian = 1.0
                }
                ":clearcode" -> {
                    codeMap.clear()
                    // Add empty entry to not set to default if code is all empty.
                    codeMap["F0"] = CodeType(code = "", name = "")
                }
                ":defaultcode" -> {
                    codeMap.clear()
                }
                ":degree" -> {
                    radian = Math.PI / 180
                }
                "pi", "π" -> {
                    opZero(calcData, ZeroArgument.PI)
                }
                "e" -> {
                    opZero(calcData, ZeroArgument.E)
                }
                "\u0061\u006c\u0069\u0065\u006e" -> {
                    val a = "\uD83D\uDEF8\uD83D\uDC7D\uD83D\uDC7E" +
                            " ".repeat((1..17).shuffled().first())
                    if (calcData.numberList.isEmpty()) {
                        calcData.numberList.add(
                            CalcLine(
                                desc = a,
                                value = Double.NaN
                            )
                        )
                    } else {
                        val op = calcData.numberList.removeLast()
                        calcData.numberList.add(
                            CalcLine(
                                desc = "$a ${op.desc}",
                                value = op.value
                            )
                        )
                    }
                }
                "validate" -> {
                    validArgs = opVarArg(calcData, VariableArguments.VALIDATE)
                    // Stop here when validate fails.
                    if (!validArgs) {
                        return
                    }
                }

                // Variable operation
                "rcl" -> {
                    recallAllVariables(calcData, false)
                }
                "memrcl" -> {
                    recallAllVariables(calcData, true)
                }
                else -> {

                    // Comment
                    // If word is comment, add comment to the last entry.
                    // (?s) = dotall = . + \n
                    val comment = getRegexOneGroup(word, commentRegex)
                    // is comment?
                    if (comment != null) {

//            // Add line if list is empty to add the text to.
//            if (calcData.numberList.isEmpty()) {
//              calcData.numberList.add(CalcLine(desc = desc, value = Double.NaN))
//            } else {
//              val op = calcData.numberList.removeLast()
//              if (op.value.isNaN()) {
//                // add comment
//                op.desc += desc
//              } else {
//                op.desc = desc
//              }
//              calcData.numberList.add(op)
//            }

                        calcData.numberList.add(CalcLine(desc = comment, value = Double.NaN))

                    } else {

                        // sto[.name]
                        val variableName = getRegexOneGroup(word, stoRegex)
                        if (variableName != null) {
                            validArgs = storeVariable(calcData, variableName, false)

                        } else {

                            // rcl[.name]
                            val recallVariable = getRegexOneGroup(word, rclRegex)
                            if (recallVariable != null) {
                                recallVariable(calcData, recallVariable, false)

                            } else {

                                // memsto[.name]
                                val memVariableName = getRegexOneGroup(word, memstoRegex)
                                if (memVariableName != null) {
                                    validArgs = storeVariable(calcData, memVariableName, true)

                                } else {

                                    // memrcl[.name]
                                    val memRecallVariable = getRegexOneGroup(word, memrclRegex)
                                    if (memRecallVariable != null) {
                                        recallVariable(calcData, memRecallVariable, true)

                                    } else {

                                        // memdel[.name]
                                        val memDeleteVariable = getRegexOneGroup(word, memdelRegex)
                                        if (memDeleteVariable != null) {
                                            deleteVariable(calcData, memDeleteVariable)

                                        } else {

                                            // Evaluate number
                                            try {
                                                val value = numberFormat.parse(word)!!
                                                    .toDouble()

                                                calcData.numberList.add(
                                                    CalcLine(
                                                        desc = "",
                                                        value = value
                                                    )
                                                )
                                            } catch (e: Exception) {
                                                // Error
                                                calcData.errorMsg =
                                                    context.getString(
                                                        R.string.calc_error_parsing_msg,
                                                        word
                                                    )
                                                success = false
                                            }

                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!success) {
                if (calcData.errorMsg.isEmpty()) {
                    calcData.errorMsg = context.getString(R.string.calc_error_msg, word)
                }
                calcRepository.updateData(calcData)

                return
            } else
                if (!validArgs) {
                    if (calcData.errorMsg.isEmpty()) {
                        calcData.errorMsg = context.getString(R.string.calc_invalid_args)
                    }
                    calcRepository.updateData(calcData)

                    return
                }
        }

        calcRepository.updateData(calcData)
    }

    private fun calcLineToStr(calcLine: CalcLine): String {

        var jsonString = ""

        try {
            // Convert to a json string.
            val gson: Gson = GsonBuilder()
                .create()

            val calcLineJson = CalcLineJson(
                desc = calcLine.desc,
                value = if(calcLine.value.isFinite()) calcLine.value else null,
                lambda = calcLine.lambda,
                definition = calcLine.definition,
                vector = null,
                matrix = null,
            )

            if (calcLine.vector != null) {
                val vector1 = mutableListOf<Double>()
                calcLine.vector!!.forEachIndexed { index, d -> vector1.add(d) }
                calcLineJson.vector = vector1.toList()
            }

            if (calcLine.matrix != null) {
                val rows = calcLine.matrix!!.size

                if (rows > 0) {
                    val cols = calcLine.matrix!![0].size
                    val array: MutableList<MutableList<Double>> =
                        MutableList(rows) { MutableList(cols) { 0.0 } }

                    for (row in 0 until rows) {
                        for (col in 0 until cols) {
                            array[row][col] += calcLine.matrix!![row][col]
                        }
                    }
                    calcLineJson.matrix = array
                }
            }

            jsonString = gson.toJson(calcLineJson)

        } catch (e: Exception) {
        }

        return jsonString
    }

    private fun strToCalcLine(
        data: String
    ): CalcLine {

        try {

            val sType = object : TypeToken<CalcLineJson>() {}.type
            val gson = Gson()
            val calcLineJson = gson.fromJson<CalcLineJson>(data, sType)

            val calcLine = CalcLine(
                desc = calcLineJson.desc,
                value = if(calcLineJson.value == null) Double.NaN else calcLineJson.value!!,
                lambda = calcLineJson.lambda,
                definition = calcLineJson.definition,
                vector = null,
                matrix = null,
            )

            if (calcLineJson.vector != null) {
                calcLine.vector =
                    DoubleArray(calcLineJson.vector!!.size) { c -> calcLineJson.vector!![c] }
            }

            if (calcLineJson.matrix != null) {
                val rows = calcLineJson.matrix!!.size

                if (rows > 0) {
                    val cols = calcLineJson.matrix!![0].size
                    calcLine.matrix =
                        Array(rows) { r -> DoubleArray(cols) { c -> calcLineJson.matrix!![r][c] } }
                }
            }

            return calcLine

        } catch (e: Exception) {
        }

        return CalcLine()
    }

    private fun storeVariable(
        calcData: CalcData,
        name: String,
        permanentStorage: Boolean
    ): Boolean {
        val argsValid = calcData.numberList.size > 0

        if (argsValid) {
            endEdit(calcData)

            // 1: op1
            val op1 = calcData.numberList.removeLast()

            if (permanentStorage) {
                val sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(context /* Activity context */)

                val memStr = calcLineToStr(op1)
                sharedPreferences
                    .edit()
                    .putString("$MEM_STORE_PREFIX$name", memStr)
                    .apply()

            } else {
                varMap[name] = op1
            }

        } else {
            calcData.errorMsg = context.getString(R.string.calc_invalid_args)
        }

        calcRepository.updateData(calcData)

        return argsValid
    }

    private fun recallVariable(calcData: CalcData, name: String, permanentStorage: Boolean) {
        endEdit(calcData)

        if (permanentStorage) {
            val sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context /* Activity context */)

            val memStr = sharedPreferences.getString("$MEM_STORE_PREFIX$name", "").toString()
            calcData.numberList.add(strToCalcLine(memStr))

            calcRepository.updateData(calcData)
        } else {
            if (varMap.containsKey(name)) {
                calcData.numberList.add(varMap[name]!!)

                calcRepository.updateData(calcData)
            }
        }
    }

    private fun recallAllVariables(calcData: CalcData, permanentStorage: Boolean) {
        endEdit(calcData)

        if (permanentStorage) {
            val sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context /* Activity context */)

            sharedPreferences.all.forEach { element ->
                val memName: String = element.key
                if (memName.startsWith(MEM_STORE_PREFIX)) {
                    val name = memName.substring(MEM_STORE_PREFIX.length)
                    calcData.numberList.add(CalcLine(desc = "Variable '$name'", value = Double.NaN))
                    val memValue: String = element.value as String
                    calcData.numberList.add(strToCalcLine(memValue))
                }
            }
        } else {
            varMap.forEach { (key, value) ->
                calcData.numberList.add(CalcLine(desc = "Variable '$key'", value = Double.NaN))
                calcData.numberList.add(value)
            }
            //calcData.numberList.add(CalcLine(desc = "${varMap.size}", value = Double.NaN))
        }

        calcRepository.updateData(calcData)
    }

    private fun deleteVariable(calcData: CalcData, name: String) {
        endEdit(calcData)

        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context /* Activity context */)

        sharedPreferences
            .edit()
            .remove("$MEM_STORE_PREFIX$name")
            .apply()
    }

    private fun displayValue(value: Double): String {
        return if (sciFormat) {
            value.toString().replace('.', separatorChar)
        } else {
            numberFormat.format(value)
        }
    }

    // clipboard import/export
    fun getText(): String {
        val calcData = submitEditline(calcData.value!!)
        calcRepository.updateData(calcData)

        if (calcData.numberList.isNotEmpty()) {
            // numberFormat.format(calcData.numberList.last().value)

            val data = calcData.numberList.last()

            val value = if (!data.value.isNaN()) {
                displayValue(data.value)
            } else
            // CalcAdapter has "[" for display purpose, but bracket needs a space to separate from the number for parsing.
                if (data.vector != null) {
                    data.vector!!.joinToString(
                        prefix = "[ ",
                        separator = " ",
                        postfix = " ]",
                    ) {
                        displayValue(
                            it
                        )
                    }
                } else
                    if (data.matrix != null) {
                        data.matrix!!.map { row ->

                            row.joinToString(
                                prefix = "[ ",
                                separator = " ",
                                postfix = " ]",
                            ) {
                                displayValue(
                                    it
                                )
                            }
                        }.joinToString(
                            prefix = "[ ",
                            separator = "  \n",
                            postfix = " ]",
                        )
                    } else {
                        ""
                    }

            val text = if (data.desc.isNotEmpty()) {
                var desc = "\"${data.desc}\""
                if (value.isNotEmpty()) {
                    desc += " $value +"
                }
                desc
            } else {
                value
            }

            return text
        }

        return ""
    }

    fun setText(text: String?, desc: String) {
        if (text != null && text.isNotEmpty()) {

            // Use function(...) to interpret the content instead of parsing as a number.
            // This allows pasting code scripts.
            function(text)

//      val calcData = calcData.value!!
//
//      calcData.editMode = true
//      calcData.editline = text
//      calcData.errorMsg = ""
//
//      calcRepository.updateData(submitEditline(calcData, desc))

        }
    }

    fun add(value: Double, desc: String) {
        val calcData = calcData.value!!

        endEdit(calcData)
        calcData.numberList.add(CalcLine(desc = desc, value = value))

        calcRepository.updateData(calcData)
    }

    fun addNum(char: Char, context: Context? = null) {
        val calcData = calcData.value!!

        if (char == separatorChar) {
            aic++

            if (context != null && aic == 7) {
                AlertDialog.Builder(context)
                    // https://convertcodes.com/unicode-converter-encode-decode-utf/
                    .setTitle(
                        "\u0041\u0049\u0020\u003d\u0020\u0041\u006c\u0069\u0065\u006e\u0020\u0049\u006e\u0074\u0065\u006c\u006c\u0069\u0067\u0065\u006e\u0063\u0065"
                    )
                    .setMessage(
                        "\u0057\u0061\u0074\u0063\u0068\u0020\u006f\u0075\u0074\u002e"
                    )
                    .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                    .show()
            }

            // Validation:
            // Only one separator char allowed.
            if (calcData.editline.contains(char)) {
                return
            }
        } else {
            aic = 0
        }

        calcData.editMode = true
        calcData.editline += char
        calcData.errorMsg = ""

        calcRepository.updateData(calcData)
    }

    fun addEEX() {
        val calcData = calcData.value!!

        // Add 'E' if not already there and at least one digit is available.
        if ("\\d+".toRegex()
                .containsMatchIn(calcData.editline) && !calcData.editline.contains('E')
        ) {
            addNum('E')
        } else {
            // Toggle scientific format display when EEX is clicked outside of a number.
            sciFormat = !sciFormat
            updateData()
        }
    }

    fun sign() {
        val calcData = calcData.value!!

        // Add/change '-' if in edit mode for 'E'
        // 1E-3
        if (calcData.editMode && calcData.editline.contains('E')) {

            calcData.editline = when {
                calcData.editline.contains("E-") -> {
                    calcData.editline.replace("E-", "E+")
                }
                calcData.editline.contains("E+") -> {
                    calcData.editline.replace("E+", "E-")
                }
                else -> {
                    calcData.editline.replace("E", "E-")
                }
            }

            calcRepository.updateData(calcData)

        } else {

            opUnary(UnaryArgument.SIGN)

        }
    }

    fun opVarArg(op: VariableArguments): Boolean {
        val calcData = submitEditline(calcData.value!!)
        return opVarArg(calcData, op)
    }

    private fun opVarArg(calcData: CalcData, op: VariableArguments): Boolean {
        var argsValid = false
        endEdit(calcData)

        when (op) {
            VariableArguments.PICK -> {
                var size = calcData.numberList.size
                if (size > 1) {

                    val n = calcData.numberList.removeLast().value.toInt()
                    size = calcData.numberList.size
                    if (size - n >= 0 && n > 0) {
                        argsValid = true
                        // copy level n to level 1
                        val nLevelOp = calcData.numberList[size - n]
                        // Clone nLevelOp
                        calcData.numberList.add(
                            CalcLine(
                                desc = nLevelOp.desc,
                                value = nLevelOp.value
                            )
                        )
                    }
                }
            }
            VariableArguments.ROLL -> {
                var size = calcData.numberList.size
                if (size > 1) {

                    val n = calcData.numberList.removeLast().value.toInt()
                    size = calcData.numberList.size
                    if (size - n >= 0 && n > 0) {
                        argsValid = true
                        // move level n to level 1
                        val nLevelOp = calcData.numberList.removeAt(size - n)
                        // Copy nLevelOp
                        calcData.numberList.add(nLevelOp)
                    }
                }
            }
            VariableArguments.SUM -> {
                var n = 0
                var sum = 0.0
                calcData.numberList.forEach { calcLine ->
                    if (calcLine.value.isFinite()) {
                        n++
                        sum += calcLine.value
                    }
                }
                if (n > 1) {
                    argsValid = true
                    calcData.numberList.clear()
                    calcData.numberList.add(CalcLine(desc = "Σ=", value = sum))
                    calcData.numberList.add(CalcLine(desc = "n=", value = n.toDouble()))
                }
            }
            VariableArguments.VAR -> {
                val size = calcData.numberList.size
                if (size > 1) {
                    var sum = 0.0
                    var n = 0
                    calcData.numberList.forEach { calcLine ->
                        val x = calcLine.value
                        if (x.isFinite()) {
                            n++
                            sum += x
                        }
                    }

                    if (n > 1) {
                        argsValid = true
                        val mean = sum / n

                        var variance = 0.0
                        calcData.numberList.forEach { calcLine ->
                            val x1 = calcLine.value
                            if (x1.isFinite()) {
                                val x = x1 - mean
                                variance += x * x
                            }
                        }

                        variance /= n

                        calcData.numberList.clear()
                        calcData.numberList.add(CalcLine(desc = "σ²=", value = variance))
                    }
                }
            }
            VariableArguments.VALIDATE -> {
                val size = calcData.numberList.size
                if (size >= 2) {

                    val n = calcData.numberList.removeLast().value.toInt()
                    val errorOp = calcData.numberList.removeLast()
                    if (n > 0 && n <= size - 2) {
                        argsValid = true
                        for (i in 0 until n) {
                            if (!calcData.numberList[size - 3 - i].value.isFinite()) {
                                argsValid = false
                                break
                            }
                        }
                    }
                    if (!argsValid) {
                        calcData.numberList.add(errorOp)
                        calcRepository.updateData(calcData)
                        return false
                    }
                }
            }
        }

        if (!argsValid) {
            calcData.errorMsg = context.getString(R.string.calc_invalid_args)
        }

        calcRepository.updateData(calcData)

        return argsValid
    }

    fun opZero(op: ZeroArgument, radix: Int = 0) {
        val calcData = submitEditline(calcData.value!!, radix)
        opZero(calcData, op)
    }

    private fun opZero(calcData: CalcData, op: ZeroArgument) {
        endEdit(calcData)

        when (op) {
            ZeroArgument.CLEAR -> {
                calcData.numberList.clear()
            }
            ZeroArgument.DEPTH -> {
                val size = calcData.numberList.size.toDouble()
                calcData.numberList.add(CalcLine(desc = "", value = size))
            }
            ZeroArgument.PI -> {
                calcData.numberList.add(CalcLine(desc = "", value = Math.PI))
            }
            ZeroArgument.E -> {
                calcData.numberList.add(CalcLine(desc = "", value = Math.E))
            }
            ZeroArgument.RAND -> {
                calcData.numberList.add(CalcLine(desc = "", value = Math.random()))
            }
        }

        calcRepository.updateData(calcData)
    }

    fun opUnary(op: UnaryArgument, radix: Int = 0): Boolean {
        val calcData = submitEditline(calcData.value!!, radix)
        return opUnary(calcData, op)
    }

    private fun opUnary(calcData: CalcData, op: UnaryArgument): Boolean {
        var argsValid = calcData.numberList.size > 0

        if (argsValid) {
            endEdit(calcData)

//      // Validation
//      val d = calcData.numberList.last()
//      if (op == UnaryOperation.INV && (d == 0.0 || d.isNaN())) {
//        return
//      }
//      if (op == UnaryOperation.SQR && d < 0.0) {
//        return
//      }

            val op1 = calcData.numberList.removeLast()

            when (op) {
                UnaryArgument.DROP -> {
                }
                UnaryArgument.DUP -> {
                    calcData.numberList.add(op1)
                    calcData.numberList.add(op1)
                }
                UnaryArgument.SQRT -> {
                    calcData.numberList.add(CalcLine(desc = "", value = op1.value.pow(0.5)))
                }
                UnaryArgument.SQ -> {
                    //calcData.numberList.add(CalcLine(desc = "", value = op1.value.pow(2)))
                    calcData.numberList.add(op1 * op1)
                }
                UnaryArgument.INV -> {
                    calcData.numberList.add(
                        if (op1.vector != null) {
                            vectorInvers(op1)
                        } else
                            if (op1.matrix != null) {
                                matrixInvers(op1)
                            } else
                            // Reverse desc if no value is present.
                                if (op1.desc.isNotEmpty() && op1.value.isNaN()) {
                                    CalcLine(desc = op1.desc.reversed(), value = Double.NaN)
                                } else {
                                    CalcLine(desc = "", value = 1 / op1.value)
                                }
                    )
                }
                UnaryArgument.ABS -> {
                    calcData.numberList.add(
                        CalcLine(
                            desc = "",
                            value = op1.value.absoluteValue
                        )
                    )
                }
                UnaryArgument.SIGN -> {
                    calcData.numberList.add(-op1)
                }
                UnaryArgument.INT -> {
                    calcData.numberList.add(
                        CalcLine(
                            desc = "",
                            value = op1.value.toInt().toDouble()
                        )
                    )
                }
                UnaryArgument.ROUND -> {
                    calcData.numberList.add(
                        CalcLine(
                            desc = "",
                            // roundToLong is not defined for Double.NaN
                            value = if (op1.value.isFinite()) {
                                op1.value.roundToLong().toDouble()
                            } else {
                                op1.value
                            }
                        )
                    )
                }
                UnaryArgument.ROUND2 -> {
                    calcData.numberList.add(
                        CalcLine(
                            desc = "",
                            // roundToLong is not defined for Double.NaN
                            value = if (op1.value.isFinite()) {
                                op1.value.times(100.0).roundToLong().toDouble().div(100.0)
                            } else {
                                op1.value
                            }
                        )
                    )
                }
                UnaryArgument.ROUND4 -> {
                    calcData.numberList.add(
                        CalcLine(
                            desc = "",
                            // roundToLong is not defined for Double.NaN
                            value = if (op1.value.isFinite()) {
                                op1.value.times(10000.0).roundToLong().toDouble().div(10000.0)
                            } else {
                                op1.value
                            }
                        )
                    )
                }
                UnaryArgument.FRAC -> {
                    val fraction = frac(op1.value)
                    if (fraction.first != null) {
                        calcData.numberList.add(
                            CalcLine(
                                desc = "",
                                value = fraction.first!!.toDouble()
                            )
                        )
                        calcData.numberList.add(
                            CalcLine(
                                desc = "",
                                value = fraction.second.toDouble()
                            )
                        )
                    } else {
                        calcData.numberList.add(op1)
                    }
                }
                UnaryArgument.FACTORIAL -> {
                    calcData.numberList.add(
                        CalcLine(
                            desc = "",
                            value = factorial(op1.value.toInt()).toDouble()
                        )
                    )
                }
                UnaryArgument.TOSTR -> {
                    // Convert the value to string.
                    if (op1.value.isFinite()) {
                        calcData.numberList.add(
                            CalcLine(
                                desc = numberFormat.format(op1.value),
                                value = Double.NaN
                            )
                        )
                    } else {
                        calcData.numberList.add(op1)
                    }
                }
                UnaryArgument.SIZE -> {
                    if (op1.vector != null) {
                        // Add vector length.
                        calcData.numberList.add(
                            CalcLine(
                                desc = "",
                                value = op1.vector!!.size.toDouble()
                            )
                        )
                    } else if (op1.matrix != null) {
                        // Add matrix size in rows and columns.
                        calcData.numberList.add(
                            CalcLine(
                                desc = "",
                                value = op1.matrix!!.size.toDouble()
                            )
                        )
                        calcData.numberList.add(
                            CalcLine(
                                desc = "",
                                value = op1.matrix!![0].size.toDouble()
                            )
                        )
                    } else {
                        // Add description length.
                        calcData.numberList.add(
                            CalcLine(
                                desc = "",
                                value = op1.desc.length.toDouble()
                            )
                        )
                    }
                }
                // Create vector Element.
                UnaryArgument.VECTOR -> {
                    val n = op1.value.toInt()
                    if (n > 0 && n <= VECTORMAX) {
                        val vector =
                            DoubleArray(n) { r ->
                                0.0
                            }

                        calcData.numberList.add(CalcLine(desc = "", value = Double.NaN, vector = vector))
                    } else {
                        calcData.errorMsg =
                            context.getString(R.string.calc_invalid_new_vector_args, VECTORMAX)
                        argsValid = false
                    }
                }
                UnaryArgument.SIN -> {
                    calcData.numberList.add(
                        CalcLine(
                            desc = "",
                            value = sin(op1.value * radian)
                        )
                    )
                }
                UnaryArgument.SINH -> {
                    calcData.numberList.add(CalcLine(desc = "", value = sinh(op1.value)))
                }
                UnaryArgument.COS -> {
                    calcData.numberList.add(
                        CalcLine(
                            desc = "",
                            value = cos(op1.value * radian)
                        )
                    )
                }
                UnaryArgument.COSH -> {
                    calcData.numberList.add(CalcLine(desc = "", value = cosh(op1.value)))
                }
                UnaryArgument.TAN -> {
                    calcData.numberList.add(
                        CalcLine(
                            desc = "",
                            value = tan(op1.value * radian)
                        )
                    )
                }
                UnaryArgument.TANH -> {
                    calcData.numberList.add(CalcLine(desc = "", value = tanh(op1.value)))
                }
                UnaryArgument.ARCSIN -> {
                    calcData.numberList.add(
                        CalcLine(
                            desc = "",
                            value = asin(op1.value) / radian
                        )
                    )
                }
                UnaryArgument.ARCSINH -> {
                    calcData.numberList.add(CalcLine(desc = "", value = asinh(op1.value)))
                }
                UnaryArgument.ARCCOS -> {
                    calcData.numberList.add(
                        CalcLine(
                            desc = "",
                            value = acos(op1.value) / radian
                        )
                    )
                }
                UnaryArgument.ARCCOSH -> {
                    calcData.numberList.add(CalcLine(desc = "", value = acosh(op1.value)))
                }
                UnaryArgument.ARCTAN -> {
                    calcData.numberList.add(
                        CalcLine(
                            desc = "",
                            value = atan(op1.value) / radian
                        )
                    )
                }
                UnaryArgument.ARCTANH -> {
                    calcData.numberList.add(CalcLine(desc = "", value = atanh(op1.value)))
                }
                UnaryArgument.LN -> {
                    calcData.numberList.add(CalcLine(desc = "", value = ln(op1.value)))
                }
                UnaryArgument.EX -> {
                    calcData.numberList.add(CalcLine(desc = "", value = Math.E.pow(op1.value)))
                }
                UnaryArgument.LOG -> {
                    calcData.numberList.add(CalcLine(desc = "", value = log10(op1.value)))
                }
                UnaryArgument.ZX -> {
                    calcData.numberList.add(CalcLine(desc = "", value = 10.0.pow(op1.value)))
                }
                UnaryArgument.NOT -> {
                    //val numberOfBits = (ln(op1.value) / ln(2.0)).toInt() + 1
                    val value = op1.value.toLong()

                    val blockSize = getBlockSize(value)

                    // Invert the bits
                    val valueInverted = invertBits(value, blockSize)

                    calcData.numberList.add(
                        CalcLine(
                            desc = "",
                            value = valueInverted.toDouble()
                        )
                    )
                }
                UnaryArgument.COMPLEMENT -> {
                    calcData.numberList.add(
                        CalcLine(
                            desc = "",
                            value = op1.value.toLong().inv().toDouble()
                        )
                    )
                }
                UnaryArgument.ERF -> {
                    calcData.numberList.add(CalcLine(desc = "", value = erf(op1.value)))
                }
            }
        } else {
            calcData.errorMsg = context.getString(R.string.calc_invalid_args)
        }

        calcRepository.updateData(calcData)

        return argsValid
    }

    fun opBinary(op: BinaryArgument, radix: Int = 0): Boolean {
        val calcData = submitEditline(calcData.value!!, radix)
        return opBinary(calcData, op)
    }

    private fun opBinary(calcData: CalcData, op: BinaryArgument): Boolean {

        var argsValid = calcData.numberList.size > 1
        if (argsValid) {
            endEdit(calcData)

//      // Validation
//      val d = calcData.numberList.last()
//      if (op == BinaryOperation.DIV && (d == 0.0 || d.isNaN())) {
//        return
//      }

            // 2: op1
            // 1: op2
            val op2 = calcData.numberList.removeLast()
            val op1 = calcData.numberList.removeLast()

            when (op) {
                BinaryArgument.ADD -> {
                    calcData.numberList.add(op1 + op2)
                }
                BinaryArgument.SUB -> {
                    calcData.numberList.add(op1 - op2)
                }
                BinaryArgument.MULT -> {
                    calcData.numberList.add(op1 * op2)
                }
                BinaryArgument.DIV -> {
                    calcData.numberList.add(op1 / op2)
                }
                BinaryArgument.ARCTAN2 -> {
                    calcData.numberList.add(
                        CalcLine(
                            desc = "",
                            value = atan2(op1.value, op2.value) / radian
                        )
                    )
                }
                BinaryArgument.HYPOT -> {
                    calcData.numberList.add(
                        CalcLine(
                            desc = "",
                            value = Math.hypot(op1.value, op2.value)
                        )
                    )
                }
                BinaryArgument.POW -> {
                    calcData.numberList.add(
                        CalcLine(
                            desc = "",
                            value = op1.value.pow(op2.value)
                        )
                    )
                }
                BinaryArgument.MIN -> {
                    if (op1.value < op2.value) {
                        calcData.numberList.add(op1)
                    } else {
                        calcData.numberList.add(op2)
                    }
                }
                BinaryArgument.MAX -> {
                    if (op1.value > op2.value) {
                        calcData.numberList.add(op1)
                    } else {
                        calcData.numberList.add(op2)
                    }
                }
                BinaryArgument.SWAP -> {
                    calcData.numberList.add(op2)
                    calcData.numberList.add(op1)
                }
                BinaryArgument.MOD -> {
                    calcData.numberList.add(
                        CalcLine(
                            desc = "",
                            value = op1.value.toLong().rem(op2.value.toLong()).toDouble()
                        )
                    )
                }
                BinaryArgument.OVER -> {
                    calcData.numberList.add(op1)
                    calcData.numberList.add(op2)
                    // Clone element
                    calcData.numberList.add(clone(op1))
                }
                // Create matrix element.
                BinaryArgument.MATRIX -> {
                    val n = op1.value.toInt()
                    val m = op2.value.toInt()
                    if (n > 0 && n <= MATRIXMAX && m > 0 && m <= MATRIXMAX) {
                        val matrix =
                            Array(n) { r ->
                                DoubleArray(m) { c ->
                                    0.0
                                }
                            }

                        // Set unity matrix if n=m.
                        if (n == m) {
                            for (i in 0 until n) {
                                matrix[i][i] = 1.0
                            }
                        }
                        calcData.numberList.add(CalcLine(desc = "", value = Double.NaN, matrix = matrix))
                    } else {
                        calcData.errorMsg =
                            context.getString(R.string.calc_invalid_new_matrix_args, MATRIXMAX)
                        argsValid = false
                    }
                }
                // Get vector element.
                BinaryArgument.GETV -> {
                    if (op1.vector != null && op2.value.isFinite() && op1.vector!!.size > op2.value.toInt()) {
                        calcData.numberList.add(
                            CalcLine(
                                desc = "",
                                value = op1.vector!![op2.value.toInt()]
                            )
                        )
                    } else {
                        calcData.errorMsg =
                            context.getString(R.string.calc_invalid_get_vector_args)
                        argsValid = false
                    }
                }
                // Percent
                BinaryArgument.PER -> {
                    calcData.numberList.add(
                        CalcLine(
                            desc = "% ",
                            value = op1.value * op2.value / 100
                        )
                    )
                }
                // Percent change
                BinaryArgument.PERC -> {
                    calcData.numberList.add(
                        CalcLine(
                            desc = "∆% ",
                            value = (op2.value - op1.value) / op1.value * 100
                        )
                    )
                }
                // Binomial coefficient
                BinaryArgument.BINOM -> {
                    calcData.numberList.add(
                        CalcLine(
                            desc = "",
                            value = binom(op1.value.toInt(), op2.value.toInt())
                        )
                    )
                }
                BinaryArgument.AND -> {
                    calcData.numberList.add(
                        CalcLine(
                            desc = "",
                            value = op1.value.toLong().and(op2.value.toLong()).toDouble()
                        )
                    )
                }
                BinaryArgument.OR -> {
                    calcData.numberList.add(
                        CalcLine(
                            desc = "",
                            value = op1.value.toLong().or(op2.value.toLong()).toDouble()
                        )
                    )
                }
                BinaryArgument.XOR -> {
                    calcData.numberList.add(
                        CalcLine(
                            desc = "",
                            value = op1.value.toLong().xor(op2.value.toLong()).toDouble()
                        )
                    )
                }
                // Lineares Gleichungsssystem.
                BinaryArgument.SOLVE -> {
                    calcData.numberList.add(solve(op1, op2))
                }
            }
        } else {
            calcData.errorMsg = context.getString(R.string.calc_invalid_args)
        }

        calcRepository.updateData(calcData)

        return argsValid
    }

    fun opTernary(op: TernaryArgument, radix: Int = 0): Boolean {
        val calcData = submitEditline(calcData.value!!, radix)
        return opTernary(calcData, op)
    }

    private fun opTernary(calcData: CalcData, op: TernaryArgument): Boolean {
        var argsValid = calcData.numberList.size > 2

        if (argsValid) {
            endEdit(calcData)

            // 3: op3
            // 2: op2
            // 1: op1
            val op1 = calcData.numberList.removeLast()
            val op2 = calcData.numberList.removeLast()
            val op3 = calcData.numberList.removeLast()

            when (op) {
                TernaryArgument.ROT -> {
                    calcData.numberList.add(op2)
                    calcData.numberList.add(op1)
                    calcData.numberList.add(op3)
                }
                // Get matrix element.
                TernaryArgument.GETM -> {
                    if (op3.matrix != null && op2.value.isFinite() && op1.value.isFinite() && op3.matrix!!.size > op2.value.toInt() && op3.matrix!![0].size > op1.value.toInt()) {
                        calcData.numberList.add(
                            CalcLine(
                                desc = "",
                                value = op3.matrix!![op2.value.toInt()][op1.value.toInt()]
                            )
                        )
                    } else {
                        calcData.errorMsg =
                            context.getString(R.string.calc_invalid_get_matrix_args)
                        argsValid = false
                    }
                }
                // Set vector element.
                TernaryArgument.SETV -> {
                    if (op3.vector != null && op2.value.isFinite() && op1.value.isFinite() && op3.vector!!.size > op2.value.toInt()) {
                        val vector = op3.vector!!.clone()
                        vector[op2.value.toInt()] = op1.value
                        calcData.numberList.add(CalcLine(desc = "", value = Double.NaN, vector = vector))
                    } else {
                        calcData.errorMsg =
                            context.getString(R.string.calc_invalid_set_vector_args)
                        argsValid = false
                    }
                }
                TernaryArgument.ZinsMonat -> {
                    val K = op3.value // Kapital
                    val P = op2.value // Jahreszins
                    val J = op1.value // Laufzeit in Jahren
                    val p = P / 12.0 / 100.0
                    val M = p * K / (1.0 - (1.0 + p).pow(-J * 12.0))
                    calcData.numberList.add(CalcLine(desc = "Monatsrate=", value = M))
                }
            }
        } else {
            calcData.errorMsg = context.getString(R.string.calc_invalid_args)
        }

        calcRepository.updateData(calcData)

        return argsValid
    }

    fun opQuad(op: QuadArgument, radix: Int = 0): Boolean {
        val calcData = submitEditline(calcData.value!!, radix)
        return opQuad(calcData, op)
    }

    private fun opQuad(calcData: CalcData, op: QuadArgument): Boolean {
        var argsValid = calcData.numberList.size > 3

        if (argsValid) {
            endEdit(calcData)

            // 4: op4 else part
            // 3: op3 then part
            // 2: op2 conditional op
            // 1: op1 conditional op
            val op1 = calcData.numberList.removeLast()
            val op2 = calcData.numberList.removeLast()
            val op3 = calcData.numberList.removeLast()
            val op4 = calcData.numberList.removeLast()

            when (op) {
                // Set matrix element.
                QuadArgument.SETM -> {
                    if (op4.matrix != null) {
                        val n = op4.matrix!!.size
                        val m = op4.matrix!![0].size

                        if (op3.value.isFinite() && op2.value.isFinite() && op1.value.isFinite() && n > op3.value.toInt() && m > op2.value.toInt()) {
                            // clone matrix (Array of DoubleArray)
                            val matrix =
                                Array(n) { r ->
                                    DoubleArray(m) { c ->
                                        op4.matrix!![r][c]
                                    }
                                }

                            matrix[op3.value.toInt()][op2.value.toInt()] = op1.value
                            calcData.numberList.add(CalcLine(desc = "", value = Double.NaN, matrix = matrix))
                        } else {
                            calcData.errorMsg =
                                context.getString(R.string.calc_invalid_set_matrix_args)
                            argsValid = false
                        }
                    }
                }
                QuadArgument.IFEQ -> {
                    val opResult = if (op2.value == op1.value) op3 else op4
                    calcData.numberList.add(opResult)
                }
                QuadArgument.IFGE -> {
                    val opResult = if (op2.value >= op1.value) op3 else op4
                    calcData.numberList.add(opResult)
                }
                QuadArgument.IFGT -> {
                    val opResult = if (op2.value > op1.value) op3 else op4
                    calcData.numberList.add(opResult)
                }
                QuadArgument.IFLE -> {
                    val opResult = if (op2.value <= op1.value) op3 else op4
                    calcData.numberList.add(opResult)
                }
                QuadArgument.IFLT -> {
                    val opResult = if (op2.value < op1.value) op3 else op4
                    calcData.numberList.add(opResult)
                }
            }
        } else {
            calcData.errorMsg = context.getString(R.string.calc_invalid_args)
        }

        calcRepository.updateData(calcData)

        return argsValid
    }

    fun drop() {
        val calcData = calcData.value!!

        if (calcData.editMode) {
            calcData.editline = calcData.editline.dropLast(1)
        } else {
            if (calcData.numberList.size > 0) {
                calcData.numberList.removeLast()
            }
        }

        calcData.errorMsg = ""
        calcRepository.updateData(calcData)
    }

    fun enter(radix: Int = 0) {
        val calcData1 = calcData.value!!

        val calcData = if (calcData1.editMode) {
            submitEditline(calcData.value!!, radix)
        } else {
            if (calcData1.numberList.size > 0) {
                val op = calcData1.numberList.last()
                calcData1.numberList.add(op)
            }
            calcData1
        }

        endEdit(calcData)
        calcRepository.updateData(calcData)
    }

    private fun submitEditline(
        calcData: CalcData,
        radix: Int = 0,
        desc: String = ""
    ): CalcData {
        if (calcData.editMode) {

            try {
                val value = when (radix) {
                    2 -> {
                        calcData.editline.toLong(radix = 2).toDouble()
                    }
                    8 -> {
                        calcData.editline.toLong(radix = 8).toDouble()
                    }
                    10 -> {
                        calcData.editline.toLong(radix = 10).toDouble()
                    }
                    16 -> {
                        calcData.editline.toLong(radix = 16).toDouble()
                    }
                    else -> {
                        numberFormat.parse(calcData.editline)!!
                            .toDouble()
                    }
                }

                endEdit(calcData)
                calcData.numberList.add(CalcLine(desc = desc, value = value))

                return calcData
            } catch (e: Exception) {
            }
        }

//    // Remove all descriptions.
//    calcData.numberList.forEach { calcLine ->
//      calcLine.desc = ""
//    }

        aic = 0
        return calcData
    }

    private fun endEdit(calcData: CalcData) {
        calcData.editMode = false
        calcData.editline = ""
        calcData.errorMsg = ""
    }

    fun getLines(): Int {
        return calcData.value!!.numberList.size
    }

    fun updateData(radix: Int = 0) {
        if (calcData.value != null) {
            val calcData = submitEditline(calcData.value!!, radix)
            calcRepository.updateData(calcData)
        } else {
            calcRepository.updateData()
        }
    }

    fun updateData(data: CalcData) {

//    data.numberList.removeIf { calcLine ->
//      calcLine.desc.isEmpty() && calcLine.value.isNaN()
//    }

        calcRepository.updateData(data)
    }
}
