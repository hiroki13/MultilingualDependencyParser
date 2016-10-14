# MultilingualDependencyParser

This repo contains the implementations of the dependency parsers and supertagger described in the following paper:
- [Improving Dependency Parsers with Supertags](http://aclweb.org/anthology/E/E14/E14-4030.pdf), EACL 2014

##### Data
- Universal Dependencies (http://universaldependencies.org/)

##### Example Comand
  - Supertagger: `java -classpath lib/commons-math3-3.6.1.jar -jar MultilingualDependencyParser.jar -parser supetagger -mode train -train path/to/data -test path/to/data -weight_size 500000 -iter 10 -beam 1 -stag_id 1 -output output.txt -hash`
  - Parser: `java -classpath lib/commons-math3-3.6.1.jar -jar MultilingualDependencyParser.jar -parser arcstandard -mode train -train path/to/data -test path/to/data -weight_size 500000 -iter 15 -beam 1 -output output.txt -labeled -hash`
  - Parser with Supertags: `java -classpath lib/commons-math3-3.6.1.jar -jar MultilingualDependencyParser.jar -parser arcstandard -mode train -train path/to/data -test path/to/data -weight_size 500000 -iter 15 -beam 1 -output output.txt -stag -labeled -hash`
