# goSeq - The p-values are not multiple hypothesis tested

suppressPackageStartupMessages(library(goseq))
pwf <- nullp(seqDEs,'','',seqLens,FALSE)
GO.wall <- goseq(pwf,'','',seqGOs)
goNums  <- GO.wall$category
results <- GO.wall$over_represented_pvalue
