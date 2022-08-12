package Engine;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.jar.JarException;
import java.util.stream.Collectors;

import EnigmaMachine.*;
import EnigmaMachineException.*;
import Jaxb.Schema.Generated.*;
import TDO.MachineDetails;
import Jaxb.Schema.Generated.CTEEnigma;
import com.sun.org.apache.xpath.internal.operations.Bool;
import javafx.util.Pair;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import EnigmaMachine.RomanNumber;
import static java.lang.Integer.parseInt;
import static javafx.application.Platform.exit;


public class EngineManager implements MachineOperations, Serializable {

    //region private data members
    private EnigmaMachine enigmaMachine;
    private StatisticsAndHistoryAnalyzer statisticsAndHistoryAnalyzer;
    private final GeneralEnigmaMachineException enigmaMachineException;


    //endregion

    public EngineManager(){
        this.enigmaMachineException = new GeneralEnigmaMachineException();
        this.statisticsAndHistoryAnalyzer = new StatisticsAndHistoryAnalyzer();
        this.enigmaMachine = null;


        //endregion
    }

    //region JAXB Translation
    public void setMachineDetailsFromXmlFile(String machineDetailsXmlFilePath) throws GeneralEnigmaMachineException, JAXBException, NotXmlFileException, FileNotFoundException {
        // TODO implement here also validation.(the file exist)
        try {
            InputStream inputStream = new FileInputStream(new File(machineDetailsXmlFilePath));
            if (!machineDetailsXmlFilePath.endsWith(".xml")) {
                throw new NotXmlFileException();
            }
            CTEEnigma enigma = deserializeFrom(inputStream);
            transformJAXBClassesToEnigmaMachine(enigma);
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException();
        }catch (JAXBException e){
            throw new RuntimeException();
        }



   }

    public CTEEnigma deserializeFrom(InputStream in) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance("Jaxb.Schema.Generated");
        Unmarshaller u = jc.createUnmarshaller();
        return (CTEEnigma) u.unmarshal(in);
    }

    private void transformJAXBClassesToEnigmaMachine(CTEEnigma JAXBGeneratedEnigma) throws GeneralEnigmaMachineException {
        // TODO implement here also validation.(the file exist),exceptions. also change the init settings to false.
        List<Character> generatedABC = new ArrayList<>();
        ABCNotValidException abcNotValidException = new ABCNotValidException();
        checkIfABCIsValid(JAXBGeneratedEnigma.getCTEMachine().getABC(), abcNotValidException, generatedABC);
        abcNotValidException.addExceptionsToTheList();

        if(abcNotValidException.shouldThrowException()){
            enigmaMachineException.addException(abcNotValidException);
            throw enigmaMachineException;
        }

        List<CTERotor> CTERotors = JAXBGeneratedEnigma.getCTEMachine().getCTERotors().getCTERotor();
        List<CTEReflector> CTEReflectors = JAXBGeneratedEnigma.getCTEMachine().getCTEReflectors().getCTEReflector();
        Map<Integer,Rotor> machineRotors;
        Map<RomanNumber, Reflector> machineReflectors;
        Map<Character,Integer> machineKeyBoard;
        int rotorsInUseCounter = JAXBGeneratedEnigma.getCTEMachine().getRotorsCount();;

        machineKeyBoard = getMachineKeyboardFromCTEKeyboard(generatedABC, abcNotValidException);
        if(abcNotValidException.shouldThrowException()){
            enigmaMachineException.addException(abcNotValidException);
            throw enigmaMachineException;
        }

        machineRotors = getMachineRotorsFromCTERotors(CTERotors, generatedABC);
        machineReflectors = getMachineReflectorsFromCTEReflectors(CTEReflectors,generatedABC);

        if(!enigmaMachineException.isExceptionNeedToThrown()) {
            enigmaMachine = new EnigmaMachine(machineRotors, machineReflectors, machineKeyBoard, rotorsInUseCounter);
        }
        else {
            throw enigmaMachineException;
        }
    }

    private void checkIfABCIsValid(String abc,ABCNotValidException abcNotValidException,List<Character> generatedABC ) {
        if(abc.length() == 0) {
            abcNotValidException.setABCempty();
        }
        abc = abc.trim();
        checkIfKeyBoardContainsDuplications(abc,abcNotValidException,generatedABC);
    }

    private boolean checkIfKeyBoardContainsDuplications(String abc, ABCNotValidException abcNotValidException, List<Character> generatedABC) {
        Map<Character, Integer> abcMap = new HashMap<>();
        for(int i = 0; i < abc.length(); i++){
            if(abcMap.containsKey(abc.charAt(i))){
                return true;
            }
            else{
                abcMap.put(abc.charAt(i), 1);
            }
        }
        for(Character charInABC : abcMap.keySet()){
            if(abcMap.get(charInABC) > 1){
                abcNotValidException.addCharToDuplicateChars(charInABC);
            }
            else {
                generatedABC.add(charInABC);
            }
        }

        return false;
    }
    private Map<RomanNumber, Reflector> getMachineReflectorsFromCTEReflectors(List<CTEReflector> cteReflectors, List<Character> CTEAbc) {
        Map<RomanNumber, Reflector> machineReflectors = new HashMap<>();
        Map<String, Boolean> insertedReflectorsIds = fillReflectorMapIdsMapWithFalseValues(cteReflectors.size());

        ReflectorNotValidException reflectorNotValidException = new ReflectorNotValidException();
        if(cteReflectors.size() == 0) {
            reflectorNotValidException.setReflectorsEmpty();
        }
        else if(cteReflectors.size() > 5) {
            reflectorNotValidException.setToManyReflectors();
        }
        for(CTEReflector reflector: cteReflectors){
             if(IsReflectorIdIsValid(reflector, reflectorNotValidException) && numberOfPairsInReflectorValid(reflector,CTEAbc,reflectorNotValidException)) {
             Map<Integer,Integer> currentReflectorMapping = new HashMap<>();
             for(CTEReflect reflect:reflector.getCTEReflect()){
                 Map<Integer,Integer> outPutColMap = new HashMap<>();
                 Map<Integer,Integer> inputColMap = new HashMap<>();
                 if(indexOutOfRange(reflect.getInput(), CTEAbc)){
                     reflectorNotValidException.addReflectorIndexOutOfRange(reflector.getId(),reflect.getInput());
                 }

                 else if(indexOutOfRange(reflect.getOutput(), CTEAbc)) {
                     reflectorNotValidException.addReflectorIndexOutOfRange(reflector.getId(), reflect.getOutput());
                 }
                 // TODO check if the Length is 1 and if the character is in ABC, and that there are no duplicates of chars in each side, check that the numbers in the reflector are in the length of, and that the index map to another one.
                 if(outPutColMap.containsKey(reflect.getOutput())){
                        reflectorNotValidException.addReflectorDuplicateOutput(reflector.getId(),reflect.getOutput());
                    }
                 if(inputColMap.containsKey(reflect.getInput())){
                        reflectorNotValidException.addReflectorDuplicateInput(reflector.getId(),reflect.getInput());
                    }
                outPutColMap.put(reflect.getOutput(),1);
                inputColMap.put(reflect.getInput(),1);

                 if(reflect.getInput() == reflect.getOutput()){
                     reflectorNotValidException.addIndexMappedToHimSelf(reflect.getInput(),reflector.getId());
                 }
                 currentReflectorMapping.put(reflect.getInput(),reflect.getOutput());
                 currentReflectorMapping.put(reflect.getOutput(),reflect.getInput());
             }
             insertedReflectorsIds.put(reflector.getId(),true);
             Reflector currentReflector = new Reflector(RomanNumber.valueOf(reflector.getId()), currentReflectorMapping);
             machineReflectors.put(RomanNumber.convertStringToRomanNumber(reflector.getId()),currentReflector);}
        }
        reflectorNotValidException.checkIfReflectorsIdsInOrder(insertedReflectorsIds);
        reflectorNotValidException.addExceptionsToTheList();
        if(reflectorNotValidException.shouldThrowException()){
            enigmaMachineException.addException(reflectorNotValidException);
        }
        return machineReflectors;
    }

    private boolean indexOutOfRange(int input, List<Character> cteAbc) {
        return input > cteAbc.size();
    }

    private boolean numberOfPairsInReflectorValid(CTEReflector reflector, List<Character> CTEAbc,ReflectorNotValidException reflectorNotValidException) {
        if(reflector.getCTEReflect().size() != CTEAbc.size()/2) {
            reflectorNotValidException.setNumberOfPairsInReflectorInvalid(reflector,CTEAbc);
            return false;
        }
        return true;
    }

    private boolean IsReflectorIdIsValid(CTEReflector reflector,ReflectorNotValidException reflectorNotValidException) {
        if(!isValidRomanNumber(reflector.getId())) {
            reflectorNotValidException.addReflectorsToOutOfRangeList(reflector.getId());
            return false;
        }
        return true;
    }
    private boolean isValidRomanNumber(String romanNumberString){
        boolean isValid = false;
        for(RomanNumber romanNumber: RomanNumber.values()){
            if(romanNumber.toString().equals(romanNumberString)){
                isValid = true;
            }
        }
        return isValid;
    }
    private Map<String, Boolean> fillReflectorMapIdsMapWithFalseValues(int size){
        Map<String , Boolean> idsMap = new HashMap<>(size);
        for(RomanNumber romanNumber: RomanNumber.values()){
            idsMap.put(romanNumber.toString(),false);
        }
        return idsMap;
    }
    private Map<Integer,Boolean> fillRotorsMapWithFalseValues(int size){
        Map<Integer,Boolean> idsMap = new HashMap<>(size);
        for(int i = 0; i < size; i++){
            idsMap.put(i,false);
        }
        return idsMap;
    }

    private Map<Integer, Rotor> getMachineRotorsFromCTERotors(List<CTERotor> cteRotors, List<Character> cteABC) {
        Map<Integer,Rotor> machineRotors = new HashMap<Integer, Rotor>();
        Map<Integer,Boolean> generatedRotorsIds = fillRotorsMapWithFalseValues(cteRotors.size());
        RotorNotValidException rotorNotValidException = new RotorNotValidException();
        checkIfRotorsIdsAreValid(cteRotors, rotorNotValidException);
        if(cteRotors.size() < 2) {
            rotorNotValidException.setNumberOfRotorsToAdd(2 - cteRotors.size());
        }

        checkThatRotorsIdsAreValid(cteRotors, rotorNotValidException);
        //TODO check if rotors id are numbers, left, right from abc. length is as the length of abc and each shows once, that the notch is in the length of the abc.
        //TODO check that the rotors count which the number of rotors in use is between 2 and 99, return the rotors count to erez.
        for(CTERotor rotor: cteRotors){
            Map<Character,Character> currentRotorMap = new HashMap<>();
            if(rotor.getNotch() > cteABC.size() || rotor.getNotch() < 0){
                rotorNotValidException.addNotchOutOfRange(rotor.getId(),rotor.getNotch());
            }
            if(rotor.getCTEPositioning().size() != cteABC.size()){
                rotorNotValidException.setNumberOfPairsInRotorInvalid(rotor,cteABC);
            }

            List<Pair<Character,Character>> currentRotorPairs = new ArrayList<>();
            for(CTEPositioning position: rotor.getCTEPositioning()){

                checkIfPositionLettersInABC(position, cteABC, rotorNotValidException, rotor.getId());
                Map<Character,Integer> leftColInRotor = new HashMap<>();
                Map<Character,Integer> rightColInRotor = new HashMap<>();
                if(leftColInRotor.containsKey(position.getLeft())){
                    rotorNotValidException.addDUplicatedCharToLeftCol(rotor.getId(),position.getLeft());
                }
                if(rightColInRotor.containsKey(position.getRight())){
                    rotorNotValidException.addDUplicatedCharToRightCol(rotor.getId(),position.getRight());
                }
                leftColInRotor.put(position.getLeft().charAt(0),1);
                rightColInRotor.put(position.getRight().charAt(0),1);

                Pair<Character,Character> currentPair = new Pair<>(position.getLeft().charAt(0),position.getRight().charAt(0));
                // TODO check if the Length is 1 and if the character is in ABC, and that there are no duplicates of chars in each side where ever there are numbers, check that they are ints.
                currentRotorPairs.add(currentPair);
                currentRotorMap.put(position.getLeft().charAt(0),position.getRight().charAt(0));
            }

            rotorNotValidException.addExceptionsToTheList();
            if(rotorNotValidException.shouldThrowException()){
                enigmaMachineException.addException(rotorNotValidException);
            }
            generatedRotorsIds.put(rotor.getId() ,true);
            Rotor currentRotor = new Rotor(rotor.getId(), rotor.getNotch() - 1, currentRotorPairs);
            machineRotors.put(rotor.getId(),currentRotor);
        }
        if(rotorNotValidException.isGeneratedRotorsIdsInOrder(generatedRotorsIds)){
            enigmaMachineException.addException(rotorNotValidException.addRotorIdsNotInOrder(generatedRotorsIds));
        }
        return machineRotors;
    }



    private void checkIfRotorsIdsAreValid(List<CTERotor> cteRotors, RotorNotValidException rotorNotValidException) {
        int numberOfRotors = cteRotors.size();
        Map<Integer,Boolean> rotorsIds = new HashMap<>();
        for(CTERotor rotor: cteRotors){
            if(rotor.getId() < 0 || rotor.getId() > numberOfRotors){
                rotorNotValidException.addRotorsToOutOfRangeList(rotor.getId());
            }
            if(rotorsIds.containsKey(rotor.getId())){
                rotorNotValidException.addDuplicatedRotorId(rotor.getId());
            }
            rotorsIds.put(rotor.getId(),true);
        }

    }

    private void checkThatRotorsIdsAreValid(List<CTERotor> cteRotors, RotorNotValidException rotorNotValidException) {
        
    }

    private void checkIfPositionLettersInABC(CTEPositioning position, List<Character> cteABC, RotorNotValidException rotorNotValidException, int rotorId) {
        //TODO add length validation.
        for(Character charInAbc: cteABC){
            if(position.getLeft().length() != 1){
                rotorNotValidException.addNotValidLetter(position.getLeft(),rotorId);
            }
            if(position.getLeft().charAt(0) == charInAbc){
                break;
            }
        }
        if(position.getLeft().length() != 1){
            rotorNotValidException.addNotValidLetter(position.getLeft(),rotorId);
        }

        for(Character charInAbc: cteABC){
            if(position.getRight().length() != 1){
                rotorNotValidException.addNotValidLetter(position.getRight(),rotorId);
                break;
            }
            if(position.getRight().charAt(0) == charInAbc){
                break;
            }
        }
        if(position.getRight().length() != 1){
            rotorNotValidException.addNotValidLetter(position.getRight(),rotorId);
        }

    }

    private Map<Character, Integer> getMachineKeyboardFromCTEKeyboard(List<Character> cteKeyboard,ABCNotValidException abcNotValidException) {
        Map<Character, Integer> machineKeyBoard = new HashMap<>();
        int indexToMappingTOInKeyboard = 0;

        if (cteKeyboard.size() % 2 != 0) {
            abcNotValidException.setIsOddLength();
        }
        for (Character letter : cteKeyboard) {
            if (machineKeyBoard.containsKey(Character.toUpperCase(letter))) {
                abcNotValidException.addCharToDuplicateChars(letter);
            }
            machineKeyBoard.put(Character.toUpperCase(letter), indexToMappingTOInKeyboard);
            indexToMappingTOInKeyboard++;
            }
            return machineKeyBoard;
    }

    //endregion

    //region Operations implements
    //region set automatic settings
    @Override
    public void setSettingsAutomatically() throws RotorsInUseSettingsException, StartingPositionsOfTheRotorException, ReflectorSettingsException, PluginBoardSettingsException, SettingsFormatException, CloneNotSupportedException, MachineNotExistsException {
        RotorIDSector rotorIDSector = getRandomRotorsIdSector();
        setSettings(rotorIDSector, getRandomStartingPositionRotorsSector(rotorIDSector.getElements().size()), getRandomReflectorSector(), getRandomPluginBoardSector());
    }

    private void setSettings(RotorIDSector rotorIDSector, StartingRotorPositionSector startingPositionRotorsSector, ReflectorIdSector reflectorSector, PluginBoardSector pluginBoardSector) throws MachineNotExistsException, RotorsInUseSettingsException, StartingPositionsOfTheRotorException, ReflectorSettingsException, CloneNotSupportedException, PluginBoardSettingsException, SettingsFormatException {
        clearSettings();
        setRotorsInUse(rotorIDSector);
        setStartingPositionRotors(startingPositionRotorsSector, rotorIDSector);
        setReflectorInUse(reflectorSector);
        setPluginBoard(pluginBoardSector);
        checkIfTheSettingsInitialized();
        resetSettings();
    }

    private PluginBoardSector getRandomPluginBoardSector() {
        List<Pair<Character, Character>> pluginPairs = new ArrayList<>();
        Set<Character> optionalCharacters = new HashSet<>(enigmaMachine.getKeyboard());
        Random randomGenerator = new Random();
        int amountOfPairs = randomGenerator.nextInt(enigmaMachine.getMaximumPairs() + 1);
        Pair<Character, Character> randomPair;

        for (int i = 0; i < amountOfPairs; i++) {
            randomPair = getRandomPluginPair(optionalCharacters.stream().collect(Collectors.toList()), pluginPairs);
            pluginPairs.add(randomPair);
            optionalCharacters.remove(randomPair.getKey());
            optionalCharacters.remove(randomPair.getValue());
        }

        return new PluginBoardSector(pluginPairs);
    }

    private Pair<Character, Character> getRandomPluginPair(List<Character> optionalCharacters, List<Pair<Character, Character>> pluginPairs) {
        Character leftCharacter = getRandomCharacterFromTheKeyboard(optionalCharacters);
        optionalCharacters.remove(leftCharacter);
        Character rightCharacter = getRandomCharacterFromTheKeyboard(optionalCharacters);
        Pair<Character, Character> randomPluginPair = new Pair<>(leftCharacter, rightCharacter);

        while(!isValidPair(randomPluginPair, pluginPairs))
        {
            optionalCharacters.add(leftCharacter);
            leftCharacter = getRandomCharacterFromTheKeyboard(optionalCharacters);
            optionalCharacters.remove(leftCharacter);
            rightCharacter = getRandomCharacterFromTheKeyboard(optionalCharacters);
            randomPluginPair = new Pair<>(leftCharacter, rightCharacter);
        }

        return randomPluginPair;
    }

    private boolean isValidPair(Pair<Character, Character> randomPair, List<Pair<Character, Character>> pluginPairs) {
        if(randomPair.getKey() == randomPair.getValue()) {
            return false;
        }

        //check if left char or right char already exists in any other plugged pair
        if(pluginPairs.stream().map(Pair::getKey).collect(Collectors.toSet()).contains(randomPair.getValue()) ||
           pluginPairs.stream().map(Pair::getKey).collect(Collectors.toSet()).contains(randomPair.getKey()) ||
           pluginPairs.stream().map(Pair::getValue).collect(Collectors.toSet()).contains(randomPair.getValue()) ||
           pluginPairs.stream().map(Pair::getValue).collect(Collectors.toSet()).contains(randomPair.getKey())) {
            return false;
        }

        return true;
    }

    private ReflectorIdSector getRandomReflectorSector() {
        Random randomGenerator = new Random();
        RomanNumber[] reflectorIdArr = enigmaMachine.getAllReflectors().keySet().toArray(new RomanNumber[enigmaMachine.getAllReflectors().keySet().size()]);
        List<RomanNumber> reflectorId = new ArrayList<RomanNumber>();

        reflectorId.add(reflectorIdArr[randomGenerator.nextInt(reflectorIdArr.length)]);

        return new ReflectorIdSector(reflectorId);
    }

    private StartingRotorPositionSector getRandomStartingPositionRotorsSector(int rotorsInUseSize) {
        List<Character> startingPositionsOfTheRotors = new ArrayList<>();

        for (int i = 0; i < rotorsInUseSize; i++) {
            startingPositionsOfTheRotors.add(getRandomCharacterFromTheKeyboard(enigmaMachine.getKeyboard().stream().collect(Collectors.toList())));
        }

        return new StartingRotorPositionSector(startingPositionsOfTheRotors);
    }

    private Character getRandomCharacterFromTheKeyboard(List<Character> optionalCharacters) {
        Random randomGenerator = new Random();

        return optionalCharacters.get(randomGenerator.nextInt(optionalCharacters.size()));
    }

    private RotorIDSector getRandomRotorsIdSector() {
        List<Integer> rotorsId = new ArrayList<>();
        Set<Integer> optionalRotorsId = new HashSet<>(enigmaMachine.getAllRotors().keySet());
        Random randomGenerator = new Random();
        int amountOfRotors = randomGenerator.nextInt(enigmaMachine.getAllRotors().size()) + 1;
        Integer randomId;

        for (int i = 0; i < amountOfRotors; i++) {
            randomId =  getRandomRotorId(optionalRotorsId.stream().collect(Collectors.toList()));
            rotorsId.add(randomId);
            optionalRotorsId.remove(randomId);
        }

        return new RotorIDSector(rotorsId);
    }


    private Integer getRandomRotorId(List<Integer> optionalRotorsId) {
        Random randomGenerator = new Random();
        int randomRotorId = optionalRotorsId.get(randomGenerator.nextInt(optionalRotorsId.size()));

        while(!enigmaMachine.getAllRotors().containsKey(randomRotorId))
        {
            randomRotorId = optionalRotorsId.get(randomGenerator.nextInt(optionalRotorsId.size()));
        }

        return randomRotorId;
    }

    //endregion

    //region set Settings
    public void clearSettings() throws MachineNotExistsException {
        if(!isMachineExists()) {
            throw new MachineNotExistsException("Go back to operation 1 and then run this operation");
        }

        enigmaMachine.clearSettings();
    }
    public void checkIfTheSettingsInitialized() throws SettingsFormatException, CloneNotSupportedException {
        if(!enigmaMachine.isMachineSettingInitialized()) {
            throw new SettingsFormatException(enigmaMachine.getSettingsFormat());
        }

        statisticsAndHistoryAnalyzer.addSettingConfiguration((SettingsFormat) enigmaMachine.getSettingsFormat().clone());
    }
    public void setRotorsInUse(RotorIDSector rotorIDSector) throws RotorsInUseSettingsException {
        enigmaMachine.initializeRotorsInUseSettings(rotorIDSector);
    }

    public void setStartingPositionRotors(StartingRotorPositionSector startingPositionTheRotors, RotorIDSector rotorIDSector) throws StartingPositionsOfTheRotorException {
        enigmaMachine.setStartingPositionRotorsSettings(startingPositionTheRotors, rotorIDSector);
    }

    public void setReflectorInUse(ReflectorIdSector reflectorInUse) throws ReflectorSettingsException, CloneNotSupportedException {
        enigmaMachine.setReflectorInUseSettings(reflectorInUse);
    }

    public void setPluginBoard(PluginBoardSector pluginBoardSector) throws PluginBoardSettingsException {
        enigmaMachine.setPluginBoardSettings(pluginBoardSector);
    }
    //endregion

    @Override
    public void resetSettings() throws MachineNotExistsException, IllegalArgumentException, ReflectorSettingsException, RotorsInUseSettingsException, SettingsFormatException, StartingPositionsOfTheRotorException, CloneNotSupportedException, PluginBoardSettingsException {
        if(!isMachineExists()) {
            throw new MachineNotExistsException("Go back to operation 1 and then run this operation");
        }
        if(!enigmaMachine.isMachineSettingInitialized()) {
            throw new IllegalArgumentException("Error: The initial code configuration has not been configured for the machine, you must return to operation 3 or 4 and then return to this operation");
        }

        enigmaMachine.resetSettings();
    }

    @Override
    public MachineDetails displaySpecifications() throws MachineNotExistsException {
        if(!isMachineExists()) {
            throw new MachineNotExistsException("Go back to operation 1 and then run this operation again");
       }

       return new MachineDetails(enigmaMachine.getAllRotors(),
                                           enigmaMachine.getCurrentRotorsInUse(),
                                           enigmaMachine.getAllReflectors(),
                                           enigmaMachine.getCurrentReflectorInUse(),
                                           enigmaMachine.getKeyboard(),
                                           enigmaMachine.getPluginBoard(),
                                           statisticsAndHistoryAnalyzer.getMessagesCounter(),
                                           enigmaMachine.getSettingsFormat());
    }

    @Override
    public String analyzeHistoryAndStatistics() throws MachineNotExistsException {
        if(!isMachineExists()) {
            throw new MachineNotExistsException("Go back to operation 1 and then run this operation again");
        }

        return statisticsAndHistoryAnalyzer.toString();
    }

    @Override
    public String processInput(String inputToProcess) throws MachineNotExistsException, IllegalArgumentException {
        if(!isMachineExists()) {
            throw new MachineNotExistsException("Go back to operation 1 and then run this operation");
        }

        OriginalStringFormat originalStringFormat = new OriginalStringFormat(inputToProcess.chars().mapToObj(ch -> (char)ch).collect(Collectors.toList()));
        Instant start = Instant.now();
        String encryptedString = getProcessedInput(inputToProcess);
        Instant end = Instant.now();
        long durationEncryptedTimeInNanoSeconds = Duration.between(start, end).toNanos();
        EncryptedStringFormat encryptedStringFormat = new EncryptedStringFormat(encryptedString.chars().mapToObj(ch -> (char)ch).collect(Collectors.toList()));
        ProcessedStringsFormat processedStringsFormat = new ProcessedStringsFormat(new ArrayList<>(Arrays.asList(originalStringFormat, encryptedStringFormat)),
                durationEncryptedTimeInNanoSeconds, enigmaMachine.getSettingsFormat().getIndexFormat());
        enigmaMachine.getSettingsFormat().advanceIndexFormat();
        statisticsAndHistoryAnalyzer.addProcessedStringFormat(enigmaMachine.getSettingsFormat(), processedStringsFormat);
        statisticsAndHistoryAnalyzer.advancedMessagesCounter();

        return encryptedString;
    }

    private String getProcessedInput(String inputToProcess) throws IllegalArgumentException{
        //TODO chen: throw exception with more info: what are the illegal char and send the legal keyboard chars
        if(containsCharNotInMAMachineKeyboard(inputToProcess)){
            throw new IllegalArgumentException("The input contains char/s that are not in the machine keyboard");
        }
        String processedInput = "";
        for(char letter: inputToProcess.toCharArray()){
            processedInput += enigmaMachine.decode(letter);
        }
        return processedInput;
    }

    private boolean containsCharNotInMAMachineKeyboard(String inputToProcess) {
        for(char letter: inputToProcess.toCharArray()){
            if(!enigmaMachine.getKeyboard().contains(letter)){
                return true;
            }
        }
        return false;
    }
    public void finishSession() {
        exit();
    }
    //endregion

    public boolean isMachineExists() {
        return enigmaMachine != null;
    }

    public boolean isMachineSettingInitialized() {
        return enigmaMachine.isMachineSettingInitialized();
    }
}