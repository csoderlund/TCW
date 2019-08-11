colData <- data.frame(repNames, Condition=grpNames)
suppressPackageStartupMessages(library(DESeq2))
dds <- DESeqDataSetFromMatrix(countData=countData, colData=colData, design = ~ Condition)
dds <- DESeq(dds)
# use $pval for the p-values, $padj is adjusted p-values
results <- results(dds)$padj
rowNames <- rownames(results(dds))