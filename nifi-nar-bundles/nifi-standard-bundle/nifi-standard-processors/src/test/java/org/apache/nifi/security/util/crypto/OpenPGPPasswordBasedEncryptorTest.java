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
package org.apache.nifi.security.util.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.apache.nifi.processor.io.StreamCallback;
import org.apache.nifi.security.util.EncryptionMethod;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class OpenPGPPasswordBasedEncryptorTest {
    private static final String FILENAME = OpenPGPPasswordBasedEncryptorTest.class.getSimpleName();

    private static final int CIPHER = PGPEncryptedData.AES_128;

    private static final byte[] PLAINTEXT = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8};

    private static final String PASSWORD = OpenPGPPasswordBasedEncryptorTest.class.getName();

    @Test
    public void testEncryptDecrypt() throws Exception {
        final ByteArrayInputStream plainStream = new ByteArrayInputStream(PLAINTEXT);

        final OpenPGPPasswordBasedEncryptor encryptor = new OpenPGPPasswordBasedEncryptor(EncryptionMethod.PGP.getAlgorithm(),
                CIPHER, EncryptionMethod.PGP.getProvider(), PASSWORD.toCharArray(), FILENAME);

        final StreamCallback encryptionCallback = encryptor.getEncryptionCallback();
        final StreamCallback decryptionCallback = encryptor.getDecryptionCallback();

        final ByteArrayOutputStream encryptedStream = new ByteArrayOutputStream();
        encryptionCallback.process(plainStream, encryptedStream);

        final InputStream encryptedInputStream = new ByteArrayInputStream(encryptedStream.toByteArray());
        final ByteArrayOutputStream decryptedStream = new ByteArrayOutputStream();
        decryptionCallback.process(encryptedInputStream, decryptedStream);

        byte[] decryptedBytes = decryptedStream.toByteArray();
        assertArrayEquals(PLAINTEXT, decryptedBytes);
    }
}
