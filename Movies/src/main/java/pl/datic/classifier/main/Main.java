package pl.datic.classifier.main;

import pl.datic.classifier.model.TestDocument;
import pl.datic.classifier.model.TrainingDocument;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.classification.NaiveBayes;
import org.apache.spark.mllib.classification.NaiveBayesModel;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.rdd.RDD;
import scala.Tuple2;
import java.util.*;

public class Main {
    public static void main(String [] args) throws InterruptedException {
        /* Initialize context */
        SparkConf sparkConf = new SparkConf().setAppName("MovieSuggestions").setMaster("local");
        JavaSparkContext sc = new JavaSparkContext(sparkConf);

        /* Prepare data */
        String file = "/Users/bartosz/Development/movie-suggestions/Data/imdb-comments-20160113-1350-work-ids.csv";

        // Load the documents
        JavaRDD<TrainingDocument> data = sc.textFile(file)
                .map((String s) -> {
                    String line[] = s.split(";");
                    return new TrainingDocument(Integer.parseInt(line[0]), line[5], line[4]);
                });
        data.cache();
        Double numDocs = (double) data.count();
        Double idfThreshold = numDocs*0.05;

        //Compute IDF
        JavaPairRDD<String,Double> terms2docs = data
                // Build the base set of term->docId
                .flatMapToPair((TrainingDocument d) -> {
                    ArrayList<Tuple2<String, Integer>> base = new ArrayList<>();
                    d.getTerms().forEach(s -> {
                        base.add(new Tuple2<String, Integer>(s, d.getDocId()));
                    });
                    return base;
                })

                // Remove redundancy i.e. remove all identical tuples
                .distinct()
                // Group by term
                .groupBy(Tuple2::_1)
                // Count docs per term
                .mapToPair(t -> {
                    double i = 0;
                    for (Tuple2<String, Integer> it : t._2()) {
                        i++;
                    }
                    return new Tuple2<String, Double>(t._1(), i);
                });
        terms2docs.cache();

        // Create a dictionary of all terms
        Map<String,Long> dict = terms2docs.map(Tuple2::_1).zipWithIndex().collectAsMap();

        // Filter and compute IDF
        Map<String,Double> idfs = terms2docs
                // Filter the terms that are available only in very few document
                .filter(t -> t._2() > idfThreshold)
                // Compute IDF
                .mapToPair(t -> {
                    Double idf = Math.log10(numDocs / t._2());
                    return new Tuple2<String, Double>(t._1(), idf);
                })
                .collectAsMap();

        // Create a labeled vectors
        RDD<LabeledPoint> trainingSet = data.map(d -> d.toTrainingExample(dict, idfs)).rdd();
        NaiveBayesModel model = NaiveBayes.train(trainingSet);

        // Predict
        TestDocument testDocMiddle = new TestDocument("terrible boring but great on the other hand");
        TestDocument testDocBad = new TestDocument("the worst movie ever");
        TestDocument testDocGreat = new TestDocument("pure awesome");
        double result;
        result = model.predict(testDocBad.vectorize(dict,idfs));
        System.out.println("Result for '"+testDocBad.toString()+"': "+result);
        result = model.predict(testDocMiddle.vectorize(dict,idfs));
        System.out.println("Result for '"+testDocMiddle.toString()+"': "+result);
        result = model.predict(testDocGreat.vectorize(dict,idfs));
        System.out.println("Result for '"+testDocGreat.toString()+"': "+result);

        /* Stop Spark */
        sc.stop();
    }
}
