**Download TCW package:**  www.agcol.arizona.edu/software/tcw or [from this site](https://github.com/csoderlund/TCW/releases)

**Documentation:** www.agcol.arizona.edu/software/tcw

**Reference:** C. Soderlund (2019) Transcriptome computational workbench (TCW): analysis of single and comparative transcriptomes. Published at [BioRxiv](https://www.biorxiv.org/content/10.1101/733311v1).

**Description:** For single-transcriptome (singleTCW) - similarity search against annotation databases, GO annotation, ORF finding and differential analysis. For multi-transcriptome (multiTCW) - compute similar pairs, provide statistics for pairs, compute clusters, provide statistics for clusters. Both singleTCW and multiTCW provide graphical interfaces for extensive query and display of the results.

**Requirements:** Java and MySQL to build the database and view the results.  R for differential analysis.  The BLAST executable is necessary. The KaKs_Calculator is optional for the multi-transcriptome analysis. All other external software used by TCW is contained in the package tar file. 

**TCW package:** To use TCW, download the TCW package from the link at the top. The tar file contains all necessary jar files, R scripts and demo files. Follow the instructions at http://www.agcol.arizona.edu/software/tcw/doc/. 

**TCW github code:** This site does not include the external packages; you will need to extract them from the TCW package or provide them yourself. The necessary Java classes are provided, see TCW/java/README.

