package com.sunny;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.Enumeration;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
/*
Author: vknow360 aka Sunny Gupta (https://sunnythedeveloper.xyz)
Version: 1.2
*/
public class DocsGenerator extends Application {
    public Scene scene;
    public File selectedFile;
    public Label label;
    public Stage primaryStage;
    public FlowPane flowPane;
    public Label result;
    public ProgressBar progress;
    public StringBuilder builder = new StringBuilder();
    public Button generate;
    public Button choose;
    public boolean genFile = false;
    public Hyperlink about;
    public Pane pane;

    public void showAbout(){
        try {
            Desktop desktop = Desktop.getDesktop();
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)){
                desktop.browse(URI.create("https://github.com/vknow360/DocsGenerator"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void checkChanged(){
        genFile = !genFile;
    }
    public void chooseFile(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose aix file");
        selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile != null) {
            label.setText(selectedFile.getAbsolutePath());
        }
    }
    public void showAlert(String content,String title){
        Alert alert = new Alert(Alert.AlertType.NONE,content, ButtonType.OK);
        if (title != null) {
            alert.setTitle(title);
        }
        alert.setResizable(false);
        try {
            ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image(Objects.requireNonNull(getClass().getResource("/icon.png")).toString()));
        }catch (Exception e){
            e.printStackTrace();
        }
        alert.show();
    }
    public void generateDocs(){
        if (!label.getText().startsWith("No file chosen")) {
            new DocsTask().start();
        }else{
            showAlert("Please choose an aix file!",null);
        }
    }
    public void onDragOv(DragEvent event){
        if (event.getDragboard().hasFiles()) {
            if (!event.getDragboard().getFiles().get(0).getAbsolutePath().endsWith(".aix")){
                //lets ignore it
                event.consume();
                return;
            }else {
                event.acceptTransferModes(TransferMode.LINK);
            }
        }
        event.consume();
    }
    public void onDragDrop(DragEvent event){
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            success = event.getDragboard().getFiles().get(0).getAbsolutePath().endsWith(".aix");
            System.out.println("Is aix file: " +success);
            if (success) {
                File file = db.getFiles().get(0);
                label.setText(file.getAbsolutePath());
            }
        }
        event.setDropCompleted(success);
        event.consume();
    }
    @Override
    public void start(Stage primaryStage) throws Exception{
        this.primaryStage = primaryStage;
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("sample.fxml")));
        primaryStage.getIcons().add(new Image(getClass().getResource("/icon.png").toString()));
        primaryStage.setResizable(false);
        primaryStage.setTitle("DocsGenerator Tool");
        scene = new Scene(root, 500, 350);
        primaryStage.setScene(scene);
        primaryStage.setFullScreen(false);
        primaryStage.show();
    }
    public class DocsTask extends Thread{
        public JSONArray array;
        public DocsTask(){}
        @Override
        public void run() {
            flowPane.setVisible(true);
            try {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        result.setText("Reading aix file");
                        progress.setProgress(0.1);
                    }
                });
                ZipFile file = new ZipFile(label.getText());
                Enumeration<? extends ZipEntry> entries = file.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if(entry.getName().endsWith("components.json")){
                        String newLine = System.getProperty("line.separator");
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(file.getInputStream(entry)));
                        StringBuilder result = new StringBuilder();
                        for (String line; (line = reader.readLine()) != null; ) {
                            if (result.length() > 0) {
                                result.append(newLine);
                            }
                            result.append(line);
                        }
                        array = (JSONArray) new JSONParser().parse(result.toString());
                        //text = result.toString();
                        //System.out.println(text);
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                DocsGenerator.this.result.setText("Parsing json file");
                                progress.setProgress(0.30);
                            }
                        });
                        break;
                    }
                }
                for (int i = 0;i < array.size();i++) {
                    builder.append("> <h2> Docs for: ").append(((JSONObject) array.get(i)).get("name")).append("</h2> \n\n");
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            result.setText("Reading event blocks");
                            progress.setProgress(0.50);
                        }
                    });
                    addEvents(i);
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            result.setText("Reading function blocks");
                            progress.setProgress(0.70);
                        }
                    });
                    addMethods(i);
                    Thread.sleep(500);
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            result.setText("Reading property blocks");
                            progress.setProgress(0.85);
                        }
                    });
                    addProperties(i);
                    Thread.sleep(500);
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            result.setText("Finalizing docs");
                            progress.setProgress(1.00);
                        }
                    });
                    Thread.sleep(500);
                }

                if (genFile){
                    File docsFile = new File(label.getText().replaceAll(".aix","-docs.txt"));
                    System.out.println(docsFile.toString());
                    FileOutputStream fos = new FileOutputStream(docsFile);
                    OutputStreamWriter writer = new OutputStreamWriter(fos);
                    writer.write(builder.toString());
                    writer.flush();
                    writer.close();
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            showAlert("Docs generated!", null);
                        }
                    });
                }else {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            final Clipboard clipboard = Clipboard.getSystemClipboard();
                            final ClipboardContent content = new ClipboardContent();
                            content.putString(builder.toString());
                            clipboard.setContent(content);
                            showAlert("Docs copied to clipboard!", null);
                        }
                    });
                }
            }catch (Exception e){
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        showAlert(e.toString(),"An error occurred!");
                    }
                });
                e.printStackTrace();
            }finally {
                flowPane.setVisible(false);
            }
        }
        public void addEvents(int index) throws ParseException {
            JSONObject object = (JSONObject) array.get(index);
            JSONArray array = (JSONArray) object.get("events");
            if (!array.isEmpty()) {
                builder.append("> <h2> Events </h2> \n\n ");
                for (Object item : array) {
                    JSONObject ob = (JSONObject) item;
                    builder.append("> <h3>");
                    builder.append(ob.get("name"));
                    builder.append("</h3>");
                    builder.append(ob.get("description"));
                    JSONArray arr = (JSONArray) ob.get("params");
                    if (!arr.isEmpty()) {
                        builder.append("\n" +
                                "Params           |  []()       \n" +
                                "---------------- | ------- \n" +
                                "\n");
                        for (Object value : arr) {
                            JSONObject o = (JSONObject) value;
                            builder.append("```` ").append(o.get("name")).append(" | ");
                            builder.append(o.get("type")).append("````\n");
                        }
                    }
                    builder.append("\n ____________________________________\n\n");
                }
            }
        }
        public void addMethods(int index) throws ParseException {
            JSONObject object = (JSONObject)array.get(index);
            JSONArray array = (JSONArray) object.get("methods");
            if (!array.isEmpty()) {
                builder.append("> <h2> Methods </h2> \n\n ");
                for (Object item : array) {
                    JSONObject ob = (JSONObject) item;
                    builder.append("> <h3>");
                    builder.append(ob.get("name"));
                    builder.append("</h3>");
                    builder.append(ob.get("description"));
                    JSONArray arr = (JSONArray) ob.get("params");
                    if (!arr.isEmpty()) {
                        builder.append("\n" +
                                "Params           |  []()       \n" +
                                "---------------- | ------- \n" +
                                "\n");
                        for (Object value : arr) {
                            JSONObject o = (JSONObject) value;
                            builder.append("```` ").append(o.get("name")).append(" | ");
                            builder.append(o.get("type")).append("````<br>\n");
                        }
                    }
                    if (ob.containsKey("returnType")){
                        builder.append("\n<i>Return type : ").append(ob.get("returnType")).append("</i>\n");
                    }
                    builder.append("\n____________________________________\n\n");
                }
            }
        }
        public void addProperties(int index) throws ParseException {
            JSONObject object = (JSONObject)array.get(index);
            JSONArray array = (JSONArray) object.get("blockProperties");
            if (!array.isEmpty()) {
                builder.append("> <h2> Properties </h2> \n\n ");
                for (Object item : array) {
                    JSONObject ob = (JSONObject) item;
                    builder.append("> <h3>");
                    builder.append(ob.get("name"));
                    builder.append("</h3>");
                    builder.append(ob.get("description"));
                    if (ob.containsKey("rw")){
                        builder.append("\n<i>Property Type : ").append(ob.get("rw")).append("</i>");
                    }
                    if (ob.containsKey("type")){
                        builder.append("<br><i>Accepts : ").append(ob.get("type")).append("</i>");
                    }
                    builder.append("\n____________________________________\n\n");
                }
            }
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}
