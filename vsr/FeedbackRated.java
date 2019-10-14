package ir.vsr;

import java.io.*;
import java.util.*;
import java.lang.*;

import ir.utilities.*;
import ir.vsr.DocumentReference;

/**
 * Gets and stores information about relevance feedback from the user and
 * computes an updated query based on original query and retrieved documents
 * that are rated relevant and irrelevant.
 *
 * @author Ray Mooney
 */

public class FeedbackRated extends Feedback {
    /**
     * The list of DocumentReference's that were rated relevant
     */
    public HashMap<DocumentReference, Double> goodDocRefs = new HashMap<DocumentReference, Double>();
    /**
     * The list of DocumentReference's that were rated irrelevant
     */
    public HashMap<DocumentReference, Double> badDocRefs = new HashMap<DocumentReference, Double>();

    /**
     * Create a feedback object for this query with initial retrievals to be rated
     */
    public FeedbackRated(HashMapVector queryVector, Retrieval[] retrievals, InvertedIndex invertedIndex) {
        super(queryVector, retrievals, invertedIndex);
    }

    /**
     * Add a document to the list of those deemed relevant
     */
    public void addGood(DocumentReference docRef, double rating) {
        goodDocRefs.put(docRef, rating);
    }

    /**
     * Add a document to the list of those deemed irrelevant
     */
    public void addBad(DocumentReference docRef, double rating) {
        badDocRefs.put(docRef, rating);
    }

    /**
     * Prompt the user for feedback on this numbered retrieval
     */
    @Override
    public void getFeedback(int showNumber) {
        // Get the docRef for this document (remember showNumber starts at 1 and is 1
        // greater than array index)
        DocumentReference docRef = retrievals[showNumber - 1].docRef;
        String response = UserInput.prompt("Is document #" + showNumber + ":" + docRef.file.getName()
                + " relevant (enter a number between -1 and 1 where -1: very irrelevant, 0: unsure, +1: very relevant)?: ");
        double responseValue = Double.parseDouble(response);
        if (responseValue >= 0.0)
            addGood(docRef, responseValue);
        else if (responseValue < 0.0)
            addBad(docRef, responseValue);
        else if (!response.equals("u"))
            getFeedback(showNumber);
    }

    /**
     * Has the user rated any documents yet?
     */
    @Override
    public boolean isEmpty() {
        if (goodDocRefs.isEmpty() && badDocRefs.isEmpty())
            return true;
        else
            return false;
    }

    /**
     * Has the user already provided feedback on this numbered retrieval?
     */
    @Override
    public boolean haveFeedback(int showNumber) {
        // Get the docRef for this document (remember showNumber starts at 1 and is 1
        // greater than array index)
        DocumentReference docRef = retrievals[showNumber - 1].docRef;
        if (goodDocRefs.containsKey(docRef) || badDocRefs.containsKey(docRef))
            return true;
        else
            return false;
    }

    /**
     * Use the Ide_regular algorithm to compute a new revised query.
     *
     * @return The revised query vector.
     */
    @Override
    public HashMapVector newQuery() {
        // Start the query as a copy of the original
        HashMapVector newQuery = queryVector.copy();
        // Normalize query by maximum token frequency and multiply by alpha
        newQuery.multiply(ALPHA / newQuery.maxWeight());
        // Add in the vector for each of the positively rated documents
        for (Map.Entry<DocumentReference, Double> entry : goodDocRefs.entrySet()) {
            // Get the document vector for this positive document
            Document doc = entry.getKey().getDocument(invertedIndex.docType, invertedIndex.stem);
            HashMapVector vector = doc.hashMapVector();
            // Multiply positive docs by beta and feedback value and normalize by max token
            // frequency
            vector.multiply(entry.getValue() * (BETA / vector.maxWeight()));
            // Add it to the new query vector
            newQuery.add(vector);
        }
        // Subtract the vector for each of the negatively rated documents
        for (Map.Entry<DocumentReference, Double> entry : badDocRefs.entrySet()) {
            // Get the document vector for this negative document
            Document doc = entry.getKey().getDocument(invertedIndex.docType, invertedIndex.stem);
            HashMapVector vector = doc.hashMapVector();
            // Multiply negative docs by beta and normalize by max token frequency
            vector.multiply(entry.getValue() * (GAMMA / vector.maxWeight()));
            // Subtract it from the new query vector
            newQuery.subtract(vector);
        }
        return newQuery;
    }

}
