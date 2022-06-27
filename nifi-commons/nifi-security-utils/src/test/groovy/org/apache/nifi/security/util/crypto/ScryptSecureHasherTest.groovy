/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.security.util.crypto

import org.bouncycastle.util.encoders.Hex
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

import java.nio.charset.StandardCharsets

import static org.junit.jupiter.api.Assertions.assertThrows

class ScryptSecureHasherTest {

    @Test
    void testShouldBeDeterministicWithStaticSalt() {
        // Arrange
        int n = 1024
        int r = 8
        int p = 2
        int dkLength = 32

        int testIterations = 10
        byte[] inputBytes = "This is a sensitive value".bytes

        final String EXPECTED_HASH_HEX = "a67fd2f4b3aa577b8ecdb682e60b4451a84611dcbbc534bce17616056ef8965d"

        ScryptSecureHasher scryptSH = new ScryptSecureHasher(n, r, p, dkLength)

        def results = []

        // Act
        testIterations.times { int i ->
            byte[] hash = scryptSH.hashRaw(inputBytes)
            String hashHex = new String(Hex.encode(hash))
            results << hashHex
        }

        // Assert
        assert results.every { it == EXPECTED_HASH_HEX }
    }

    @Test
    void testShouldBeDifferentWithRandomSalt() {
        // Arrange
        int n = 1024
        int r = 8
        int p = 2
        int dkLength = 128

        int testIterations = 10
        byte[] inputBytes = "This is a sensitive value".bytes

        final String EXPECTED_HASH_HEX = "a67fd2f4b3aa577b8ecdb682e60b4451"

        ScryptSecureHasher scryptSH = new ScryptSecureHasher(n, r, p, dkLength, 16)

        def results = []

        // Act
        testIterations.times { int i ->
            byte[] hash = scryptSH.hashRaw(inputBytes)
            String hashHex = new String(Hex.encode(hash))
            results << hashHex
        }

        // Assert
        assert results.unique().size() == results.size()
        assert results.every { it != EXPECTED_HASH_HEX }
    }

    @Test
    void testShouldHandleArbitrarySalt() {
        // Arrange
        int n = 1024
        int r = 8
        int p = 2
        int dkLength = 32

        def input = "This is a sensitive value"
        byte[] inputBytes = input.bytes

        final String EXPECTED_HASH_HEX = "a67fd2f4b3aa577b8ecdb682e60b4451a84611dcbbc534bce17616056ef8965d"
        final String EXPECTED_HASH_BASE64 = "pn/S9LOqV3uOzbaC5gtEUahGEdy7xTS84XYWBW74ll0"
        final byte[] EXPECTED_HASH_BYTES = Hex.decode(EXPECTED_HASH_HEX)

        // Static salt instance
        ScryptSecureHasher staticSaltHasher = new ScryptSecureHasher(n, r, p, dkLength)
        ScryptSecureHasher arbitrarySaltHasher = new ScryptSecureHasher(n, r, p, dkLength, 16)

        final byte[] STATIC_SALT = AbstractSecureHasher.STATIC_SALT
        final String DIFFERENT_STATIC_SALT = "Diff Static Salt"

        // Act
        byte[] staticSaltHash = staticSaltHasher.hashRaw(inputBytes)
        byte[] arbitrarySaltHash = arbitrarySaltHasher.hashRaw(inputBytes, STATIC_SALT)
        byte[] differentArbitrarySaltHash = arbitrarySaltHasher.hashRaw(inputBytes, DIFFERENT_STATIC_SALT.getBytes(StandardCharsets.UTF_8))
        byte[] differentSaltHash = arbitrarySaltHasher.hashRaw(inputBytes)

        String staticSaltHashHex = staticSaltHasher.hashHex(input)
        String arbitrarySaltHashHex = arbitrarySaltHasher.hashHex(input, new String(STATIC_SALT, StandardCharsets.UTF_8))
        String differentArbitrarySaltHashHex = arbitrarySaltHasher.hashHex(input, DIFFERENT_STATIC_SALT)
        String differentSaltHashHex = arbitrarySaltHasher.hashHex(input)

        String staticSaltHashBase64 = staticSaltHasher.hashBase64(input)
        String arbitrarySaltHashBase64 = arbitrarySaltHasher.hashBase64(input, new String(STATIC_SALT, StandardCharsets.UTF_8))
        String differentArbitrarySaltHashBase64 = arbitrarySaltHasher.hashBase64(input, DIFFERENT_STATIC_SALT)
        String differentSaltHashBase64 = arbitrarySaltHasher.hashBase64(input)

        // Assert
        assert staticSaltHash == EXPECTED_HASH_BYTES
        assert arbitrarySaltHash == EXPECTED_HASH_BYTES
        assert differentArbitrarySaltHash != EXPECTED_HASH_BYTES
        assert differentSaltHash != EXPECTED_HASH_BYTES

        assert staticSaltHashHex == EXPECTED_HASH_HEX
        assert arbitrarySaltHashHex == EXPECTED_HASH_HEX
        assert differentArbitrarySaltHashHex != EXPECTED_HASH_HEX
        assert differentSaltHashHex != EXPECTED_HASH_HEX

        assert staticSaltHashBase64 == EXPECTED_HASH_BASE64
        assert arbitrarySaltHashBase64 == EXPECTED_HASH_BASE64
        assert differentArbitrarySaltHashBase64 != EXPECTED_HASH_BASE64
        assert differentSaltHashBase64 != EXPECTED_HASH_BASE64
    }

    @Test
    void testShouldValidateArbitrarySalt() {
        // Arrange
        int n = 1024
        int r = 8
        int p = 2
        int dkLength = 32

        def input = "This is a sensitive value"
        byte[] inputBytes = input.bytes

        // Static salt instance
        ScryptSecureHasher secureHasher = new ScryptSecureHasher(n, r, p, dkLength, 16)
        final byte[] STATIC_SALT = "bad_sal".bytes

        assertThrows(IllegalArgumentException.class, { -> new ScryptSecureHasher(n, r, p, dkLength, 7) })
        assertThrows(RuntimeException.class, { -> secureHasher.hashRaw(inputBytes, STATIC_SALT) })
        assertThrows(RuntimeException.class, { -> secureHasher.hashHex(input, new String(STATIC_SALT, StandardCharsets.UTF_8)) })
        assertThrows(RuntimeException.class, { -> secureHasher.hashBase64(input, new String(STATIC_SALT, StandardCharsets.UTF_8)) })
    }

    @Test
    void testShouldFormatHex() {
        // Arrange
        String input = "This is a sensitive value"

        final String EXPECTED_HASH_HEX = "6a9c827815fe0718af5e336811fc78dd719c8d9505e015283239b9bf1d24ee71"

        SecureHasher scryptSH = new ScryptSecureHasher()

        // Act
        String hashHex = scryptSH.hashHex(input)

        // Assert
        assert hashHex == EXPECTED_HASH_HEX
    }

    @Test
    void testShouldFormatBase64() {
        // Arrange
        String input = "This is a sensitive value"

        final String EXPECTED_HASH_BASE64 = "apyCeBX+BxivXjNoEfx43XGcjZUF4BUoMjm5vx0k7nE"

        SecureHasher scryptSH = new ScryptSecureHasher()

        // Act
        String hashB64 = scryptSH.hashBase64(input)

        // Assert
        assert hashB64 == EXPECTED_HASH_BASE64
    }

    @Test
    void testShouldHandleNullInput() {
        // Arrange
        List<String> inputs = [null, ""]

        final String EXPECTED_HASH_HEX = ""
        final String EXPECTED_HASH_BASE64 = ""

        ScryptSecureHasher scryptSH = new ScryptSecureHasher()

        def hexResults = []
        def B64Results = []

        // Act
        inputs.each { String input ->
            String hashHex = scryptSH.hashHex(input)
            hexResults << hashHex

            String hashB64 = scryptSH.hashBase64(input)
            B64Results << hashB64
        }

        // Assert
        assert hexResults.every { it == EXPECTED_HASH_HEX }
        assert B64Results.every { it == EXPECTED_HASH_BASE64 }
    }

    /**
     * This test can have the minimum time threshold updated to determine if the performance
     * is still sufficient compared to the existing threat model.
     */
    @EnabledIfSystemProperty(named = "nifi.test.performance", matches = "true")
    @Test
    void testDefaultCostParamsShouldBeSufficient() {
        // Arrange
        int testIterations = 100
        byte[] inputBytes = "This is a sensitive value".bytes

        ScryptSecureHasher scryptSH = new ScryptSecureHasher()

        def results = []
        def resultDurations = []

        // Act
        testIterations.times { int i ->
            long startNanos = System.nanoTime()
            byte[] hash = scryptSH.hashRaw(inputBytes)
            long endNanos = System.nanoTime()
            long durationNanos = endNanos - startNanos

            String hashHex = Hex.encode(hash)

            results << hashHex
            resultDurations << durationNanos
        }

        // Assert
        final long MIN_DURATION_NANOS = 75_000_000 // 75 ms
        assert resultDurations.min() > MIN_DURATION_NANOS
        assert resultDurations.sum() / testIterations > MIN_DURATION_NANOS
    }

    @Test
    void testShouldVerifyRBoundary() throws Exception {
        // Arrange
        final int r = 32

        // Act
        boolean valid = ScryptSecureHasher.isRValid(r)

        // Assert
        assert valid
    }

    @Test
    void testShouldFailRBoundary() throws Exception {
        // Arrange
        def rValues = [-8, 0, 2147483647]

        // Act
        def results = rValues.collect { rValue ->
            def isValid = ScryptSecureHasher.isRValid(rValue)
            [rValue, isValid]
        }

        // Assert
        results.each { rValue, isRValid ->
            assert !isRValid
        }
    }

    @Test
    void testShouldVerifyNBoundary() throws Exception {
        // Arrange
        final Integer n = 16385
        final int r = 8

        // Act
        boolean valid = ScryptSecureHasher.isNValid(n, r)

        // Assert
        assert valid
    }

    @Test
    void testShouldFailNBoundary() throws Exception {
        // Arrange
        Map costParameters = [(-8): 8, 0: 32]

        // Act
        def results = costParameters.collect { n, p ->
            def isValid = ScryptSecureHasher.isNValid(n, p)
            [n, isValid]
        }

        // Assert
        results.each { n, isNValid ->
            assert !isNValid
        }
    }

    @Test
    void testShouldVerifyPBoundary() throws Exception {
        // Arrange
        final List<Integer> ps = [1, 8, 1024]
        final List<Integer> rs = [8, 1024, 4096]

        // Act
        def pResults = ps.collectEntries { int p ->
            def rResults = rs.collectEntries { int r ->
                boolean valid = ScryptSecureHasher.isPValid(p, r)
                [r, valid]
            }
            [p, rResults]
        }

        // Assert
        pResults.each { p, rResult ->
            assert rResult.every { r, isValid -> isValid }
        }
    }

    @Test
    void testShouldFailIfPBoundaryExceeded() throws Exception {
        // Arrange
        final List<Integer> ps = [4096 * 64, 1024 * 1024]
        final List<Integer> rs = [4096, 1024 * 1024]

        // Act
        def pResults = ps.collectEntries { int p ->
            def rResults = rs.collectEntries { int r ->
                boolean valid = ScryptSecureHasher.isPValid(p, r)
                [r, valid]
            }
            [p, rResults]
        }

        // Assert
        pResults.each { p, rResult ->
            assert rResult.every { r, isValid -> !isValid }
        }
    }

    @Test
    void testShouldVerifyDKLengthBoundary() throws Exception {
        // Arrange
        final Integer dkLength = 64

        // Act
        boolean valid = ScryptSecureHasher.isDKLengthValid(dkLength)

        // Assert
        assert valid
    }

    @Test
    void testShouldFailDKLengthBoundary() throws Exception {
        // Arrange
        def dKLengths = [-8, 0, 2147483647]

        // Act
        def results = dKLengths.collect { dKLength ->
            def isValid = ScryptSecureHasher.isDKLengthValid(dKLength)
            [dKLength, isValid]
        }

        // Assert
        results.each { dKLength, isDKLengthValid ->
            assert !isDKLengthValid
        }
    }

    @Test
    void testShouldVerifySaltLengthBoundary() throws Exception {
        // Arrange
        def saltLengths = [0, 64]

        // Act
        def results = saltLengths.collect { saltLength ->
            def isValid = new ScryptSecureHasher().isSaltLengthValid(saltLength)
            [saltLength, isValid]
        }

        // Assert
        results.each { saltLength, isSaltLengthValid ->
            assert { it == isSaltLengthValid }
        }
    }

    @Test
    void testShouldFailSaltLengthBoundary() throws Exception {
        // Arrange
        def saltLengths = [-8, 1, 2147483647]

        // Act
        def results = saltLengths.collect { saltLength ->
            def isValid = new ScryptSecureHasher().isSaltLengthValid(saltLength)
            [saltLength, isValid]
        }

        // Assert
        results.each { saltLength, isSaltLengthValid ->
            assert !isSaltLengthValid
        }
    }

}
