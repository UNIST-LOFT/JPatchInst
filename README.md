# JPatchInst

JPatchInst is instrumentation tool for Java APR tools. It compares the buggy program and patched program and instruments patched program with considering the differences between them.

In default, JPatchInst adds branch counter for each branches. If you want your own instrumentation, you can easily add your own instrumentation.

## Environment
JDK 1.8.

Java version of the target project should be lower or equal to 1.8 (version 1.8 is recommended).

JPatchInst works well with all Defects4j subjects except Mockito.

## How to run
Clone JPatchInst:

```bash
git clone https://github.com/UNIST-LOFT/JPatchInst.git
```

Build JPatchInst:
```bash
cd JPatchInst
./gradlew shadowJar
```

Compiled .jar file will be generated in `build/libs/`.

Run JPatchInst with:

`java -jar build/libs/JPatchInst.jar [options] <original_class_path>  <patched_class_path>`

* original_class_path: the path of the original class file
* patched_class_path: the path of the patched class file

### Options
* `-i/--branch-id <ids>`: Specify the branch ID to instrument. IDs seperated with comma(,). Default is None (i.e. all branches).
* `-t/--time-output-file <file>`: Compute and save the time to instrument each file.

For example, if the project is Maven project, run JPatchInst with:

```bash
java -jar <path-to-JPatchInst>/build/libs/JPatchInst.jar <path-to-buggy>/target/classes <path-to-patched>/target/classes
```