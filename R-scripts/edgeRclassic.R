# edgeR classic method using exactTest
library(edgeR)
y <- DGEList(counts=countData,group=grpNames)
y <- calcNormFactors(y)
if (nGroup1==1 && nGroup2==1) {
    writeLines("Using classic with fixed dispersion")
    et <- exactTest(y, dispersion=disp)

} else {
 	writeLines("Using classic with design matrix")
    design <- model.matrix(~grpNames)
    y <- estimateDisp(y, design)
    et <- exactTest(y) 
} 
res <- topTags(et, n=nrow(et), adjust.method="BH")
# Columns are:  logFC    logCPM        PValue          FDR
results <- res$table$FDR
rowNames <- rownames(res)
