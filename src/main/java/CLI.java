import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represent the Command Line Interface
 *
 * @author Davide Iaci
 * @version 1.0
 * @see <a href="http://mabe02.github.io/lanterna/apidocs/3.1/overview-summary.html">Lanterna</a>
 */
public class CLI {

    // terminal component
    private Terminal terminal = null;
    public Screen screen = null;
    private TextGraphics textGraphics = null;
    private boolean isRunning = false; // true if the terminal is running

    // structure of the prompt (part before user input)
    public String promptStructure = "";
    private final String userInformation = System.getProperty("user.name") + "@localhost";
    public String location = "~";

    private StringBuffer terminalBuffer = new StringBuffer();
    private ArrayList<String> history = new ArrayList<>(10);
    private int historyIndex = 0;

    // command executor
    private Command commandExecutor = new Command(this);


    public CLI() {
        try {
            startTerminal();
            onTerminalRun();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startTerminal() throws IOException {
        //  auto-detection mechanism for figuring out which terminal implementation to create based on characteristics of the system the program is running on
        DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory();

        // creazione del terminale
        terminal = defaultTerminalFactory.createTerminal(); // instantiates a Terminal according to the factory implementation.
        screen = new TerminalScreen(terminal); // instantiates the layer Screen
        textGraphics = screen.newTextGraphics(); // interface that exposes functionality to 'draw' text graphics on a section of the terminal

        isRunning = true; // terminal is running

        screen.startScreen(); // start the screen
        screen.setCursorPosition(new TerminalPosition(0, 2)); // set cursor position

        // first line of the terminal
        textGraphics.putString(0, 0, "Command Line Interface emulator, bash like, based on the lanterna java library");
        textGraphics.putString(0, 1, "(c) Davide Iaci");

        promptStructure = userInformation + ":" + location + "$ "; // set the promptStructure
        newLine();
        refreshInput();
    }

    private void onTerminalRun() throws IOException {
        boolean horizontalArrowIsClicked = false;
        boolean cursorIsAtEnd = true;
        // while terminal is running user can insert input
        while (isRunning) {
            // user input
            // input key
            KeyStroke key = screen.readInput();
            switch (key.getKeyType()) {
                case Character -> {
                    terminalBuffer.insert(screen.getCursorPosition().getColumn() - promptStructure.length(), key.getCharacter()); // insert the stroke key to the bufferedString
                    incrementCursorColumn(1); // update cursor position
                }
                case Enter -> {
                    commandOutput();
                    // the terminalBuffer is added to history only if the content is not equal to precedent command or if isn't empty
                    if (!history.isEmpty()) { // if is not the first element
                        if (!terminalBuffer.toString().equals(history.get(0)) && !terminalBuffer.isEmpty()) {
                            history.add(0, terminalBuffer.toString());
                            historyIndex = 0; // reset history index
                        }
                    } else {
                        if (!terminalBuffer.isEmpty())
                            history.add(0, terminalBuffer.toString());
                    }
                    terminalBuffer = new StringBuffer();
                    newLine();
                }
                case Backspace -> deleteChar();
                case Delete -> deleteNextChar();
                case ArrowLeft -> {
                    // limit to move left with the cursor
                    if (!(screen.getCursorPosition().getColumn() <= promptStructure.length())) {
                        decrementCursorColumn(1);
                        screen.refresh();
                    }
                    horizontalArrowIsClicked = true;
                }
                // limit to move right with the cursor
                case ArrowRight -> {
                    if (!(screen.getCursorPosition().getColumn() >= (promptStructure.length() + terminalBuffer.length()))) {
                        incrementCursorColumn(1);
                        screen.refresh();
                    }
                    horizontalArrowIsClicked = true;
                }
                case ArrowUp -> { // command before
                    if (historyIndex < history.size()) { // limit
                        terminalBuffer = new StringBuffer(history.get(historyIndex));
                        historyIndex++;
                        cleanLine();
                        screen.setCursorPosition(new TerminalPosition(promptStructure.length() + terminalBuffer.length(), screen.getCursorPosition().getRow())); // set cursor position
                    }
                }
                case ArrowDown -> { // command successive
                    if (!(historyIndex <= 1)) { // limit
                        historyIndex--;
                        terminalBuffer = new StringBuffer(history.get(historyIndex - 1));
                        cleanLine();
                        screen.setCursorPosition(new TerminalPosition(promptStructure.length() + terminalBuffer.length(), screen.getCursorPosition().getRow())); // set cursor position
                    }
                }
                case EOF -> stopTerminal();
            }

            // check if the cursor is at the end of the line
            cursorIsAtEnd = screen.getCursorPosition().getColumn() >= (promptStructure.length() + terminalBuffer.length());
            // the cursor is not upload if the cursor it isn't at the end of if horizontal arrow is clicked
            if (cursorIsAtEnd && !horizontalArrowIsClicked) {
                refreshInput();
            } else {
                horizontalArrowIsClicked = false;
                refreshInputNoCursor();
            }
        }
    }

    private void stopTerminal() throws IOException {
        isRunning = false;

        // Beep and clear
        terminal.bell();
        screen.clear();
    }

    private void refreshInput() throws IOException {
        promptStructure = userInformation + ":" + location + "$ "; // set the promptStructure
        textGraphics.putString(0, screen.getCursorPosition().getRow(), promptStructure + terminalBuffer.toString()); // echo user input
        incrementCursorColumn(promptStructure.length() + terminalBuffer.toString().length() - screen.getCursorPosition().getColumn()); // update cursor column position
        screen.refresh();
    }

    // it work as refreshInput() but doesn't upload cursor position
    private void refreshInputNoCursor() throws IOException {
        promptStructure = userInformation + ":" + location + "$ "; // set the promptStructure
        textGraphics.putString(0, screen.getCursorPosition().getRow(), promptStructure + terminalBuffer.toString()); // echo user input
        screen.refresh();
    }

    // backspace
    private void deleteChar() {
        // limit to move left with the cursor
        if (!(screen.getCursorPosition().getColumn() <= promptStructure.length())) {
            cleanLine();
            terminalBuffer.deleteCharAt(screen.getCursorPosition().getColumn() - promptStructure.length() - 1); // delete the char before the cursor in the bufferedString
            decrementCursorColumn(1);
        }
    }

    // canc
    private void deleteNextChar() {
        // limit to move right with the cursor
        if (!(screen.getCursorPosition().getColumn() >= (promptStructure.length() + terminalBuffer.length()))) {
            cleanLine();
            terminalBuffer.deleteCharAt(screen.getCursorPosition().getColumn() - promptStructure.length()); // delete the char after the cursor in the bufferedString
        }
    }

    // delete all to the right of terminalBuffer
    private void cleanLine() {
        for (int i = promptStructure.length() - 1; i < screen.getTerminalSize().getColumns(); i++) {
            textGraphics.putString(i, screen.getCursorPosition().getRow(), " ");
        }
    }

    private void newLine() {
        // update cursor position
        incrementCursorRow(1); // increment the row
        screen.setCursorPosition(new TerminalPosition(0, screen.getCursorPosition().getRow())); // reset column position to 0
        incrementCursorColumn(promptStructure.length()); // move the cursor after the $
    }

    private void commandOutput() throws IOException {
        String output = commandExecutor.executeCommand(terminalBuffer.toString()); // command is execute
        if (!output.isEmpty()) {
            // print of the output
            String[] outputToPrint = output.split("\\r\\n"); // split the output by new line
            for (String line : outputToPrint) {
                scrollLine(1);
                incrementCursorRow(1);
                textGraphics.putString(0, screen.getCursorPosition().getRow(), line);
                scrollLine(1);
                if (screen.getCursorPosition().getRow() >= screen.getTerminalSize().getRows() - 1)
                    decrementCursorRow(1);
                }
            } else{
                scrollLine(1);
            }
        }

        private void scrollLine ( int nLine){
            if (screen.getCursorPosition().getRow() >= screen.getTerminalSize().getRows() - 1)
                screen.scrollLines(0, screen.getTerminalSize().getRows(), nLine);
        }

        // TODO  autocomplete commands, filenames or folder names
        private void autocompletetion () {
            // TODO autocomplete for command name
            // TODO autocomplete for file and folder
        }

        // increment and decrement of cursor position

        private void incrementCursorColumn ( int n){
            screen.setCursorPosition(new TerminalPosition(screen.getCursorPosition().getColumn() + n, screen.getCursorPosition().getRow()));
        }

        private void decrementCursorColumn ( int n){
            screen.setCursorPosition(new TerminalPosition(screen.getCursorPosition().getColumn() - n, screen.getCursorPosition().getRow()));
        }

        private void incrementCursorRow ( int n){
            screen.setCursorPosition(new TerminalPosition(screen.getCursorPosition().getColumn(), screen.getCursorPosition().getRow() + n));
        }

        private void decrementCursorRow ( int n){
            screen.setCursorPosition(new TerminalPosition(screen.getCursorPosition().getColumn(), screen.getCursorPosition().getRow() - n));
        }
    }
