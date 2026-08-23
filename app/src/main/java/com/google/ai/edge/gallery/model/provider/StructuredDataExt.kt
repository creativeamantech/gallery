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
package com.google.ai.edge.gallery.model.provider

/** Extension functions for seamlessly converting between native Map/List types and StructuredData. */

fun StructuredData.toNative(): Any? = when (this) {
    is StructuredData.Null -> null
    is StructuredData.StringValue -> this.value
    is StructuredData.NumberValue -> this.value
    is StructuredData.BooleanValue -> this.value
    is StructuredData.ArrayValue -> this.items.map { it.toNative() }
    is StructuredData.ObjectValue -> this.fields.mapValues { it.value.toNative() }
}

fun Any?.toStructuredData(): StructuredData = when (this) {
    null -> StructuredData.Null
    is String -> StructuredData.StringValue(this)
    is Number -> StructuredData.NumberValue(this.toDouble())
    is Boolean -> StructuredData.BooleanValue(this)
    is Map<*, *> -> StructuredData.ObjectValue(
        this.entries.associate { it.key.toString() to it.value.toStructuredData() }
    )
    is List<*> -> StructuredData.ArrayValue(this.map { it.toStructuredData() })
    is Array<*> -> StructuredData.ArrayValue(this.map { it.toStructuredData() })
    else -> StructuredData.StringValue(this.toString()) // Fallback
}
