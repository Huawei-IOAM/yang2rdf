java -jar rmlmapper-7.3.1-r374-all.jar --serialization nquads -m yang-rml-mapping.ttl > output.nq

./postprocessing.sh

python3 data-to-yang.py