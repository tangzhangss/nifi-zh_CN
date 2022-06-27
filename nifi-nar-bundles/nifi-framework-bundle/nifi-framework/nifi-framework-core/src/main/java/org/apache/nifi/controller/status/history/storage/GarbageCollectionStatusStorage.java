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
package org.apache.nifi.controller.status.history.storage;

import org.apache.nifi.controller.status.history.GarbageCollectionHistory;
import org.apache.nifi.controller.status.history.GarbageCollectionStatus;

import java.time.Instant;

/**
 * Readable status storage for garbage collection status entries.
 */
public interface GarbageCollectionStatusStorage extends StatusStorage<GarbageCollectionStatus> {

    /**
     * Returns with the status history of the garbage collection for the specified time range.
     *
     * @param start Start date of the history, inclusive.
     * @param end End date of the history, inclusive.
     *
     * @return Status history.
     */
    GarbageCollectionHistory read(Instant start, Instant end);
}
