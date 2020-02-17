/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:ContextualSerialization(JsonCustomSerializersTest.B::class)

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.modules.*
import kotlin.test.*

class JsonCustomSerializersTest : JsonTestBase() {
    
    @Serializable
    data class A(@Id(1) val b: B)

    data class B(@Id(1) val value: Int)

    object BSerializer : KSerializer<B> {
        override val descriptor: SerialDescriptor = PrimitiveDescriptor("B", PrimitiveKind.INT)
        override fun serialize(encoder: Encoder, value: B) {
            encoder.encodeInt(value.value)
        }

        override fun deserialize(decoder: Decoder): B {
            return B(decoder.decodeInt())
        }
    }

    @Serializable
    data class BList(@Id(1) val bs: List<B>)

    @Serializable
    data class C(@Id(1) val a: Int = 31, @Id(2) val b: Int = 42) {
        @Serializer(forClass = C::class)
        companion object: KSerializer<C> {
            override fun serialize(encoder: Encoder, value: C) {
                val elemOutput = encoder.beginStructure(descriptor)
                elemOutput.encodeIntElement(descriptor, 1, value.b)
                if (value.a != 31) elemOutput.encodeIntElement(descriptor, 0, value.a)
                elemOutput.endStructure(descriptor)
            }
        }
    }

    @Serializable
    data class CList1(@Id(1) val c: List<C>)

    @Serializable
    data class CList2(@Id(1) val d: Int = 5, @Id(2) val c: List<C>) {
        @Serializer(forClass = CList2::class)
        companion object: KSerializer<CList2> {
            override fun serialize(encoder: Encoder, value: CList2) {
                val elemOutput = encoder.beginStructure(descriptor)
                elemOutput.encodeSerializableElement(descriptor, 1, C.list, value.c)
                if (value.d != 5) elemOutput.encodeIntElement(descriptor, 0, value.d)
                elemOutput.endStructure(descriptor)
            }
        }
    }

    @Serializable
    data class CList3(@Id(1) val e: List<C> = emptyList(), @Id(2) val f: Int) {
        @Serializer(forClass = CList3::class)
        companion object: KSerializer<CList3> {
            override fun serialize(encoder: Encoder, value: CList3) {
                val elemOutput = encoder.beginStructure(descriptor)
                if (value.e.isNotEmpty()) elemOutput.encodeSerializableElement(descriptor, 0, C.list, value.e)
                elemOutput.encodeIntElement(descriptor, 1, value.f)
                elemOutput.endStructure(descriptor)
            }
        }
    }

    @Serializable
    data class CList4(@Id(1) val g: List<C> = emptyList(), @Id(2) val h: Int) {
        @Serializer(forClass = CList4::class)
        companion object: KSerializer<CList4> {
            override fun serialize(encoder: Encoder, value: CList4) {
                val elemOutput = encoder.beginStructure(descriptor)
                elemOutput.encodeIntElement(descriptor, 1, value.h)
                if (value.g.isNotEmpty()) elemOutput.encodeSerializableElement(descriptor, 0, C.list, value.g)
                elemOutput.endStructure(descriptor)
            }
        }
    }

    @Serializable
    data class CList5(@Id(1) val g: List<Int> = emptyList(), @Id(2) val h: Int) {
        @Serializer(forClass = CList5::class)
        companion object: KSerializer<CList5> {
            override fun serialize(encoder: Encoder, value: CList5) {
                val elemOutput = encoder.beginStructure(descriptor)
                elemOutput.encodeIntElement(descriptor, 1, value.h)
                if (value.g.isNotEmpty()) elemOutput.encodeSerializableElement(
                    descriptor, 0, Int.serializer().list,
                    value.g
                )
                elemOutput.endStructure(descriptor)
            }
        }
    }

    private val moduleWithB = serializersModuleOf(B::class, BSerializer)

    private fun createJsonWithB() = Json { unquoted = true; serialModule = moduleWithB }

    @Test
    fun testWriteCustom() = parametrizedTest { useStreaming ->
        val a = A(B(2))
        val j = createJsonWithB()
        val s = j.stringify(a, useStreaming)
        assertEquals("{b:2}", s)
    }

    @Test
    fun testReadCustom() = parametrizedTest { useStreaming ->
        val a = A(B(2))
        val j = createJsonWithB()
        val s = j.parse<A>("{b:2}", useStreaming)
        assertEquals(a, s)
    }

    @Test
    fun testWriteCustomList() = parametrizedTest { useStreaming ->
        val obj = BList(listOf(B(1), B(2), B(3)))
        val j = createJsonWithB()
        val s = j.stringify(obj, useStreaming)
        assertEquals("{bs:[1,2,3]}", s)
    }

    @Test
    fun testReadCustomList() = parametrizedTest { useStreaming ->
        val obj = BList(listOf(B(1), B(2), B(3)))
        val j = createJsonWithB()
        val bs = j.parse<BList>("{bs:[1,2,3]}", useStreaming)
        assertEquals(obj, bs)
    }

    @Test
    fun testWriteCustomListRootLevel() = parametrizedTest { useStreaming ->
        val obj = listOf(B(1), B(2), B(3))
        val j = createJsonWithB()
        val s = j.stringify(BSerializer.list, obj, useStreaming)
        assertEquals("[1,2,3]", s)
    }

    @Test
    fun testReadCustomListRootLevel() = parametrizedTest { useStreaming ->
        val obj = listOf(B(1), B(2), B(3))
        val j = createJsonWithB()
        val bs = j.parse(BSerializer.list, "[1,2,3]", useStreaming)
        assertEquals(obj, bs)
    }

    @Test
    fun testWriteCustomInvertedOrder() = parametrizedTest { useStreaming ->
        val obj = C(1, 2)
        val s = unquoted.stringify(obj, useStreaming)
        assertEquals("{b:2,a:1}", s)
    }

    @Test
    fun testWriteCustomOmitDefault() = parametrizedTest { useStreaming ->
        val obj = C(b = 2)
        val s = unquoted.stringify(obj, useStreaming)
        assertEquals("{b:2}", s)
    }

    @Test
    fun testReadCustomInvertedOrder() = parametrizedTest { useStreaming ->
        val obj = C(1, 2)
        val s = unquoted.parse<C>("{b:2,a:1}", useStreaming)
        assertEquals(obj, s)
    }

    @Test
    fun testReadCustomOmitDefault() = parametrizedTest { useStreaming ->
        val obj = C(b = 2)
        val j = Json { unquoted = true }
        val s = j.parse<C>("{b:2}", useStreaming)
        assertEquals(obj, s)
    }

    @Test
    fun testWriteListOfOptional() = parametrizedTest { useStreaming ->
        val obj = listOf(C(a = 1), C(b = 2), C(3, 4))
        val s = unquoted.stringify(C.list, obj, useStreaming)
        assertEquals("[{b:42,a:1},{b:2},{b:4,a:3}]", s)
    }

    @Test
    fun testReadListOfOptional() = parametrizedTest { useStreaming ->
        val obj = listOf(C(a = 1), C(b = 2), C(3, 4))
        val j = "[{b:42,a:1},{b:2},{b:4,a:3}]"
        val s = unquoted.parse(C.list, j, useStreaming)
        assertEquals(obj, s)
    }

    @Test
    fun testWriteOptionalList1() = parametrizedTest { useStreaming ->
        val obj = CList1(listOf(C(a = 1), C(b = 2), C(3, 4)))
        val s = unquoted.stringify(obj, useStreaming)
        assertEquals("{c:[{b:42,a:1},{b:2},{b:4,a:3}]}", s)
    }

    @Test
    fun testWriteOptionalList1Quoted() = parametrizedTest { useStreaming ->
        val obj = CList1(listOf(C(a = 1), C(b = 2), C(3, 4)))
        val s = Json { unquoted = false }.stringify(obj, useStreaming)
        assertEquals("""{"c":[{"b":42,"a":1},{"b":2},{"b":4,"a":3}]}""", s)
    }

    @Test
    fun testReadOptionalList1() = parametrizedTest { useStreaming ->
        val obj = CList1(listOf(C(a = 1), C(b = 2), C(3, 4)))
        val j = "{c:[{b:42,a:1},{b:2},{b:4,a:3}]}"
        assertEquals(obj, unquoted.parse(j, useStreaming))
    }

    @Test
    fun testWriteOptionalList2a() = parametrizedTest { useStreaming ->
        val obj = CList2(7, listOf(C(a = 5), C(b = 6), C(7, 8)))
        val s = unquoted.stringify(obj, useStreaming)
        assertEquals("{c:[{b:42,a:5},{b:6},{b:8,a:7}],d:7}", s)
    }

    @Test
    fun testReadOptionalList2a() = parametrizedTest { useStreaming ->
        val obj = CList2(7, listOf(C(a = 5), C(b = 6), C(7, 8)))
        val j = "{c:[{b:42,a:5},{b:6},{b:8,a:7}],d:7}"
        assertEquals(obj, unquoted.parse(j, useStreaming))
    }

    @Test
    fun testWriteOptionalList2b() = parametrizedTest { useStreaming ->
        val obj = CList2(c = listOf(C(a = 5), C(b = 6), C(7, 8)))
        val s = unquoted.stringify(obj, useStreaming)
        assertEquals("{c:[{b:42,a:5},{b:6},{b:8,a:7}]}", s)
    }

    @Test
    fun testReadOptionalList2b() = parametrizedTest { useStreaming ->
        val obj = CList2(c = listOf(C(a = 5), C(b = 6), C(7, 8)))
        val j = "{c:[{b:42,a:5},{b:6},{b:8,a:7}]}"
        assertEquals(obj, unquoted.parse(j, useStreaming))
    }

    @Test
    fun testWriteOptionalList3a() = parametrizedTest { useStreaming ->
        val obj = CList3(listOf(C(a = 1), C(b = 2), C(3, 4)), 99)
        val s = unquoted.stringify(obj, useStreaming)
        assertEquals("{e:[{b:42,a:1},{b:2},{b:4,a:3}],f:99}", s)
    }

    @Test
    fun testReadOptionalList3a() = parametrizedTest { useStreaming ->
        val obj = CList3(listOf(C(a = 1), C(b = 2), C(3, 4)), 99)
        val j = "{e:[{b:42,a:1},{b:2},{b:4,a:3}],f:99}"
        assertEquals(obj, unquoted.parse(j, useStreaming))
    }

    @Test
    fun testWriteOptionalList3b() = parametrizedTest { useStreaming ->
        val obj = CList3(f=99)
        val s = unquoted.stringify(obj, useStreaming)
        assertEquals("{f:99}", s)
    }

    @Test
    fun testReadOptionalList3b() = parametrizedTest { useStreaming ->
        val obj = CList3(f=99)
        val j = "{f:99}"
        assertEquals(obj, unquoted.parse(j, useStreaming))
    }

    @Test
    fun testWriteOptionalList4a() = parametrizedTest { useStreaming ->
        val obj = CList4(listOf(C(a = 1), C(b = 2), C(3, 4)), 54)
        val s = unquoted.stringify(obj, useStreaming)
        assertEquals("{h:54,g:[{b:42,a:1},{b:2},{b:4,a:3}]}", s)
    }

    @Test
    fun testReadOptionalList4a() = parametrizedTest { useStreaming ->
        val obj = CList4(listOf(C(a = 1), C(b = 2), C(3, 4)), 54)
        val j = "{h:54,g:[{b:42,a:1},{b:2},{b:4,a:3}]}"
        assertEquals(obj, unquoted.parse(j, useStreaming))
    }

    @Test
    fun testWriteOptionalList4b() = parametrizedTest { useStreaming ->
        val obj = CList4(h=97)
        val j = "{h:97}"
        val s = unquoted.stringify(obj, useStreaming)
        assertEquals(j, s)
    }

    @Test
    fun testReadOptionalList4b() = parametrizedTest { useStreaming ->
        val obj = CList4(h=97)
        val j = "{h:97}"
        assertEquals(obj, unquoted.parse(j, useStreaming))
    }

    @Test
    fun testWriteOptionalList5a() = parametrizedTest { useStreaming ->
        val obj = CList5(listOf(9,8,7,6,5), 5)
        val s = unquoted.stringify(obj, useStreaming)
        assertEquals("{h:5,g:[9,8,7,6,5]}", s)
    }

    @Test
    fun testReadOptionalList5a() = parametrizedTest { useStreaming ->
        val obj = CList5(listOf(9,8,7,6,5), 5)
        val j = "{h:5,g:[9,8,7,6,5]}"
        assertEquals(obj, unquoted.parse(j, useStreaming))
    }

    @Test
    fun testWriteOptionalList5b() = parametrizedTest { useStreaming ->
        val obj = CList5(h=999)
        val s = unquoted.stringify(obj, useStreaming)
        assertEquals("{h:999}", s)
    }

    @Test
    fun testReadOptionalList5b() = parametrizedTest { useStreaming ->
        val obj = CList5(h=999)
        val j = "{h:999}"
        assertEquals(obj, unquoted.parse(j, useStreaming))
    }

    @Test
    fun testMapBuiltinsTest() = parametrizedTest { useStreaming ->
        val map = mapOf(1 to "1", 2 to "2")
        val serial = (Int.serializer() to String.serializer()).map
        val s = Json { unquoted = true }.stringify(serial, map, useStreaming)
        assertEquals("{1:1,2:2}", s)
    }

    @Test
    fun testResolveAtRootLevel() = parametrizedTest { useStreaming ->
        val j = createJsonWithB()
        val bs = j.parse<B>("1", useStreaming)
        assertEquals(B(1), bs)
    }
}
