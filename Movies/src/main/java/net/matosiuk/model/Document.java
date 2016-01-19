package net.matosiuk.model;

import org.apache.spark.mllib.linalg.Vector;
import java.util.HashSet;
import java.util.Map;

public interface Document {
    HashSet<String> getTerms();
    Vector vectorize(Map<String, Long> dict, Map<String, Double> idf);
    Double tf(String term);
}
