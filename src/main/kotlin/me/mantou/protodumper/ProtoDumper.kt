package me.mantou.protodumper

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label.*
import com.google.protobuf.DescriptorProtos.FileOptions
import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.EnumDescriptor
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.Descriptors.GenericDescriptor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.jar.JarFile

class ProtoDumper(private val file: File? = null, private val tabLength: Int = 4) {

    private val protoClassesStore = mutableListOf<String>()
    private var classLoader: ClassLoader? = null
    private val outputFiles = mutableListOf<String>()

    fun init() {
        if (file == null) {
            this.classLoader = this::class.java.classLoader
            return
        }
        this.classLoader = URLClassLoader(arrayOf(file.toURI().toURL()))

        val jarFile = JarFile(file)
        jarFile.entries().iterator().forEach { entry ->
            if (entry.name.endsWith(".class")) {
                val classReader = ClassReader(jarFile.getInputStream(entry))
                classReader.accept(object : ClassVisitor(Opcodes.ASM9) {
                    override fun visit(
                        version: Int,
                        access: Int,
                        name: String,
                        signature: String?,
                        superName: String?,
                        interfaces: Array<out String>?
                    ) {
                        if (!name.startsWith("com/google/protobuf")) {
                            if (superName == "com/google/protobuf/GeneratedMessageV3") {
                                protoClassesStore.add(name.replace("/", "."))
                            } else if (interfaces?.contains("com/google/protobuf/ProtocolMessageEnum") == true) {
                                protoClassesStore.add(name.replace("/", "."))
                            }
                        }
                        super.visit(version, access, name, signature, superName, interfaces)
                    }
                }, ClassReader.SKIP_CODE)
            }
        }
    }

    fun addNeedDumpClass(className: String) {
        protoClassesStore.add(className)
    }

    fun addNeedDumpClass(className: Class<*>) {
        protoClassesStore.add(className.name)
    }

    fun dumpTo(path: Path) {
        for (protoClass in protoClassesStore) {
            val loadClass = classLoader!!.loadClass(protoClass)

            val descriptor = loadClass.getMethod("getDescriptor").let {
                it.isAccessible = true
                it.invoke(null)
            } as GenericDescriptor

            genProtoFile(path, descriptor.file)
        }
    }

    private fun genProtoFile(path: Path, descriptor: FileDescriptor) {
        val name = descriptor.fullName
        if (outputFiles.contains(name)) {
            return
        }

        val file = path.resolve(name).toFile()
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        if (!file.exists()) {
            file.createNewFile()
        } else {
            file.delete()
        }

        BufferedWriter(FileWriter(file, true)).use {
            writeMeta(it, descriptor)
            it.newLine()
            descriptor.messageTypes.forEach { descriptor ->
                writeMessage(it, descriptor)
                it.newLine()
            }
            it.newLine()
            descriptor.enumTypes.forEach { descriptor ->
                writeEnum(it, descriptor)
                it.newLine()
            }
            it.newLine()
        }

        outputFiles.add(name)
    }

    private fun writeMeta(writer: BufferedWriter, descriptor: FileDescriptor) {
        val proto = descriptor.toProto()
        writer.write("syntax = \"${proto.syntax}\";\n\n")
        writer.write("package ${descriptor.`package`};\n\n")
        writeDependencies(writer, descriptor)
        writeOption(writer, proto.options)
    }

    private fun writeDependencies(writer: BufferedWriter, descriptor: FileDescriptor) {
        descriptor.dependencies.forEach {
            writer.write("import \"${it.name}\";\n\n")
        }
    }

    private fun writeOption(writer: BufferedWriter, options: FileOptions) {
        writer.write("option java_package = \"${options.javaPackage}\";\n")
        writer.write("option java_outer_classname = \"${options.javaOuterClassname}\";\n")
        writer.write("option java_multiple_files = ${options.javaMultipleFiles};\n")
    }

    private fun writeEnum(writer: BufferedWriter, descriptor: EnumDescriptor, spaceCount: Int = 0) {

        fun writeWithIndent(text: String) {
            writer.write(" ".repeat(spaceCount) + text)
        }

        writeWithIndent("enum ${descriptor.name} {\n")

        for ((fieldNumber, field) in descriptor.values.withIndex()) {
            writeWithIndent(" ".repeat(tabLength) + "${field.name} = $fieldNumber;\n")
        }

        writeWithIndent("}\n")
    }

    // 递归写入消息定义
    private fun writeMessage(writer: BufferedWriter, descriptor: Descriptor, spaceCount: Int = 0) {

        fun writeWithIndent(text: String) {
            writer.write(" ".repeat(spaceCount) + text)
        }

        writeWithIndent("message ${descriptor.name} {\n")

        // 写入字段
        var fieldNumber = 1
        for (field in descriptor.fields) {
            val type = when (field.type.javaType!!) {
                Descriptors.FieldDescriptor.JavaType.INT -> "int32"
                Descriptors.FieldDescriptor.JavaType.LONG -> "int64"
                Descriptors.FieldDescriptor.JavaType.STRING -> "string"
                Descriptors.FieldDescriptor.JavaType.BOOLEAN -> "bool"
                Descriptors.FieldDescriptor.JavaType.FLOAT -> "float"
                Descriptors.FieldDescriptor.JavaType.ENUM -> field.enumType.fullName
                Descriptors.FieldDescriptor.JavaType.BYTE_STRING -> "bytes"
                Descriptors.FieldDescriptor.JavaType.MESSAGE -> field.messageType.fullName
                Descriptors.FieldDescriptor.JavaType.DOUBLE -> "double"
            }
            val statement = "$type ${field.name} = $fieldNumber;\n"

            var label = ""
            if (field.toProto().hasLabel()) {
                label = when (field.toProto().label!!) {
                    LABEL_OPTIONAL -> ""
                    LABEL_REPEATED -> "repeated "
                    LABEL_REQUIRED -> "required "
                }
            }
            writeWithIndent(" ".repeat(tabLength) + label + statement)
            fieldNumber++
        }

        var needNewLine = true

        // 处理嵌套的消息类型
        for ((index, nestedDescriptor) in descriptor.nestedTypes.withIndex()) {
            if (needNewLine) {
                writer.newLine()
                needNewLine = false
            }

            writeMessage(writer, nestedDescriptor, spaceCount + this.tabLength)
            if (index < descriptor.nestedTypes.size - 1) {
                writer.newLine()
            }
        }

        // 处理嵌套的枚举类型
        for ((index, nestedEnumDescriptor) in descriptor.enumTypes.withIndex()) {
            if (needNewLine) {
                writer.newLine()
                needNewLine = false
            }

            writeEnum(writer, nestedEnumDescriptor, spaceCount + this.tabLength)
            if (index < descriptor.enumTypes.size - 1) {
                writer.newLine()
            }
        }

        writeWithIndent("}\n")
    }
}