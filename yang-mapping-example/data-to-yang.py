from rdflib import Dataset, URIRef, Literal, BNode

# File paths
NQUAD_FILE_PATH = "output.nq"
RDF_FILE_PATH = "ne40e-small.ttl"
OUTPUT_FILE = "modified_output.ttl"

# Create an RDF dataset (Supports Named Graphs)
g = Dataset()
g.parse(NQUAD_FILE_PATH, format="nquads")
g.parse(RDF_FILE_PATH, format="turtle")

# Dictionary to store mappings of `filteredPath` → `subject`
filtered_subjects = {}

# Step 1: Process all triples in one loop
modified_triples = []

for s, p, o, g_name in g.quads():
    if isinstance(g_name, URIRef):  
        graph_str = str(g_name)

        # Extract only the clean path after "data/"
        if "data/" in graph_str:
            extracted_path = graph_str.split("data/")[-1]

            # If `filteredPath` is already processed, reuse its subject
            if extracted_path not in filtered_subjects:
                print(f" Extracted Path: {extracted_path}")

                # SPARQL Query to find corresponding subjects
                query = f"""
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX yang: <http://www.typefox.io/yang/Yang#>

                SELECT ?subject WHERE {{
                  ?subject rdf:type ?o .
                  BIND(STRAFTER(STR(?subject), "huawei-ifm/") AS ?filteredPath)
                  FILTER (?filteredPath = "{extracted_path}")
                }}
                """

                # Run query locally
                results = g.query(query)

                # Store the first found subject for the extracted path
                for row in results:
                    filtered_subjects[extracted_path] = str(row.subject)
                    print(f" Found Subject: {row.subject} for `{extracted_path}`")
                    break  # Use only the first matching subject

            # Assign new property using the mapped subject if found
            new_property = URIRef(filtered_subjects.get(extracted_path, p))  # Replace property instead of graph

        else:
            new_property = p  # Keep original property if no match

        # Ensure correct RDF syntax for the object
        if isinstance(o, URIRef):  
            o_str = f"<{o}>"  
        elif isinstance(o, Literal):  
            o_str = f'"{o}"'  
        elif isinstance(o, BNode):  
            o_str = f'_:b{o}'  
        else:
            o_str = str(o)  

        # Append correctly formatted triple without the fourth element
        modified_triples.append(f"<{s}> <{new_property}> {o_str} .")

# Step 2: Write the modified triples to a new file
with open(OUTPUT_FILE, "w") as file:
    file.write("\n".join(modified_triples) + "\n")

print(f"\n Filtered and Modified N-Quads saved to {OUTPUT_FILE}")
