# JPatchInst

JPatchInst is instrumentation tool for Java APR tools. It compares the buggy program and patched program and instruments patched program with considering the differences between them.

In default, JPatchInst adds branch counter for each branches. If you want your own instrumentation, you can easily add your own instrumentation.

## Environment
* Java 1.8 and target program should be lower than or equal to Java 1.8.

I tested JPatchInst with Defects4j.

## How to run
Clone JPatchInst:

```$ git clone https://github.com/FreddyYJ/greybox-APR.git```

Build JPatchInst:
```
$ cd greybox-APR
$ ./gradlew shadowJar
```

Run with:

`java -jar build/libs/JPatchInst.jar <original_file> <patched_file> <original_source_path> <target_source_path> <class_path>`

* original_file: patched source file in buggy program
* patched_file: patched source file in patched program
* original_source_path: source path of buggy program
* target_source_path: source path of patched program
* class_path: absolute path of JPatchInst.jar