# Scalable Approximate Nearest Neighbor Search (ScANNS)

**ScANNS** is a nearest neighbor search library for [Apache Spark](http://spark.apache.org/) originally
developed by [Namit Katariya](https://www.linkedin.com/in/namitkatariya) from the LinkedIn Machine Learning
Algorithms team. It enables nearest neighbor search in a batch offline context within the
[cosine](https://en.wikipedia.org/wiki/Cosine_similarity#Angular_distance_and_similarity),
[jaccard](https://en.wikipedia.org/wiki/Jaccard_index) and [euclidean](https://en.wikipedia.org/wiki/Euclidean_distance)
distance spaces.

This library has been tested to scale to _hundreds of millions to low billions of data points_. 

<!-- MarkdownTOC autolink=true bracket=round depth=0 -->

- [Example usage](#example-usage)
- [Why a new library?](#why-a-new-library)
  - [Shortcomings of existing solutions](#shortcomings-of-existing-solutions)
    - [Bucket skew](#bucket-skew)
    - [Top-k join](#threshold-based-similarity-join-vs-top-k-join)
- [Understanding the implementation](#understanding-the-implementation)
- [Understanding the model parameters](#understanding-the-model-parameters)
- [Build and try it out!](#build-and-try-it-out)
- [Contributions](#contributions)
- [References](#references)

<!-- /MarkdownTOC -->

### Example usage
The input for the algorithms is datasets of the form `RDD[(Long, org.apache.spark.ml.linalg.Vector)]`. The reason for
using RDDs and not DataFrame / Dataset was to retain some of the lower level API that allows more control over the joins
performed in the algorithm.

As explained [here](https://github.com/linkedin/photon-ml/blob/master/README.md#input-data-format), we use the so-called
name-term-value avro format at LinkedIn to represent features. The
[NearestNeighborSearchDriver](scanns/src/main/scala/com/linkedin/nn/NearestNeighborSearchDriver.scala) provides a few
utilities to read datasets of this form. It can also be referred to for examples to better understand how to use the
library.

However, you will likely have your own representation for features. The requirement is simply that you _convert your
representations to [Vector](https://spark.apache.org/docs/latest/api/scala/index.html#org.apache.spark.ml.linalg.Vector)
form_. Here is an example to perform nearest neighbor search in cosine distance space

```scala
import com.linkedin.nn.algorithm.CosineSignRandomProjectionNNS

val items: RDD[(Long, Vector)] // your dataset conversion steps here
/* If you intend to search for nearest neighbors of a set of source items within a different set of candidate items,
read that dataset too */
val candidatePool: RDD[(Long, Vector)] // your dataset conversion steps here

val numFeatures = items.first._2.size
val model = new CosineSignRandomProjectionNNS()
              .setNumHashes(300)
              .setSignatureLength(15)
              .setJoinParallelism(5000)
              .setBucketLimit(1000)
              .setShouldSampleBuckets(true)
              .setNumOutputPartitions(100)
              .createModel(numFeatures)

val numCandidates = 100

// get 100 nearest neighbors for each item in items from within candidatePool
val nbrs: RDD[(Long, Long, Double)] = model.getAllNearestNeighbors(items, candidatePool, numCandidates)

// get 100 nearest neighbors for each item in items from within itself
val nbrs: RDD[(Long, Long, Double)] = model.getSelfAllNearestNeighbors(items, numCandidates)

// assuming we have a query vector, get 100 nearest neighbors of query from within items
val nbrs: Array[(Long, Double)] = model.getNearestNeighbors(query, items, numCandidates)
```

Please refer to the sections about [implementation](#understanding-the-implementation) and
[model parameters](#understanding-the-model-parameters) for an in-depth explanation of the API and each of the
parameters used above

## Why a new library?
When this effort first started, the Spark community was in the process of reviewing pull requests for adding
[locality sensitive hashing](https://en.wikipedia.org/wiki/Locality-sensitive_hashing) support to spark. These were
eventually approved in Spark 2.0. However, there were a few issues. Search in only jaccard and L2 distance spaces were
supported. For our use case, we wanted support for nearest neighbor search in cosine distance space e.g. nearest neighbor
search in [tf-idf](https://en.wikipedia.org/wiki/Tf-idf) space. Cosine is also a useful metric for many of the entity
[embeddings](http://colah.github.io/posts/2014-07-NLP-RNNs-Representations/) that we learn here at LinkedIn.

### Shortcomings of existing solutions
#### Bucket Skew
Even discounting the availability and lack of support for cosine distance, we tried jaccard as well as L2 distance
[locality sensitive hashing](https://en.wikipedia.org/wiki/Locality-sensitive_hashing) implementations in Spark when
they became available as well as other open source implementations on hadoop / spark but could not get them to work on
our datasets. For a dataset with few million items, performing all-nearest-neighbors would fail with memory errors. 
After bumping up the resources to a pretty high value, another issue, what we call "bucket skew", became apparent.
Remember that the idea behind LSH is that it tries to map similar items to same buckets with a high probability and the
idea for nearest neighbors is therefore to perform LSH and perform brute force pairwise distance computation within
each bucket. **Every pair in the bucket is a potential candidate pair.** Therefore, a particular bucket becomes a
bottleneck if it has a large number of items (say >40k-50k) which becomes more probable as you scale your input.

Many implementations in the community that we reviewed used the built-in `join` / `group` operator to perform LSH
based nearest neighbor search. The idea is that you assign bucket ids to each item in your dataset and then group by the
bucket id to figure out items that lie in the same bucket. However, note that if there is a bucket skew, the `join` is
really a skewed join which means it will run into severe performance issues. If we want to process the skewed buckets in
their entirety, we have no choice but to suffer large wait times. However, given that LSH is an approximate algorithm,
we make some concessions and do "better" than process all the skewed buckets to completion. (see
[implementation](#understanding-the-implementation) section for more details)

#### Threshold based similarity join vs top-k join

Many of these implementations are also based on a threshold distance *t*: the user specifies what maximum distance is 
acceptable between any two candidates and the algorithm only outputs pairs that are at most *t* apart. This gets 
applied to *all* items for which nearest neighbors are sought. This is a way for the implementation to keep the output 
tractable. Note that since we perform brute-force all-pairs comparison within each bucket, without any constraints, we 
would simply output all pairs in a bucket, for every bucket. For data with thousands / millions of buckets, this output 
will be very large to make sense of and also, most likely be extremely noisy. Thresholding the pairs based on distance 
is one way of addressing this problem. However, since the threshold is global, this strategy can be undesirable in some 
scenarios because there can be a large difference in the distribution of neighbor-distances for different items. Many 
items in the data might have several small distance neighbors available so that we would potentially want to set the 
maximum distance small enough. However this could lead to no neighbors being returned for several other items in the 
data which do not have many "nearby" neighbors. For applications such as say near-dupe detection, not getting any 
neighbors for some items in the data is fine and could even be desirable (might just mean that there are no 
sufficiently similar objects in the data to the given item). However, for applications such as candidate generation 
for recommender systems, one wants to return recommendations for all items and thus wants neighbors / candidates for 
all of them.

We therefore rely on thresholding based on number of nearest neighbors required for each item (i.e top-*k*). A fair 
question is "why can't we set the threshold to be a very large value and then choose the top-*k*?". That would be 
entirely possible if the join did work. Since skewed joins are not addressed and threshold is being applied as a 
post-processing step, the threshold value is redundant (as a scalability tool) if the join cannot be performed. Our 
implementation would face the same problem if we did not address the skewed joins and chose top-*k* (instead of 
thresholding) as a post-processing step. Therefore, our implementation, more importantly, scales the algorithm to 
process buckets and also, addresses the difficulty of the user having to choose a global threshold by returning top-*k* 
nearest neighbors instead. Since we also output distance between the item and the candidate item, the user is free to 
apply thresholding as a post-processing step to discard items that are too far.

## Understanding the implementation
Let's consider a very simple example with 2 datasets X and Y where we intend to find the nearest neighbor items to all
items in X from within the items in Y. Assume that ![hvi](https://latex.codecogs.com/png.latex?h(v_i)) as well as
![gvi](https://latex.codecogs.com/png.latex?g(v_i)) below are the individual bucket ids that a given item vector
 ![vi](https://latex.codecogs.com/png.latex?v_i) lies in; that is, the hash functions have already been combined via
 [amplification](https://en.wikipedia.org/wiki/Locality-sensitive_hashing#Amplification) and the consolidated hashes
 are what we refer to by *g* and *h*. *i* is the index over various items in the data.

| X | Y |
|:-:|:-:|
| <table> <tr><th> id </th><th> vector </th><th> bucket ids </th></tr><tr><td> 0 </td><td> v0 </td><td> h(v0), g(v0) </td></tr><tr><td> 1 </td><td> v1 </td><td> h(v1), g(v1) </td></tr></table> | <table> <tr><th> id </th><th> vector </th><th> bucket ids </th></tr><tr><td> 2 </td><td> v2 </td><td> h(v2), g(v2) </td></tr></tr><tr><td> 3 </td><td> v3 </td><td> h(v3), g(v3) </td></tr></table> |

Now we want to map the points to their respective buckets and process each bucket individually. The way to do this is by
"exploding" the two datasets. The idea is to replicate the data point with one row each for each of the buckets in which
it falls.

| X_exploded | Y_exploded |
|:-:|:-:|
| <table> <tr><th> id </th><th> vector </th><th> bucket </th></tr><tr><td> 0 </td><td> v0 </td><td> (0, h(v0)) </td></tr><tr><td> 0 </td><td> v0 </td><td> (1, g(v0)) </td></tr><tr><td> 1 </td><td> v1 </td><td> (0, h(v1)) </td></tr><tr><td> 1 </td><td> v1 </td><td> (1, g(v1)) </td></tr></table> | <table> <tr><th> id </th><th> vector </th><th> bucket </th></tr><tr><td> 2 </td><td> v2 </td><td> (0, h(v2)) </td></tr><tr><td> 2 </td><td> v2 </td><td> (1, g(v2)) </td></tr><tr><td> 3 </td><td> v3 </td><td> (0, h(v3)) </td></tr><tr><td> 3 </td><td> v3 </td><td> (1, g(v3)) </td></tr></table> |

Now if we were to join X_exploded and Y_exploded on the "bucket" column, we would get an iterator over all points that
lie in that buckets at which point we can figure out which of those candidate pairs are relevant. There are several
tricks that we apply to make this more efficient:

1. **[Hashing trick](https://en.wikipedia.org/wiki/Feature_hashing)**: The (index, hash bucket) tuple can be expensive
to shuffle around since it consists of an integer index along with multiple hash values (This is because the hash 
bucket is formed via amplification so that it is identified by a combination of hashes). What we ultimately care about
is just that the index matches and the individual hash values match. We don't care about the actual value of the index
or the hashes themselves. Therefore, we use the hashing trick to simply hash this tuple to an integer. <br/> _Note:_
This "hash" has nothing to do with the hashes of LSH or the distance metric. This is akin to hash codes defined for
custom objects in Java / Scala). <p> While this guarantees that tuples that are same get mapped to the same integer,
there can also be collisions and unequal hash bucket items may get the same integer value. However, this does not affect
the "correctness" of our algorithm, it simply increases the number of elements we would need to check in the brute
force step. </p>

2. **Custom join**: The locality sensitive family hashes that we generate are based on randomness. So there is always a
 chance, either due to the particular choice of the functions, number of hashes being too small or due to the
 distribution of the input data, that a particular bucket or multiple buckets end up containing a very large number of
 items unsuitable for brute force search. This is what we have referred to as "bucket skew" previously. <p> Additionally, note that "exploded" data is a highly wasteful representation and ends up replicating the item vector
 several times. This means that the partition / reducer that processes a skewed bucket ends up having to process a very
 large amount of data, drastically slowing things down. To address bucket skew and replication, it is essential to have
 control over the join we perform and be able to accept or discard items while assigning them to buckets. Most
 implementations join the exploded datasets using the built-in `join` operation of the platform (Hadoop, Spark)
 and then process the joined dataset. However if this is a skewed join, the processing step faces a very large
 wait-time. Instead, we perform the join ourselves by partitioning the data based on the keys (bucket ids) and
 processing each partition using custom logic and our own data structures. Refer
 [this code](scanns/src/main/scala/com/linkedin/nn/model/LSHNearestNeighborSearchModel.scala) for details. </p> <p> We also introduce user-configurable parameters called *bucketLimit* and *shouldSampleBuckets* to return candidates. The
 idea of "bucket limit" is fairly straight-forward. If we do brute force search over populous buckets in their entirety,
 we will end up creating a bottleneck. However, given that LSH is an approximate algorithm, we make a trade-off. The
 intuition is that if an item has a lot of similar items i.e falls in a bucket along with numerous other items and it
 is expensive to compare it to all of them, it's better to compare it to some of them rather than all or none. This is
 a slightly principled extension of a partial join. A naive way to deal with skewed buckets would be to return the
 results for bucket ids that have tractable number of items and ignore the skewed buckets altogether, acknowledging
 that it is not an exact join. The improvement made over this approach is to give the user control over *how many items
 in the skewed bucket a given item gets compared against* by configuring `bucketLimit` and *how they are chosen* by
 configuring `shouldSampleBuckets`. If `shouldSampleBuckets` is set to `true`, every item in a skewed bucket is compared
 against a random sample of `bucketLimit` items from within that bucket. While we do not make claims about this being
 theoretically sound, we found this to work quite well in practice. </p> <p> Given the randomized nature of this approach, this also works well when you aggregate and improve results over several
 runs of this algorithm on the same dataset. Assume that you had two runs
 ![r_1](https://latex.codecogs.com/png.latex?r_1) and ![r_2](https://latex.codecogs.com/png.latex?r_2) of this algorithm
 to find the nearest neighbors. Consider an item *I* for which you got *k* nearest neighbors from
 ![r_1](https://latex.codecogs.com/png.latex?r_1) and *k* nearest neighbors from
 ![r_2](https://latex.codecogs.com/png.latex?r_2) A simple way to get a better set of nearest neighbors for *I* would be
 to merge the two sets of neighbors from each of the runs and keep the top-*k*. If one assumes that similar items tend
 to fall in the same bucket, a "new" random sample in every run, over a series of runs can get closer and closer to the
 result of compared the item against *all* items in a skewed bucket in the first run itself. What this means for the
 application is that the candidate set could be made "better" (with respect to nearest neighbors) over time. </p>

3. **TopNQueue** We also use a custom [TopNQueue](scanns/src/main/scala/com/linkedin/nn/utils/TopNQueue.scala) that is a
wrapper around scala's PriorityQueue with the total number of elements it can hold being constant. This is used to
return the top-k nearest neighbors rather than neighbors within a threshold distance. Note that this queue needs to be a
non-duplicate queue since for a particular source and candidate item, they can "match" in multiple buckets.

4. **Custom iterator**: Within a bucket, while returning the candidates via brute force, it can be wasteful to
materialize all pairs in memory since the number of pairs can be pretty large. Instead, we generate pairs on an
on-demand basis (think [yield in python](https://stackoverflow.com/questions/231767/what-does-the-yield-keyword-do)) by
building a custom iterator to process the buckets.

For reference, here is the [API diagram](API.png)

## Understanding the model parameters
The first choice you need to make is what we want to define as the distance between two vectors in our datasets. Three
metrics are currently supported

| Metric | Description | LSH algorithm class | LSH Model class |
|:-:|---|:-:|:-:|
| jaccard | Used for computing distance / similarity between two sets. With sets, you only have present-absent information which in terms of vectors, translates to binary feature vectors. What this means is that even though the vector has (index, value) fields, the value will be ignored and only the indices information will be used   | [JaccardMinHashNNS](scanns/src/main/scala/com/linkedin/nn/algorithm/JaccardMinHashNNS.scala) | [JaccardMinHashModel](scanns/src/main/scala/com/linkedin/nn/model/JaccardMinHashModel.scala) |
| cosine | Intuitively, this metric measures defines distance between two given vectors as the angle between them. Unlike jaccard, the value information in the vector will be preserved. | [CosineSignRandomProjectionNNS](scanns/src/main/scala/com/linkedin/nn/algorithm/CosineSignRandomProjectionNNS.scala) | [CosineSignRandomProjectionModel](scanns/src/main/scala/com/linkedin/nn/model/CosineSignRandomProjectionModel.scala) |
| l2 | This is the standard euclidean distance metric. Unlike jaccard and similar to cosine, the value information in the vector will be preserved. | [L2ScalarRandomProjectionNNS](scanns/src/main/scala/com/linkedin/nn/algorithm/L2ScalarRandomProjectionNNS.scala) | [L2ScalarRandomProjectionModel](scanns/src/main/scala/com/linkedin/nn/model/L2ScalarRandomProjectionModel.scala) |

#### numHashes and signatureLength
The **numHashes** and **signatureLength** (also called _bandSize_ in some literature) parameters are common to all
supported LSH algorithms. In short, the number of hashes decreases the variance of our estimates while the signature
length controls the false positive / false negative rate. Larger signature lengths decrease the false positive rate but
may also lead to higher false negatives (items that were sufficiently similar but did not end up in the same bucket).

#### bucketWidth
For L2 distance LSH, we have an additional parameter **bucketWidth** (also referred to as _length_ in some literature).
It controls the "width" of hash buckets; a larger bucket lowers the false negative rate. The number of buckets will be
(max L2 norm of input vectors) / bucketWidth. If input vectors are normalized, 1-10 times of
![bucketWidth](https://latex.codecogs.com/png.latex?\text{numRecords}^{-1/\text{inputDim}}) is supposedly a reasonable
value.

<p>
There are four additional parameters that are implementation specific but are critical to the performance.
<b> joinParallelism </b> is related to the need of a custom join while <b> bucketLimit </b> and
<b> shouldSampleBuckets </b> are related to the bucket skew issue. <b> numOutputPartitions </b> decides how many
partitions to repartition the output into (in turn controlling the number of files that would get written if we were to
write the output back to a filesystem).
</p>

#### joinParallelism
The parallelism of the join controls how much data each join partition / join task would process. You want this to be a
reasonable size given the size of your datasets. Remember that the dataset that gets processed in the join is the
"exploded" dataset so in cases where your original dataset itself is large (for instance, tens to hundreds of millions
of items), you will want to set the parallelism to fairly large values such as few tens of thousands or even a couple of
hundred thousand. There is a tradeoff here: increasing it arbitrarily does not guarantee good performance since there is
an overhead to task creation and management in spark. Setting joinParallelism to 50k means that there are going to be
50k tasks performing the join. You want the gains made in processing lesser data per task to out-do the overhead of task
management for the value to be deemed reasonable.

#### bucketLimit and shouldSampleBuckets
The bucket limit is critical to address the aforementioned bucket skew issue. When a bucket contains more items than
the limit set by this parameter, you can make a choice by setting the _shouldSampleBuckets_ boolean parameter
appropriately. In either case, we are going to be discarding elements in the bucket. If _shouldSampleBuckets_ is set to
`true`, **bucketLimit** number of items will be reservoir sampled from the incoming stream. If it is set to `false`, the
first **bucketLimit** number of items will be retained and the rest ignored. The rationale here is that if we were
missing out on high-similarity neighbors in this bucket, given their high similarity, the chance is high that they will
end up matching in another bucket that is not skewed.

#### numOutputPartitions
Since the parallelism of the join can be high, the output produced by the join operation will have a very high number of
partitions even though its size isn't large. Setting **numOutputPartitions** repartitions the output of the join into
given number of partitions so that if the user tried to write this output RDD back to a filesystem, the number of files
it will be split into is tractable.

## Build and try it out!
The easiest way to try it out would be via spark-shell or a jupyter notebook. The jar can be obtained by building the
project.
```bash
git clone git@github.com:linkedin/scanns.git
cd scanns
./gradlew build # jar will be built in build/scanns_2.1*/libs
```

## Contributions
Contributions are welcome. A good way to get started would be to begin with reporting an issue, participating in
discussions, or sending out a pull request addressing an issue. For major functionality changes, it is highly
recommended to exchange thoughts and designs with reviewers beforehand. Well communicated changes will have the highest
probability of getting accepted.

## References
* [Ch3 of Mining Massive Datasets](http://infolab.stanford.edu/~ullman/mmds/book.pdf) and
  [accompanying slides](http://infolab.stanford.edu/~ullman/mining/2009/similarity3.pdf)

