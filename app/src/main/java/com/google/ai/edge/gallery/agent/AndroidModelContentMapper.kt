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

import android.graphics.Bitmap
import com.google.ai.edge.gallery.model.provider.ModelContent
import java.io.ByteArrayOutputStream

/** Utility for converting Android-specific Agent Attachments into platform-neutral ModelContent. */
object AndroidModelContentMapper {
    fun map(attachment: Attachment): ModelContent? {
        return when (attachment) {
            is Attachment.ImageBitmap -> {
                val stream = ByteArrayOutputStream()
                attachment.bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                ModelContent.Image(stream.toByteArray(), "image/jpeg")
            }
            is Attachment.AudioBytes -> {
                ModelContent.Audio(attachment.audioBytes, "audio/wav")
            }
            else -> null // Other formats (like URIs or plain Files) should be handled via a loader if supported.
        }
    }
}
