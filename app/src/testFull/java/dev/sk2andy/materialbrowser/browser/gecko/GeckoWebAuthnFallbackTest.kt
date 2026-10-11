package dev.sk2andy.materialbrowser.browser.gecko

import java.io.DataInputStream
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Guards passkeys option A against a GeckoView update. GeckoView's WebAuthn code calls Google Play
 * services FIDO, which Vola replaces with refusing stand-ins (`src/full/java/com/google`). Every
 * call GeckoView makes before a stand-in refuses must exist in them: a missing class or method is a
 * `NoClassDefFoundError` on the Gecko thread, which crashed Gecko on accounts.google.com (#123, H1).
 *
 * The references are read from GeckoView's own class files. If this fails after a GeckoView update,
 * re-read `WebAuthnTokenManager` at the new tag and update the stand-ins, not this list alone.
 */
class GeckoWebAuthnFallbackTest {
    @Test
    fun algorithmsListedWhenGeckoLoadsWebAuthnExist() {
        val references = references(TOKEN_MANAGER)
        val algorithms = references.filter {
            it.kind == Kind.Field && (it.owner == EC2 || it.owner == RSA)
        }
        assertEquals(
            setOf("ED256", "ED512", "ES256", "ES384", "ES512", "PS256", "PS384", "PS512", "RS256", "RS384", "RS512"),
            algorithms.map { it.name }.toSet(),
        )
        algorithms.forEach(::assertResolves)
    }

    @Test
    fun platformAuthenticatorQueryReachesARefusingClient() {
        val references = references(TOKEN_MANAGER)
        val privileged = Reference(Kind.Method, FIDO, "getFido2PrivilegedApiClient", "(L$CONTEXT;)L$PRIVILEGED_CLIENT;")
        val regular = Reference(Kind.Method, FIDO, "getFido2ApiClient", "(L$CONTEXT;)L$CLIENT;")
        // GeckoView builds one of the two branches, chosen by its MOZILLA_OFFICIAL constant.
        assertTrue("GeckoView no longer asks Fido for a client", privileged in references || regular in references)
        val required = buildList {
            add(Reference(Kind.Method, TASK, "addOnSuccessListener", "(L$GMS/tasks/OnSuccessListener;)L$TASK;"))
            add(Reference(Kind.Method, TASK, "addOnFailureListener", "(L$GMS/tasks/OnFailureListener;)L$TASK;"))
            if (privileged in references) {
                add(privileged)
                add(Reference(Kind.Method, PRIVILEGED_CLIENT, UVPAA, "()L$TASK;"))
                add(Reference(Kind.Method, PRIVILEGED_CLIENT, "getCredentialList", "(Ljava/lang/String;)L$TASK;"))
            }
            if (regular in references) {
                add(regular)
                add(Reference(Kind.Method, CLIENT, UVPAA, "()L$TASK;"))
            }
        }
        required.forEach { assertReferencedAndResolves(references, it) }
    }

    @Test
    fun passkeyFallbacksStartWithAStandInThatRefuses() {
        val references = references(TOKEN_MANAGER)
        listOf(
            Reference(Kind.Method, "$COMMON/PublicKeyCredentialCreationOptions\$Builder", "<init>", "()V"),
            Reference(Kind.Method, "$COMMON/AuthenticationExtensions\$Builder", "<init>", "()V"),
            Reference(
                Kind.Method,
                "$COMMON/PublicKeyCredentialDescriptor",
                "<init>",
                "(Ljava/lang/String;[BLjava/util/List;)V",
            ),
            Reference(Kind.Field, "$COMMON/PublicKeyCredentialType", "PUBLIC_KEY", "L$COMMON/PublicKeyCredentialType;"),
            Reference(Kind.Method, "$COMMON/PublicKeyCredentialType", "toString", "()Ljava/lang/String;"),
        ).forEach { assertReferencedAndResolves(references, it) }
    }

    @Test
    fun transportsMappedBeforeTheSignInFallbackExist() {
        val references = references(WEBAUTHN_UTILS)
        listOf("USB", "NFC", "BLUETOOTH_LOW_ENERGY", "INTERNAL", "HYBRID").forEach { name ->
            assertReferencedAndResolves(
                references,
                Reference(Kind.Field, "$GMS/fido/common/Transport", name, "L$GMS/fido/common/Transport;"),
            )
        }
    }

    private enum class Kind { Field, Method }

    private data class Reference(val kind: Kind, val owner: String, val name: String, val descriptor: String)

    private fun assertReferencedAndResolves(references: Set<Reference>, reference: Reference) {
        assertTrue("GeckoView no longer calls $reference; re-audit the stand-ins", reference in references)
        assertResolves(reference)
    }

    private fun assertResolves(reference: Reference) {
        val owner = runCatching { Class.forName(reference.owner.replace('/', '.'), false, javaClass.classLoader) }
            .getOrElse { fail("Missing stand-in class for $reference"); return }
        val resolved = when (reference.kind) {
            Kind.Field -> owner.declaredFields.any {
                it.name == reference.name && descriptorOf(it.type) == reference.descriptor
            }
            Kind.Method -> if (reference.name == "<init>") {
                owner.declaredConstructors.any { descriptorOf(it) == reference.descriptor }
            } else {
                (owner.declaredMethods.asSequence() + owner.methods.asSequence()).any {
                    it.name == reference.name && descriptorOf(it) == reference.descriptor
                }
            }
        }
        assertTrue("Stand-in lacks $reference", resolved)
    }

    private fun references(className: String): Set<Reference> {
        val stream = javaClass.classLoader!!.getResourceAsStream("$className.class")
            ?: error("GeckoView class $className is not on the test classpath")
        return DataInputStream(stream.buffered()).use { readGmsReferences(it) }
    }

    /** Reads the member references of a class file's constant pool (JVMS 4.4) that name GMS types. */
    private fun readGmsReferences(input: DataInputStream): Set<Reference> {
        check(input.readInt() == 0xCAFEBABE.toInt()) { "Not a class file" }
        input.readUnsignedShort()
        input.readUnsignedShort()
        val count = input.readUnsignedShort()
        val utf8 = arrayOfNulls<String>(count)
        val classNames = IntArray(count)
        val nameAndTypes = arrayOfNulls<IntArray>(count)
        val members = mutableListOf<Triple<Kind, Int, Int>>()
        var index = 1
        while (index < count) {
            var slots = 1
            when (val tag = input.readUnsignedByte()) {
                1 -> utf8[index] = input.readUTF()
                3, 4 -> input.readInt()
                5, 6 -> {
                    input.readLong()
                    slots = 2
                }
                7 -> classNames[index] = input.readUnsignedShort()
                8, 16, 19, 20 -> input.readUnsignedShort()
                9 -> members += Triple(Kind.Field, input.readUnsignedShort(), input.readUnsignedShort())
                10, 11 -> members += Triple(Kind.Method, input.readUnsignedShort(), input.readUnsignedShort())
                12 -> nameAndTypes[index] = intArrayOf(input.readUnsignedShort(), input.readUnsignedShort())
                15 -> {
                    input.readUnsignedByte()
                    input.readUnsignedShort()
                }
                17, 18 -> input.readInt()
                else -> error("Unknown constant pool tag $tag")
            }
            index += slots
        }
        return members.mapNotNull { (kind, classIndex, nameAndTypeIndex) ->
            val owner = utf8[classNames[classIndex]] ?: return@mapNotNull null
            if (!owner.startsWith("$GMS/")) return@mapNotNull null
            val (name, descriptor) = nameAndTypes[nameAndTypeIndex]!!
            Reference(kind, owner, utf8[name]!!, utf8[descriptor]!!)
        }.toSet()
    }

    private fun descriptorOf(type: Class<*>): String = when {
        type.isArray -> "[" + descriptorOf(type.componentType!!)
        type == Void.TYPE -> "V"
        type == java.lang.Boolean.TYPE -> "Z"
        type == java.lang.Byte.TYPE -> "B"
        type == java.lang.Character.TYPE -> "C"
        type == java.lang.Short.TYPE -> "S"
        type == Integer.TYPE -> "I"
        type == java.lang.Long.TYPE -> "J"
        type == java.lang.Float.TYPE -> "F"
        type == java.lang.Double.TYPE -> "D"
        else -> "L" + type.name.replace('.', '/') + ";"
    }

    private fun descriptorOf(method: Method): String =
        method.parameterTypes.joinToString("", "(", ")") { descriptorOf(it) } + descriptorOf(method.returnType)

    private fun descriptorOf(constructor: Constructor<*>): String =
        constructor.parameterTypes.joinToString("", "(", ")V") { descriptorOf(it) }

    private companion object {
        const val TOKEN_MANAGER = "org/mozilla/geckoview/WebAuthnTokenManager"
        const val WEBAUTHN_UTILS = "org/mozilla/gecko/util/WebAuthnUtils"
        const val GMS = "com/google/android/gms"
        const val COMMON = "$GMS/fido/fido2/api/common"
        const val EC2 = "$COMMON/EC2Algorithm"
        const val RSA = "$COMMON/RSAAlgorithm"
        const val FIDO = "$GMS/fido/Fido"
        const val CLIENT = "$GMS/fido/fido2/Fido2ApiClient"
        const val PRIVILEGED_CLIENT = "$GMS/fido/fido2/Fido2PrivilegedApiClient"
        const val TASK = "$GMS/tasks/Task"
        const val CONTEXT = "android/content/Context"
        const val UVPAA = "isUserVerifyingPlatformAuthenticatorAvailable"
    }
}
