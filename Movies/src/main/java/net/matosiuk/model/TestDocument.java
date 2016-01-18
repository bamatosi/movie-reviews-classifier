package net.matosiuk.model;

import org.apache.spark.mllib.linalg.SparseVector;
import org.apache.spark.mllib.linalg.Vector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class TestDocument implements Document{
    private String document;
    private HashSet<String> terms;

    public TestDocument(String document) {
        this.document = document;
        this.terms = new HashSet<String>(Arrays.asList(document.toLowerCase()
                .replaceAll("[^a-z0-9 ]+", "")
                .replaceAll("^\"", "")
                .replaceAll("$\"", "")
                .split(" ")));
    }

    public HashSet<String> getTerms() {
        return terms;
    }

    public Vector vectorize(Map<String,Long> dict, Map<String,Double> dictTF, Map<String,Double> dictIDF) {
        HashMap<Integer,Double> terms = new HashMap<Integer,Double>();
        System.out.println("Vectorize test doc: ");
        for (String term : getTerms()) {
            // Long coming from Spark's zipWithIndex needs to be casted to Integer in here because of SparseVector requirements
            if (dict.containsKey(term)) {
                terms.put(Math.toIntExact(dict.get(term)), dictTF.get(term) * dictIDF.get(term));
                System.out.println("\t "+term+" ("+Math.toIntExact(dict.get(term))+") -> "+dictTF.get(term)+", "+dictIDF.get(term)+"="+dictTF.get(term)*dictIDF.get(term));
            } else {
                System.out.println("Term "+term+" is not known in the model");
            }
        }

        // Convert ArrayList to primitives array required by SparseVector
        int[] termsIdsP = new int[terms.size()];
        double[] termsValsP = new double[terms.size()];
        int i=0;
        for(Map.Entry<Integer,Double> e : terms.entrySet()) {
            termsIdsP[i] = e.getKey();
            termsValsP[i] = e.getValue();
            i++;
        }

        // Return a SparseVector with TF-IDF product for a terms available in the specified document
        return new SparseVector(dict.size(), termsIdsP, termsValsP);
    }

    @Override
    public String toString() {
        return "TestDocument{" +
                ", terms=" + terms +
                ", document='" + document + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestDocument document = (TestDocument) o;

        return !(terms != null ? !terms.equals(document.terms) : document.terms != null);

    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (terms != null ? terms.hashCode() : 0);
        return result;
    }
}
