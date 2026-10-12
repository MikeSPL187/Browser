package dev.sk2andy.materialbrowser.browser.gecko

import java.io.DataInputStream
import java.io.File
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.net.JarURLConnection

/**
 * The Google Play services classes, fields and methods GeckoView's class files name, read from
 * their constant pools (JVMS 4.4), and whether Vola's stand-ins (`src/full/java/com/google`) have
 * them. A test helper for the passkeys option A guards.
 */
internal object GeckoPlayServicesReferences {
    const val GMS = "com/google/android/gms"
    const val TOKEN_MANAGER = "org/mozilla/geckoview/WebAuthnTokenManager"
    const val WEBAUTHN_UTILS = "org/mozilla/gecko/util/WebAuthnUtils"

    enum class Kind { Class, Field, Method }

    data class Reference(val kind: Kind, val owner: String, val name: String = "", val descriptor: String = "") {
        /** A class as its internal name, a field as `owner.name:descriptor`, a method as `owner.name(descriptor)`. */
        val key: String
            get() = when (kind) {
                Kind.Class -> owner
                Kind.Field -> "$owner.$name:$descriptor"
                Kind.Method -> "$owner.$name$descriptor"
            }
    }

    private val loader: ClassLoader = GeckoPlayServicesReferences::class.java.classLoader!!

    /** The references one GeckoView class makes. */
    fun inClass(className: String): Set<Reference> {
        val stream = loader.getResourceAsStream("$className.class")
            ?: error("GeckoView class $className is not on the test classpath")
        return DataInputStream(stream.buffered()).use(::read)
    }

    /** The references every class in the archive (or folder) GeckoView loads from makes. */
    fun inGeckoView(): Set<Reference> {
        val url = loader.getResource("$TOKEN_MANAGER.class")
            ?: error("GeckoView class $TOKEN_MANAGER is not on the test classpath")
        val found = mutableSetOf<Reference>()
        if (url.protocol == "jar") {
            val connection = url.openConnection() as JarURLConnection
            connection.useCaches = false
            connection.jarFile.use { jar ->
                jar.entries().asSequence().filter { it.name.endsWith(".class") }.forEach { entry ->
                    found += DataInputStream(jar.getInputStream(entry).buffered()).use(::read)
                }
            }
        } else {
            val root = File(url.toURI()).path.removeSuffix("$TOKEN_MANAGER.class".replace('/', File.separatorChar))
            File(root).walkTopDown().filter { it.isFile && it.name.endsWith(".class") }.forEach { file ->
                found += DataInputStream(file.inputStream().buffered()).use(::read)
            }
        }
        check(found.isNotEmpty()) { "No Play services reference found in GeckoView; is $url its archive?" }
        return found
    }

    fun ownerClass(reference: Reference): Class<*>? =
        runCatching { Class.forName(reference.owner.replace('/', '.'), false, loader) }.getOrNull()

    fun resolves(reference: Reference): Boolean {
        val owner = ownerClass(reference) ?: return false
        return when (reference.kind) {
            Kind.Class -> true
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
    }

    private fun read(input: DataInputStream): Set<Reference> {
        check(input.readInt() == 0xCAFEBABE.toInt()) { "Not a class file" }
        input.readUnsignedShort()
        input.readUnsignedShort()
        val count = input.readUnsignedShort()
        val utf8 = arrayOfNulls<String>(count)
        val classNames = IntArray(count)
        val nameAndTypes = arrayOfNulls<IntArray>(count)
        val members = mutableListOf<Triple<Kind, Int, Int>>()
        val classes = mutableListOf<Int>()
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
                7 -> {
                    classNames[index] = input.readUnsignedShort()
                    classes += index
                }
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
        // A class constant names a type the code checks, casts to or creates; an array type counts
        // as its element type.
        val classReferences = classes.mapNotNull { classIndex ->
            val name = utf8[classNames[classIndex]]?.trimStart('[')?.removePrefix("L")?.removeSuffix(";")
            name?.takeIf { it.startsWith("$GMS/") }?.let { Reference(Kind.Class, it) }
        }
        return members.mapNotNull { (kind, classIndex, nameAndTypeIndex) ->
            val owner = utf8[classNames[classIndex]] ?: return@mapNotNull null
            if (!owner.startsWith("$GMS/")) return@mapNotNull null
            val (name, descriptor) = nameAndTypes[nameAndTypeIndex]!!
            Reference(kind, owner, utf8[name]!!, utf8[descriptor]!!)
        }.toSet() + classReferences
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
}
