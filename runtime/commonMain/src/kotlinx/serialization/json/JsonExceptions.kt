/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*

/**
 * Generic exception indicating a problem with JSON serialization.
 */
public open class JsonException(message: String) : SerializationException(message)

/**
 * Thrown when [Json] has failed to parse the given JSON string or deserialize it to a target class.
 */
public class JsonDecodingException(offset: Int, message: String) :
    JsonException("Unexpected JSON token at offset $offset: $message")

/**
 * Thrown when [Json] has failed to create a JSON string from the given value.
 */
public class JsonEncodingException(message: String) : JsonException(message)

internal fun JsonDecodingException(offset: Int, message: String, input: String) =
    JsonDecodingException(offset, "$message.\n JSON input: $input")

internal fun InvalidFloatingPoint(value: Number, type: String, output: String) = JsonEncodingException(
    "'$value' is not a valid '$type' as per JSON specification. " +
            "You can enable 'serializeSpecialFloatingPointValues' property to serialize such values\n" +
            "Current output: $output"
)

internal fun InvalidFloatingPoint(value: Number, key: String, type: String, output: String) = JsonEncodingException(
    "'$value' with key '$key' is not a valid $type as per JSON specification. " +
            "You can enable 'serializeSpecialFloatingPointValues' property to serialize such values.\n" +
            "Current output: $output"
)

internal fun UnknownKeyException(key: String, input: String) = JsonDecodingException(
    -1,
    "JSON encountered unknown key: '$key'. You can enable 'ignoreUnknownKeys' property to ignore unknown keys.\n" +
            "JSON input: $input"
)

internal fun InvalidKeyKindException(keyDescriptor: SerialDescriptor) = JsonEncodingException(
    "Value of type '${keyDescriptor.serialName}' can't be used in JSON as a key in the map. " +
            "It should have either primitive or enum kind, but its kind is '${keyDescriptor.kind}.'\n" +
            "You can convert such maps to arrays [key1, value1, key2, value2,...] using 'allowStructuredMapKeys' property in JsonConfiguration"
)
