# edgeR glm with contrast 
library(edgeR)
y <- DGEList(counts=countData,group=grpNames)
y <- calcNormFactors(y)
if (nGroup1==1 && nGroup2==1) {
  writeLines("Using classic with fixed dispersion")
  et <- exactTest(y, dispersion=disp)
  res <- topTags(et, n=nrow(et), adjust.method="BH")
} else {
 	writeLines("Using glm with contrast")
    design <- model.matrix(~0+grpNames, data=y$samples)
    colnames(design) <- levels(y$samples$group)
    y <- estimateDisp(y, design)
    fit <- glmQLFit(y,design)
    qlf <- glmQLFTest(fit,contrast=c(-1,1))
    res <- topTags(qlf, n=nrow(qlf), adjust.method="BH")
} 
# Columns are:  logFC    logCPM        F       PValue          FDR
results <- res$table$FDR
rowNames <- rownames(res)
