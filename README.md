# ProtoDumper (Protobuf Code Dumper)
A simple tool to dump all protobuf classes to .proto file from a jar file or specified classes.

## Features
- Dump all protobuf classes **fast** and **accurate**
- Support **oneof**, **map** and **repeated** keywords
- Auto **formatting** for dumped .proto file
- Keep **package** structure

original proto file:
```protobuf
syntax = "proto3";
import "me/mantou/common/common.proto";
package me.mantou.client;

option java_package = "me.mantou.client";
option java_outer_classname = "ClientProto";
option java_multiple_files = true;

message UserSettings{
  common.User user = 1;
  repeated string ranks = 2;
  map<string, Setting> settings = 3;

  oneof Payload{
    string s = 4;
    int32 i = 5;
    bool b = 6;
  }

  message Setting{
    string value = 1;
  }
}
```
ProtoDumper dumped proto file:
```protobuf
syntax = "proto3";

package me.mantou.client;

import "me/mantou/common/common.proto";

option java_package = "me.mantou.client";
option java_outer_classname = "ClientProto";
option java_multiple_files = true;

message UserSettings {
  me.mantou.common.User user = 1;
  repeated string ranks = 2;
  repeated me.mantou.client.UserSettings.SettingsEntry settings = 3;
  oneof Payload {
    string s = 4;
    int32 i = 5;
    bool b = 6;
  }

  message SettingsEntry {
    string key = 1;
    me.mantou.client.UserSettings.Setting value = 2;
  }

  message Setting {
    string value = 1;
  }
}
```

package structure:

![](/screenshots/001.png)

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