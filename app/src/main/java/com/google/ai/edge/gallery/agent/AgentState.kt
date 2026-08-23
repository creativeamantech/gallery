/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ai.edge.gallery.agent

import com.google.ai.edge.gallery.model.provider.ModelMessage

/** Represents the current execution status of the Agent Runtime. */
enum class AgentStatus {
    IDLE,
    RUNNING,
    WAITING_FOR_CAPABILITY,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Encapsulates the runtime context and active state of an ongoing agent task.
 * 
 * @property taskId The unique task ID.
 * @property status The current lifecycle status.
 * @property history The mutable conversation history of the task.
 * @property iterationCount The number of full reasoning loops completed.
 * @property toolCallCount The total number of capabilities invoked during this session.
 * @property retryCount The number of retries attempted for failures.
 */
class AgentState(
    val taskId: String,
    var status: AgentStatus = AgentStatus.IDLE,
    val history: MutableList<ModelMessage> = mutableListOf(),
    var iterationCount: Int = 0,
    var toolCallCount: Int = 0,
    var retryCount: Int = 0,
    val finalOutput: StringBuilder = StringBuilder()
)
