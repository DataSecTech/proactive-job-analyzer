val t = sc.textFile("hdfs:///demo-data/sample-text.txt")

println("Total Lines: " + t.count())

for (l <- t.collect()) {
  println(l)
}
