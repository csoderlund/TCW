# goSeq

suppressPackageStartupMessages(library(goseq))
pwf <- nullp(seqDEs,'','',seqLens,FALSE)
GO.wall <- goseq(pwf,'','',seqGOs)
goNums  <- GO.wall$category
# oResults <- GO.wall$over_represented_pvalue
oResults <- p.adjust(GO.wall$over_represented_pvalue, method="none")
