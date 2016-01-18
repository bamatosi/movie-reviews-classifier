package net.matosiuk.model;

import org.apache.spark.mllib.linalg.*;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.regression.LabeledPoint;

import java.util.*;

public class TrainingDocument implements Document {
    private int docId;
    private String document;
    private String classLabel;
    private HashSet<String> terms;

    public TrainingDocument(int docId, String document, String classLabel) {
        this.docId = docId;
        this.document = document;
        this.classLabel = classLabel;
        this.terms = new HashSet<String>(Arrays.asList(document.toLowerCase()
                .replaceAll("[^a-z0-9 ]+", "")
                .replaceAll("^\"", "")
                .replaceAll("$\"", "")
                .split(" ")));
    }

    public int getDocId(){
        return docId;
    }

    public String getClassLabel() {
        return classLabel;
    }

    public HashSet<String> getTerms() {
        return terms;
    }

    public Vector vectorize(Map<String,Long> dict, Map<String,Double> dictTF, Map<String,Double> dictIDF) {
        HashMap<Integer,Double> terms = new HashMap<Integer,Double>();
        System.out.println("Vectorize "+docId+"("+classLabel+")");
        for (String term : getTerms()) {
            // Long coming from Spark's zipWithIndex needs to be casted to Integer in here because of SparseVector requirements
            System.out.println("\t "+term+" ("+Math.toIntExact(dict.get(term))+") -> "+dictTF.get(term)+", "+dictIDF.get(term)+"="+dictTF.get(term)*dictIDF.get(term));
            terms.put(Math.toIntExact(dict.get(term)), dictTF.get(term)*dictIDF.get(term));
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

    public LabeledPoint toTrainingExample(Map<String,Long> dict, Map<String,Double> dictTF, Map<String,Double> dictIDF) {
        // Return a LabeledPoint containing a document class and SparseVector with TF-IDF product for a terms available in the specified document
        return new LabeledPoint(Double.valueOf(getClassLabel()), vectorize(dict, dictTF, dictIDF));
    }

    @Override
    public String toString() {
        return "TrainingDocument{" +
                "docId=" + docId +
                ", classLabel='" + classLabel + '\'' +
                ", terms=" + terms +
                ", document='" + document + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrainingDocument document = (TrainingDocument) o;

        if (docId != document.docId) return false;
        return !(terms != null ? !terms.equals(document.terms) : document.terms != null);

    }

    @Override
    public int hashCode() {
        int result = docId;
        result = 31 * result + (terms != null ? terms.hashCode() : 0);
        return result;
    }
}
