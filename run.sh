mvn clean compile assembly:single
java -cp ./target/janusgraph-importer-0.0.1-SNAPSHOT-jar-with-dependencies.jar:./resources net.mpolonioli.ldbcimpls.janusgraph.importer.JanusGraphImporter