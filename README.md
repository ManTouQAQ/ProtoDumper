# ProtoDumper (Protobuf Code Dumper)
A simple tool to dump all protobuf classes from a jar file or specified classes.

## Usage
dump a jar file:

```kotlin
import java.io.File

val protoDumper = ProtoDumper(File("path/to/your/file"))
protoDumper.init()
protoDumper.dumpTo(Path.of("output"))
```

dump specified classes:

```kotlin
val protoDumper = ProtoDumper()
protoDumper.init()
protoDumper.addNeedDumpClass(YourProto::class.java)
protoDumper.addNeedDumpClass("com.example.OtherProto")
protoDumper.dumpTo(Path.of("output"))
```