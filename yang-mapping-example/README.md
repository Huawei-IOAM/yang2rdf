This example demonstrates how to convert YANG data into RDF and link it to the YANG model. The process involves using RML mappings to transform the data into RDF format, followed by a Python script and a SPARQL query to establish connections between the RDF data and the YANG model.

## Requirements

- Python 3.12
- [RMLmapper](https://github.com/RMLio/rmlmapper-java/releases)

## Setting Up

Clone the repository

```bash
git clone https://github.com/Huawei-IOAM/yang2rdf.git
```

Go to the folder
```bash
cd yang2rdf/yang-mapping-example
```

Download RMLmapper to the folder (Change to another release if the link below doesn't work)
```bash
wget https://github.com/RMLio/rmlmapper-java/releases/download/v7.3.1/rmlmapper-7.3.1-r374-all.jar
```

Run the script
```bash
./run.sh
```

