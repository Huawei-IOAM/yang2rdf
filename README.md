# yang2rdf

Convert YANG models to RDF.

## Requirements

- Java 11 or later
- Maven 3.9.1 or later

# Setting Up

Clone the repository with submodules recursively

```bash
git clone https://github.com/Huawei-IOAM/yang2rdf.git --recursive
```

Or, clone the repository and update the submodules

```bash
git clone https://github.com/Huawei-IOAM/yang2rdf.git
git submodule update --init --recursive
```

Build applications

```bash
mvn clean install
```

Now, you can run CLI application to convert YANG models to RDF

Linux

```bash
cd target
./yang2rdf <yangDir> <outputFile>
```

Windows

```
cd target
yang2rdf <yangDir> <outputFile>
```

This application requires 2 mandatory arguments:

- yangDir - path to folder with Yang models
- outputFile - path to output file with generated RDF data

Examples of YANG models can be found in `yang` folder. This folder points to public repository with platform-specific YANG models for Huawei's products: https://github.com/Huawei/yang

Example (Linux)

```bash
./yang2rdf ../yang/network-router/8.20.0/ne40e-x8x16 ./ne40e-x8x16.rdf
```

Example (Windows)

```bash
yang2rdf ../yang/network-router/8.20.0/ne40e-x8x16 ne40e-x8x16.rdf
```

Alternative option is to execute `yang2rdf` application directly from Maven, `ne40e-x8x16` YANG model is defined as example in profile

```bash
mvn exec:java -P yang2rdf
```

The generated RDF file will be stored in `target/ne40e-x8x16.rdf`.

As next step, you can run CLI application to generate the optimized RDF data

Linux

```bash
./rdf2rdf <sourceFile> <targetFile>
```

Windows

```
rdf2rdf <sourceFile> <targetFile>
```

This application requires 2 mandatory arguments:

- sourceFile - path to source RDF file
- targetFile - path to target RDF file, which will be generated

Example (Linux)

```bash
./rdf2rdf ./ne40e-x8x16.rdf ./ne40e-x8x16-opt.rdf
```

Example (Windows)

```cmd
rdf2rdf ne40e-x8x16.rdf ne40e-x8x16-opt.rdf
```

The `rdf2rdf` application can also be executed directly from Maven

```bash
mvn exec:java -P rdf2rdf
```

The generated RDF will be stored in `target/ne40e-x8x16-opt.rdf`.