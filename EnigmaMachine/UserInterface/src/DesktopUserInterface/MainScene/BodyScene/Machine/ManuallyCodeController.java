package DesktopUserInterface.MainScene.BodyScene.Machine;

import DTO.MachineDetails;
import DesktopUserInterface.MainScene.ErrorDialog;
import Engine.*;
import EnigmaMachine.RomanNumber;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ManuallyCodeController {
    @FXML private Button setCodeButton;
    @FXML private HBox RotorsId;
    @FXML private HBox StartingPositions;
    @FXML private ChoiceBox<RomanNumber> reflectorChoiceBox;
    private CodeCalibrationController codeCalibrationController;
    private MachineDetails machineDetails;
    @FXML private ScrollPane pluginBoardArea;
    @FXML private ScrollPane rotorsIdArea;
    @FXML private ScrollPane startingPositionsArea;
    @FXML private ScrollPane reflectorArea;
    private ScrollPane currentScrollPaneToDisplay;
    @FXML private ChoiceBox<Character> leftPluggedPairChoiceBox;

    @FXML private ChoiceBox<Character> rightPluggedPairChoiceBox;

    @FXML private Button addPluginPairButton;

    @FXML private Button deletePluginPairButton;

    @FXML private TableView<PluggedPair> pluggedPairsTable;

    @FXML private TableColumn<PluggedPair, Character> firstCharColumn;

    @FXML private TableColumn<PluggedPair, Character> secondCharColumn;

    private List<Sector> codeConfigurationSectors;

    public void initialize() {
        pluginBoardArea.setVisible(false);
        rotorsIdArea.setVisible(false);
        startingPositionsArea.setVisible(false);
        reflectorArea.setVisible(false);
        currentScrollPaneToDisplay = rotorsIdArea;
        currentScrollPaneToDisplay.setVisible(true);

        initializePluginPairsTable();
        codeConfigurationSectors = new ArrayList<>();
    }

    private void initializePluginPairsTable() {
        firstCharColumn.setCellValueFactory(new PropertyValueFactory<PluggedPair, Character>("firstCharacter"));
        secondCharColumn.setCellValueFactory(new PropertyValueFactory<PluggedPair, Character>("secondCharacter"));
    }

    @FXML void OnPluginBoardButtonClicked(ActionEvent event) {
        currentScrollPaneToDisplay.setVisible(false);
        pluginBoardArea.setVisible(true);
        currentScrollPaneToDisplay = pluginBoardArea;
    }
    @FXML void OnReflectorButtonClicked(ActionEvent event) {
        currentScrollPaneToDisplay.setVisible(false);
        reflectorArea.setVisible(true);
        currentScrollPaneToDisplay = reflectorArea;
    }

    @FXML void OnRotorIdButtonClicked(ActionEvent event) {
        currentScrollPaneToDisplay.setVisible(false);
        rotorsIdArea.setVisible(true);
        currentScrollPaneToDisplay = rotorsIdArea;
    }
    @FXML void OnStartingPositionsButtonClicked(ActionEvent event) {
        currentScrollPaneToDisplay.setVisible(false);
        startingPositionsArea.setVisible(true);
        currentScrollPaneToDisplay = startingPositionsArea;
    }

    @FXML void OnSetCodeButtonClicked(ActionEvent event) {
        translateFromControllersToSectors();

        try {
            codeCalibrationController.codeConfigurationSetted(codeConfigurationSectors);
            codeCalibrationController.codeConfigurationSetSuccessfully();
        }
        catch (Exception e) {
            new ErrorDialog(e, "Failed to initialize the code configuration manually");
        }
    }

    private void translateFromControllersToSectors() {
        addRotorsIdSector();
        addRotorsStartingPositionsSector();
        addReflectorIdSector();
        addPluginBoardSector();
    }

    private void addPluginBoardSector() {
        List pluginPairs = pluggedPairsTable.getItems().stream()
                .map(pluggedPair -> new Pair(pluggedPair.getFirstCharacter(), pluggedPair.getSecondCharacter()))
                .collect(Collectors.toList());

        codeConfigurationSectors.add(new PluginBoardSector(pluginPairs));
    }

    private void addReflectorIdSector() {
        if (reflectorChoiceBox.getValue() != null) {
            codeConfigurationSectors.add(new ReflectorIdSector(Arrays.asList(reflectorChoiceBox.getValue())));
        }
        else {
            codeConfigurationSectors.add(new ReflectorIdSector(new ArrayList<>()));
        }
    }

    private void addRotorsStartingPositionsSector() {
        List<Character> rotorStartingPosition = StartingPositions.getChildren().stream().filter(node -> node instanceof VBox)
                                                                               .map(vbox -> (VBox)vbox)
                                                                               .flatMap(vbox -> vbox.getChildren().stream())
                                                                               .filter(node -> node instanceof ChoiceBox)
                                                                               .map(node -> (ChoiceBox<Character>)node)
                                                                               .filter(choiceBox -> choiceBox.getValue() != null)
                                                                               .map(ChoiceBox::getValue)
                                                                               .collect(Collectors.toList());

        Collections.reverse(rotorStartingPosition);
        codeConfigurationSectors.add(new StartingRotorPositionSector(rotorStartingPosition));
    }

    private void addRotorsIdSector() {
        List<Integer> rotorsId = RotorsId.getChildren().stream().filter(node -> node instanceof VBox)
                                                       .map(vbox -> (VBox)vbox)
                                                       .flatMap(vbox -> vbox.getChildren().stream())
                                                       .filter(node -> node instanceof ChoiceBox)
                                                       .map(node -> (ChoiceBox<Integer>)node)
                                                       .filter(choiceBox -> choiceBox.getValue() != null)
                                                       .map(ChoiceBox::getValue)
                                                       .collect(Collectors.toList());


        Collections.reverse(rotorsId);
        codeConfigurationSectors.add(new RotorIDSector(rotorsId));
    }

    @FXML
    void OnClearButtonClicked(ActionEvent event) {
        reflectorChoiceBox.valueProperty().set(null);
        pluggedPairsTable.getItems().clear();
        leftPluggedPairChoiceBox.setValue(null);
        rightPluggedPairChoiceBox.setValue(null);

        clearChoiceBoxList(RotorsId.getChildren());
        clearChoiceBoxList(StartingPositions.getChildren());
    }

    private void clearChoiceBoxList(ObservableList observableList) {
        for(Object object : observableList) {
            if(object instanceof VBox) {
                for(Node node : ((VBox) object).getChildren()) {
                    if (node instanceof ChoiceBox) {
                        ChoiceBox choiceBox = (ChoiceBox) node;
                        choiceBox.setValue(null);
                    }
                }
            }
        }
    }

    @FXML void OnAddPluginPairButtonClicked(ActionEvent event) {
         if(leftPluggedPairChoiceBox.getValue() != null && rightPluggedPairChoiceBox.getValue() != null ) {
             PluggedPair pluggedPair = new PluggedPair(leftPluggedPairChoiceBox.getValue(), rightPluggedPairChoiceBox.getValue());
             ObservableList<PluggedPair> pluggedPairs = pluggedPairsTable.getItems();
             pluggedPairs.add(pluggedPair);
             pluggedPairsTable.setItems(pluggedPairs);
         }
         else {
             new ErrorDialog(new Exception("Error: you need to select 2 characters exactly in order to add plugin pair to the plugin board"), "Failed to add plugin pair");
         }
    }
    @FXML void OnDeletePluginPairButtonClicked(ActionEvent event) {
        pluggedPairsTable.getItems().removeAll(pluggedPairsTable.getSelectionModel().getSelectedItems());
    }

    public void initializeControls()  {
        VBox rotorIdVBox;
        VBox rotorStartingPositionVBox;
        Label label;
        ChoiceBox<Integer> choiceBox;

        for (int i = 0; i < machineDetails.getAmountCurrentRotorsInUse(); i++) {
            rotorIdVBox = new VBox();
            label = new Label("Rotor id: " + machineDetails.getAllRotorsId().get(i).toString());
            label.setWrapText(true);
            rotorIdVBox.getChildren().add(label);
            choiceBox = new ChoiceBox<Integer>(FXCollections.observableArrayList(machineDetails.getAllRotorsId()));
            choiceBox.setPrefWidth(label.getPrefWidth());
            rotorIdVBox.getChildren().add(choiceBox);
            RotorsId.getChildren().add(rotorIdVBox);

            rotorStartingPositionVBox = new VBox();
            rotorStartingPositionVBox.getChildren().add(label);
            rotorStartingPositionVBox.getChildren().add(new ChoiceBox<Character>(FXCollections.observableArrayList(machineDetails.getKeyboardCharacters())));
            StartingPositions.getChildren().add(rotorStartingPositionVBox);
        }

        reflectorChoiceBox.setItems(FXCollections.observableArrayList(machineDetails.getAllReflectorsId()));
        leftPluggedPairChoiceBox.setItems(FXCollections.observableArrayList(machineDetails.getKeyboardCharacters()));
        rightPluggedPairChoiceBox.setItems(FXCollections.observableArrayList(machineDetails.getKeyboardCharacters()));
    }

    public void setCodeCalibrationController(CodeCalibrationController codeCalibrationController) {
        this.codeCalibrationController = codeCalibrationController;
    }

    public void setMachineDetails(MachineDetails machineDetails) {
        this.machineDetails = machineDetails;
    }
}
