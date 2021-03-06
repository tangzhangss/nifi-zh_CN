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
package org.apache.nifi.processors.beats.frame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.bind.DatatypeConverter;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;


public class TestBeatsEncoder {
    private BeatsEncoder encoder;


    @BeforeEach
    public void setup() {
        this.encoder = new BeatsEncoder();
    }

    @Test
    public void testEncode() {
        BeatsFrame frame = new BeatsFrame.Builder()
            .version((byte) 0x31)
            .frameType((byte) 0x41)
            .payload(ByteBuffer.allocate(4).putInt(123).array())
            .build();

        byte[] encoded = encoder.encode(frame);

        assertArrayEquals(DatatypeConverter.parseHexBinary("31410000007B"), encoded);
    }
}