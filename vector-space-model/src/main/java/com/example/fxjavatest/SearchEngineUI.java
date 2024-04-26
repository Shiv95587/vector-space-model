package com.example.fxjavatest;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.util.Pair;
import opennlp.tools.stemmer.PorterStemmer;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class SearchEngineUI extends Application {

//    Map<term, Pair<Idf, ArrayList<Pair<documentID, termFrequency>>>>
    static Map<String, Pair<Integer, ArrayList<Pair<Long, Long>>>> invertedIndex = new HashMap<>();
    // This is the vector space it will consist of all document vectors
    Map<Long, ArrayList<Pair<Float, String>>> vectorSpace = new HashMap<>();
    static long N;
    static Map<String, Long> documentIDs = new HashMap<>();
    ArrayList<String> resultSet = new ArrayList<>();
    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        // Search Field
        TextField searchField = new TextField();
        searchField.setPromptText("Enter your query");
        searchField.setMinWidth(200); // Set a minimum width to prevent resizing issues

        // Search Button with Icon
        Button searchButton = new Button();
        Image searchIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("search_icon.png")));
        ImageView searchIconView = new ImageView(searchIcon);
        searchIconView.setFitWidth(16);
        searchIconView.setFitHeight(16);
        searchButton.setGraphic(searchIconView);
        searchButton.setMinWidth(40); // Set a minimum width to prevent resizing issues


        // HBox to hold search controls
        HBox searchBox = new HBox(10);
        searchBox.getChildren().addAll(searchField, searchButton);
        HBox.setHgrow(searchField, Priority.ALWAYS); // Expand search field to fill available space

        // VBox to hold search controls and results
        VBox vbox = new VBox(20);
        vbox.setPadding(new Insets(20));
        vbox.getChildren().addAll(searchBox, createResultList());

        // Apply CSS to the result list
        vbox.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        // Add VBox to the center of the BorderPane
        root.setCenter(vbox);

        // Event handling for the search button
        searchButton.setOnAction(event -> {
            String query = searchField.getText().trim();
            System.out.println(query);
            if (!query.isEmpty())
            {
                ArrayList<Pair<Float, String>> queryVector = getQueryVector(query);
                System.out.println(queryVector);
                // retrieve documents whose cosine similarity will be calculated
                Set<Long> tempSet = new HashSet<>();
                ArrayList<String> queryTerms = new ArrayList<>(List.of(query.split(" ")));
                caseFold(queryTerms);
                stem(queryTerms);
                System.out.println("Query terms are: " + queryTerms);

                for(String queryTerm : queryTerms) {
                    if(invertedIndex.containsKey(queryTerm)) {
                        for (Pair<Long, Long> p : invertedIndex.get(queryTerm).getValue()) {
                            tempSet.add(p.getKey());
                        }
                    }
                }
                ArrayList<Long> docs = new ArrayList<>(tempSet);
                System.out.println(docs);

                for (Long doc : docs) {
                    System.out.print("DOC-ID " + doc + ":  ");
                    // System.out.println(vectorSpace.get(docs.get(i)));
                }

                // calculating cosine similarity of query vectors and docs
                ArrayList<Pair<Float, Long>> results = getResults(queryVector, docs);
                // Perform search operation and update the resultList accordingly
                updateSearchResults((ListView<String>) vbox.getChildren().get(1), results,query);
            }
            else {
                showErrorDialog("Enter a query");
            }


        });

        String path = "ResearchPapers/";
        File directory = new File(path);
        File[] files = directory.listFiles();

        // No of documents in the collection
        assert files != null;
        N = files.length;

        ArrayList<String> stopWords = new ArrayList<>();
        // Reading stopwords
        String fileName = "Stopword-List.txt"; // Replace with your file name
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Process each line here
                stopWords.add(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        }
        System.out.println("Stop words are: ");
        System.out.println(stopWords);

        if (files != null) {
            //  Reads the files and apply preprocessing techniques tokenizing,casefolding, stemming
            preprocess(path, files,stopWords);

            invertedIndex.forEach((token, value) -> {
                Integer df = value.getKey();
                ArrayList<Pair<Long, Long>> docs = value.getValue();
                for(Pair<Long,Long> pr : docs)
                {
                    Long docId =  pr.getKey();
                    Long tf = pr.getValue();

                    if(!vectorSpace.containsKey(docId))
                    {
                        ArrayList<Pair<Float, String>> docVector = new ArrayList<>();
                        docVector.add(new Pair<>(tf * calcInverseDocumentFrequency(N, df), token));
                        vectorSpace.put(docId, docVector);
                    }
                    else {
                       vectorSpace.get(docId).add(new Pair<>(tf * calcInverseDocumentFrequency(N, df),token));
                    }
                }
            });

            vectorSpace.forEach((key,val)->{
                vectorSpace.put(key, normalize(vectorSpace.get(key)));
            });
        }
        Scene scene = new Scene(root, 600, 400);
        primaryStage.setTitle("Vector Space Model");
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public ArrayList<Pair<Float, Long>> getResults(ArrayList<Pair<Float, String>> queryVector, ArrayList<Long> docs)
    {
        // threshold value
        float alpha = 0.03F;
        ArrayList<Pair<Float, Long>> resultSet = new ArrayList<Pair<Float,Long>>();
        for(Long doc : docs) {
            System.out.print("DOC-ID " + doc + ": ");
            float score = calcCosineScore(queryVector, vectorSpace.get(doc));
            System.out.println("score: " + score);
            if (score > alpha)
            {
                resultSet.add(new Pair<>(score,doc));
            }
            // Sort the ArrayList by the first value of each pair in descending order using lambda expression
            resultSet.sort((pair1, pair2) -> pair2.getKey().compareTo(pair1.getKey()));
            System.out.println(resultSet);
        }
        return resultSet;
    }



    public ArrayList<Pair<Float,String>> getQueryVector(String query) {
        Map<String, Long> queryfreq = new HashMap<>();
        ArrayList<String> queryTerms = new ArrayList<>(List.of(query.split(" ")));
        // Preprocessing query
        caseFold(queryTerms);
        stem(queryTerms);
        // calculating term frequencies
        for (String s : queryTerms) {
            if (queryfreq.containsKey(s)) {
                queryfreq.put(s, queryfreq.get(s) + 1);
            } else {
                queryfreq.put(s, 1L);
            }
        }
        // creating query vector using tf-idf scheme
        ArrayList<Pair<Float, String>> queryVector = new ArrayList<>();
        queryfreq.forEach((key,value)->{
            float idf = 0;
            if(invertedIndex.containsKey(key)) {
                int df = invertedIndex.get(key).getKey();
                idf = (float) Math.log(N/(float)df);
            }
            queryVector.add(new Pair<>((float)value*idf, key));
        });
        // length normalizing the query vector
        normalize(queryVector);
        return queryVector;
    }

    // length normalization of a vector
    public ArrayList<Pair<Float, String>> normalize(ArrayList<Pair<Float, String>> pairs) {
        float sum = 0;
        for(Pair<Float, String> p : pairs)
        {
            float v = p.getKey() * p.getKey();
            sum += v;
        }
        float len = (float) Math.sqrt(sum);
        for(int i = 0; i < pairs.size(); ++i)
        {
            pairs.set(i, new Pair<>(pairs.get(i).getKey() / len, pairs.get(i).getValue()));
        }
        return pairs;
    }

    // Function to calculate the cosine similarity between query vector (q) and document vector (d)
    public float calcCosineScore(ArrayList<Pair<Float, String>> q, ArrayList<Pair<Float, String>> d)
    {
        float score = 0;
        for(Pair<Float,String> p1 : q)
        {
            for(Pair<Float, String> p2: d)
            {
                if(p1.getValue().equals(p2.getValue()))
                {
                    score += p1.getKey() * p2.getKey();
                    break;
                }
            }
        }
        return score;
    }
    // Create and configure the ListView for search results
    private ListView<String> createResultList() {
        ListView<String> resultList = new ListView<>();
        resultList.setPrefHeight(300); // Set preferred height
        resultList.setStyle("-fx-font-size: 14px;"); // Increase font size for better readability
        return resultList;
    }

    // method to display search results
    private void updateSearchResults(ListView<String> resultList,ArrayList<Pair<Float, Long>> results,String query) {
        System.out.println(results);
        // Clear existing results
        resultList.getItems().clear();

        // Add search results
        if (results.isEmpty())
        {
            resultList.getItems().add("Your search '" + query + "' did not match any documents");
        }
        else
        {
            for (Pair<Float, Long> result : results)
            {
                resultList.getItems().add(String.valueOf(result.getValue()));
            }
        }
    }


    // Reads the files one by files and insert them to inverted index
    private static void preprocess(String path, File[] files, ArrayList<String> stopWords) {
        for (File file : files) {
            Map<String, Long> freq = new HashMap<>();
            if (file.isFile()) {
                String s = file.getName().split("\\.")[0];
                documentIDs.put(file.getName(), Long.parseLong(s));
                ArrayList<String> fileTokens = new ArrayList<>();
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        ArrayList<String> lineTokens = new ArrayList<>(Arrays.asList(line.split("[^a-zA-Z]+")));
                        fileTokens.addAll(lineTokens);
                    }
                    caseFold(fileTokens);
                    fileTokens.removeAll(stopWords);
                    stem(fileTokens);
                    long d = documentIDs.get(file.getName());

                    for(String token : fileTokens)
                    {
                        if(freq.containsKey(token))
                        {
                           freq.put(token, freq.get(token) + 1);
                        }
                        else {
                            freq.put(token, 1L);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                // Removing duplicate tokens
                Set<String> unique = new HashSet<>(fileTokens);
                fileTokens = new ArrayList<>(unique);
                for(String token : fileTokens)
                {
                    long docId = documentIDs.get(file.getName());
                    if(invertedIndex.containsKey(token))
                    {
                        Integer key = invertedIndex.get(token).getKey();
                        ArrayList<Pair<Long, Long>> value = invertedIndex.get(token).getValue();
                        key++;
                        value.add(new Pair<>(docId, freq.get(token)));
                        invertedIndex.put(token, new Pair<>(key, value));
                    }
                    else {
                        ArrayList<Pair<Long, Long>> temp = new ArrayList<>();
                        temp.add(new Pair<>(docId, freq.get(token)));
                        invertedIndex.put(token, new Pair<>(1, temp));
                    }
                }
            }
        }
    }


    // function to calculate inverse document frequency
    private Float calcInverseDocumentFrequency(Long N, Integer documentFrequency)
    {
        return (float) Math.log10(N/(float) documentFrequency);
    }
    // Preprocessing methods

    // stems the list of tokens using porter stemmer
    private static void stem(ArrayList<String> tokens) {
        PorterStemmer stemmer = new PorterStemmer();
        for (int i = 0; i < tokens.size(); ++i) {
            String token = tokens.get(i);
            tokens.set(i, stemmer.stem(token));
        }
    }

    // lowercase list of tokens
    private static void caseFold(ArrayList<String> tokens) {
        for (int i = 0; i < tokens.size(); ++i) {
            String token = tokens.get(i);
            tokens.set(i, token.toLowerCase());
        }
    }
    // end preprocessing methods

    // Function to show an error dialog
    public static void showErrorDialog(String contentText) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error Dialog");
        alert.setHeaderText(null);
        alert.setContentText(contentText);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
