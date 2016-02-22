library(tm)

#(header-FALSE, stringsAsFactors = FALSE, fileEncoding="latin1")

#setting the working directory
setwd("c:/cmpt 459 project/new testing")

#reading the required file in to data
data <- read.csv("test_tranpose.csv")
#generating a corpus
corpus <- Corpus(VectorSource(data))


#generating the Document Term Matrix, rows: 301 (documents), columns: 7184 (words)
dtm <- DocumentTermMatrix(corpus)
freq <- colSums(as.matrix(dtm))

#length should be total number of terms
length(freq)

#create sort order (descending)
ord <- order(freq,decreasing=TRUE)

freq[ord]
#write.csv(freq[ord], "freq.csv")
#dtm <- removeSparseTerms(x=dtm,sparse = 0.80)

dtm <- DocumentTermMatrix(corpus ,  control=list(wordLengths=c(5, 7),
                                                 bounds = list(global = c(51,109))))



#removes terms with sparsity < 80%
#dtm <- removeSparseTerms(x=dtm,sparse = 0.80)



dtm_tfxidf <- weightTfIdf(dtm)

m <- as.matrix(dtm_tfxidf)

rownames(m) <- sub("X","",colnames(data))

#norm_eucl <- function(m)
 # m/apply(m, 1, function(x) sum(x^2)^.5)

#m_norm <- norm_eucl(m)





cosineSim <- function(x){
  as.dist(x%*%t(x)/(sqrt(rowSums(x^2) %*% t(rowSums(x^2)))))
}
cs <- cosineSim(m)


set.seed(18000) #accuracy is 90.95

#results <- kmeans(cs, 4, 50)
results <- skmeans::skmeans(m, 4, method='pclust', control=list(verbose=TRUE))


x <- results$cluster

write.csv(x,file = "results1111.csv")

#clusters <- 1:4
#for (i in clusters) {
#  
#  cat("Cluster" ,i ,":" , findFreqTerms(dtm_tfxidf[results$cluster==i],2),"\n\n")
#}



