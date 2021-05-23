/*
 * Copyright (C) 2021
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

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.sqrt

@RunWith(AndroidJUnit4::class)
class CalcTest {

    @Test
    @Throws(Exception::class)
    fun fracTest() {

        assertEquals(Pair(5, 19), frac(5.0 / 19.0))
        assertEquals(Pair(104348, 33215), frac(3.14159265359))
        assertEquals(Pair(37, 61), frac(0.606557377049))
        assertEquals(Pair(2, 1), frac(2.0))
        assertEquals(Pair(1, 3), frac(0.33333333))

        assertEquals(Pair(0, 1), frac(0.0))

        assertEquals(Pair(-2, 1), frac(-2.0))
        assertEquals(Pair(-5, 19), frac(-5.0 / 19.0))
        assertEquals(Pair(-104348, 33215), frac(-3.14159265359))
        assertEquals(Pair(-37, 61), frac(-0.606557377049))
        assertEquals(Pair(-1, 3), frac(-0.33333333))

        // null result

        // infinity
        assertEquals(Pair(null, 1), frac(1 / 0.0))
        // NaN
        assertEquals(Pair(null, 1), frac(sqrt(-1.0)))
        // large number
        assertEquals(Pair(null, 1), frac(100000000000000.0))
    }

    @Test
    @Throws(Exception::class)
    fun bitTest() {

        assertEquals(8, getBlockSize(248))
        assertEquals(8, getBlockSize(127))
        assertEquals(8, getBlockSize(-128))
        assertEquals(16, getBlockSize(-248))

        assertEquals(16, getBlockSize(32767))
        assertEquals(16, getBlockSize(-32767))

        assertEquals(16, getBlockSize(32768))
        assertEquals(16, getBlockSize(-32768))

        assertEquals(16, getBlockSize(65535))
        assertEquals(32, getBlockSize(-65535))

        assertEquals(32, getBlockSize(65536))
        assertEquals(32, getBlockSize(-65536))

    }

    @Test
    @Throws(Exception::class)
    fun displayStringTest() {

        val valueA = 1L
        val s1 = getDisplayString(valueA, 2)
        assertEquals("00000001", s1)

        val blockSizeA = getBlockSize(valueA)
        val valueInvertedA = invertBits(valueA, blockSizeA)

        val s2 = getDisplayString(valueInvertedA, 2)
        assertEquals("11111110", s2)

        val valueB = 3735928559L
        val s3 = getDisplayString(valueB, 2)
        assertEquals("11011110101011011011111011101111", s1)

        val blockSizeB = getBlockSize(valueB)
        val valueInvertedB = invertBits(valueB, blockSizeB)

        val s4 = getDisplayString(valueInvertedB, 2)
        assertEquals("1111111111111111111111111111111100100001010100100100000100010000", s4)

    }

}
