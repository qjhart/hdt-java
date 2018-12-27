# HDT Library, Java Implementation. http://www.rdfhdt.org

The fuseki-jar branch contains only the jar files for addition to apache-jena-fuseki.

## Overview

HDT-lib is a Java Library that implements the W3C Submission (http://www.w3.org/Submission/2011/03/) of the RDF HDT (Header-Dictionary-Triples) binary format for publishing and exchanging RDF data at large scale. Its compact representation allows storing RDF in fewer space, providing at the same time direct access to the stored information. This is achieved by depicting the RDF graph in terms of three main components: Header, Dictionary and Triples. The Header includes extensible metadata required to describe the RDF data set and details of its internals. The Dictionary organizes the vocabulary of strings present in the RDF graph by assigning numerical IDs to each different string. The Triples component comprises the internal structure of the RDF graph in a compressed form.

## License

Each module has a different License. Core is LGPL, examples and tools are Apache.

* `hdt-api`: Apache License
* `hdt-java-cli`: (Commandline tools and examples): Apache License
* `hdt-java-core`: Lesser General Public License
* `hdt-jena`: Lesser General Public License
* `hdt-fuseki`: Apache License


## Authors

* Mario Arias <mario.arias@gmailcom>
* Javier D. Fernandez <jfergar@infor.uva.es>
* Miguel A. Martinez-Prieto <migumar2@infor.uva.es>


## Acknowledgements

RDF/HDT is a project developed by the Insight Centre for Data Analytics (www.insight-centre.org), University of Valladolid (www.uva.es), University of Chile (www.uchile.cl). Funded by Science Foundation Ireland: Grant No. SFI/08/CE/I1380, Lion-II; the Spanish Ministry of Economy and Competitiveness (TIN2009-14009-C02-02); and Chilean Fondecyt's 1110287 and 1-110066.
