package com.example.data.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MoshiPart(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class MoshiContent(
    val parts: List<MoshiPart>
)

@JsonClass(generateAdapter = true)
data class MoshiResponseFormatText(
    val mimeType: String
)

@JsonClass(generateAdapter = true)
data class MoshiResponseFormat(
    val text: MoshiResponseFormatText? = null
)

@JsonClass(generateAdapter = true)
data class MoshiGenerationConfig(
    val responseFormat: MoshiResponseFormat? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class MoshiGenerateContentRequest(
    val contents: List<MoshiContent>,
    val generationConfig: MoshiGenerationConfig? = null,
    val systemInstruction: MoshiContent? = null
)

@JsonClass(generateAdapter = true)
data class MoshiCandidate(
    val content: MoshiContent
)

@JsonClass(generateAdapter = true)
data class MoshiGenerateContentResponse(
    val candidates: List<MoshiCandidate>? = null
)
