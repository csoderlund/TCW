# edgeR gln method unless no replicates
library(edgeR)
y <- DGEList(counts=countData,group=grpNames)
y <- calcNormFactors(y)
if (nGroup1==1 && nGroup2==1) {
    writeLines("Using classic with fixed dispersion")
    et <- exactTest(y, dispersion=disp)
    res <- topTags(et, n=nrow(et), adjust.method="BH")
} else {
 	writeLines("Using traditional glm (quasi-likelihood F-tests)")
    design <- model.matrix(~grpNames)
    y <- estimateDisp(y, design)
    fit <- glmQLFit(y,design)
    qlf <- glmQLFTest(fit,coef=2)
    res <- topTags(qlf, n=nrow(qlf), adjust.method="BH")
} 
# Columns are:  logFC    logCPM        F       PValue          FDR
results <- res$table$FDR
rowNames <- rownames(res)
