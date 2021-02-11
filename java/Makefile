# Makefile for sTCW and mTCW 
JAVA_PATH=/usr
#JAVA_PATH=/Library/Java/JavaVirtualMachines/adoptopenjdk-15.jdk/Contents/Home


JAVA_FILES=	./src/sng/runDE/*.java \
			./src/sng/runAS/*.java \
			./src/sng/annotator/*.java \
           	./src/sng/amanager/*.java \
           	./src/sng/assem/*.java \
           	./src/sng/assem/enums/*.java \
           	./src/sng/assem/helpers/*.java \
           	./src/sng/database/*.java \
			./src/sng/dataholders/*.java \
           	./src/sng/viewer/*.java \
           	./src/sng/viewer/panels/*.java \
           	./src/sng/viewer/panels/align/*.java \
           	./src/sng/viewer/panels/Basic/*.java \
           	./src/sng/viewer/panels/seqDetail/*.java \
           	./src/sng/viewer/panels/seqTable/*.java \
           	./src/sng/viewer/panels/pairsTable/*.java \
           	./src/sng/util/*.java \
           	./src/cmp/compile/*.java \
	   		./src/cmp/compile/panels/*.java \
           	./src/cmp/database/*.java \
           	./src/cmp/align/*.java \
	   		./src/cmp/viewer/*.java \
           	./src/cmp/viewer/panels/*.java \
           	./src/cmp/viewer/seqDetail/*.java \
           	./src/cmp/viewer/seq/*.java \
           	./src/cmp/viewer/seq/align/*.java \
           	./src/cmp/viewer/groups/*.java \
           	./src/cmp/viewer/pairs/*.java \
           	./src/cmp/viewer/hits/*.java \
           	./src/cmp/viewer/table/*.java	\
           	./src/util/align/*.java \
           	./src/util/database/*.java \
           	./src/util/file/*.java \
           	./src/util/methods/*.java \
           	./src/util/ui/*.java
	   		
CLASSES_EXT = -C ./classes_ext netscape -C ./classes_ext sun -C ./classes_ext com -C ./classes_ext org
CLASSES_STCW  = -C ./classes sng -C ./classes util -C ./src html -C ./src images
CLASSES_MTCW  = -C ./classes cmp -C ./classes util -C ./src html -C ./src images

all : clean build jars clean_classes  

build:
	$(JAVA_PATH)/bin/javac -Xlint -classpath ./classes_ext -sourcepath ./src/cmp:./src/sng:./src/util -d ./classes $(JAVA_FILES)

jars: build stcw_jar mtcw_jar

stcw_jar:
	$(JAVA_PATH)/bin/jar -cfm jars/stcw.jar manifest.add $(CLASSES_EXT) $(CLASSES_STCW)

mtcw_jar:
	$(JAVA_PATH)/bin/jar -cfm jars/mtcw.jar manifest.add $(CLASSES_EXT) $(CLASSES_MTCW)


clean_classes:
	rm -rf classes/*
	
clean:
	mkdir -p classes
	mkdir -p jars
	rm -rf classes/*
	rm -rf jars/* 

