import com.googlecode.lanterna.TerminalPosition;
import org.unix4j.Unix4j;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * Represent a command executor
 *
 * @author Davide Iaci
 * @version 1.0
 */
public class Command {

    private CLI commandLineInterface;

    private StringTokenizer stringTokenizer; // permise to split the string command
    private ArrayList<String> commandParts; // syntax: command [-options] [arguments]

    // parts of the command
    private String name;
    private StringBuffer options;
    private ArrayList<String> argument;

    // utilities for cd command
    int lengthOfLastDir;

    public Command(CLI cli) {
        commandLineInterface = cli;

        stringTokenizer = null;
        commandParts = null;

        name = "";
        options = new StringBuffer();
        argument = new ArrayList<>();

        lengthOfLastDir = 0;
    }

    /**
     * Execute the command passed by argument
     *
     * @param command the command passed by argument
     * @return the output of the command
     */
    public String executeCommand(String command) {
        commandParts = new ArrayList<>();
        splitCommand(command);

        String output = ""; // output of the command

        if (!commandParts.isEmpty()) { // if the command is not empty
            switch (name) { // name of command
                case "cat":
                    File f = new File(argument.get(0));
                    if (!f.isDirectory()) { // check that is not a directory
                        if (!argument.isEmpty()) {
                            try {
                                output = Unix4j.cat(argument.get(0)).toStringResult();
                            } catch (IllegalArgumentException e) {
                                output = "cat: can't open '" + argument.get(0) + "' : No such file or directory";
                            }
                        } else {
                            output = "(argument is necessary) syntax: cat FILE";
                        }
                    } else {
                        output = "cat: read error: Is a directory";
                    }
                    break;
                case "clear":
                    commandLineInterface.screen.clear(); // clear screen
                    commandLineInterface.screen.setCursorPosition(new TerminalPosition(0, 0));
                    break;
//                case "cd":
//                    if (argument.get(0).equals(".")) {
//                        commandLineInterface.location = commandLineInterface.location; // location is the same
//                        System.out.println(".");
//                    } else if (argument.get(0).equals("..")) {
//                        System.out.println("..");
//                        commandLineInterface.location = commandLineInterface.location.substring(0, commandLineInterface.location.length() - lengthOfLastDir); // set location
//                    } else {
//                        // check if a file exist
//                        File file = new File(System.getProperty("user.dir") + commandLineInterface.location.replace("~", "\\") + argument.get(0));
//                        if (file.exists())
//                            commandLineInterface.location +=  "\\" + argument.get(0);
//                        else
//                            output = "bash: cd: can't cd to " + argument.get(0) + ": No such file or directory";
//                    }
//                    break;
                case "echo":
                    // option not supported yet
                    StringBuilder completeArgument = new StringBuilder(); // the sum of all arguments
                    for (String args : argument)
                        completeArgument.append(args).append(" ");
                    if (options.isEmpty()) {
                        output = Unix4j.echo(completeArgument.toString()).toStringResult();
                        output = output.substring(0, output.length() - 1); // remove the last char that is blank
                    } else {
                        output = "echo: options not yet supported by the command";
                    }
                    break;
                case "exit":
                    try {
                        commandLineInterface.screen.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case "grep":
                    // TODO filtra contenuto file
                    break;
                case "help":
                    output = """
                            cat: print FILE to stdout
                            echo: display a line of text
                            grep: Search for PATTERN in FILE
                            help: Display information about builtin commands
                            ls: list directory contents (only command with option)
                            pwd: print name of current/working directory
                            rm: remove FILE
                            touch: create new file
                            wget: Retrieve files via HTTP""";
                    break;
                case "ls":
                    try {
                        // bug: find only the first
                        if (!argument.isEmpty())
                            output = Unix4j.ls("-" + options.toString(), argument.get(0)).toStringResult();
                        else
                            output = Unix4j.ls("-" + options.toString()).toStringResult();
                    } catch (IllegalArgumentException e) {
                        if (e.toString().contains("file not found"))
                            output = "ls: " + argument.get(0) + ": No such file or directory";
                        else
                            output = "ls: unrecognized option: " + command;
                    }
                    break;
                case "pwd":
                    output = System.getProperty("user.dir") + commandLineInterface.location.replace("~", "");
                    break;
                case "rm": // remove file
                    File fileToRemove;
                    if (!argument.isEmpty()) {
                        fileToRemove = new File(argument.get(0));
                        if (fileToRemove.delete())
                            System.out.println("File successfully deleted");
                        else
                            System.out.println("Error in file elimination");
                    }
                    break;
                case "touch": // create file
                    File fileToAdd;
                    if (!argument.isEmpty()) {
                        fileToAdd = new File(argument.get(0));
                        try {
                            if (!fileToAdd.exists()) {
                                if (fileToAdd.createNewFile())
                                    System.out.println("File successfully created");
                                else
                                    System.out.println("Error in file creation");
                            } else { // if the file already exist remove it and create the empty file
                                boolean isDeleted = fileToAdd.delete();
                                if (fileToAdd.createNewFile())
                                    System.out.println("File successfully replaced");
                                else
                                    System.out.println("Error in file replaced");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else
                        output = "Usage: touch FILE (no blanks)";
                    break;
                case "wget": // download a file with inside the html of a page
                    // TODO scarica file da internet
                    if (!argument.isEmpty()) {
                        // read html page
                        URL url = null;
                        // try to open the page before with http and in case then with https
                        try {
                            url = new URL(argument.get(0)); // create the URL via the argument passed by argument
                            Scanner scanner = new Scanner(url.openStream());
                            StringBuffer fileContent = new StringBuffer();
                            while (scanner.hasNext()) {
                                fileContent.append(scanner.next());
                            }
                        } catch (IOException e) { // if url is not found
                            output = "wget: bad address '" + argument.get(0) + "'";
                        }

                        // create file with inside the content read before
//                        File html = new File("");

                    } else {
                        output = "(argument is necessary) syntax: wget URL";
                    }
                    break;
                default:
                    output = "bash: " + command + ": command not found";
                    break;
            }
            // reset parts of command
            name = "";
            options = new StringBuffer();
            argument.clear();
        }

        return output;
    }

    // split the parts of the command
    private void splitCommand(String command) {
        // split the command in other strings
        stringTokenizer = new StringTokenizer(command);
        while (stringTokenizer.hasMoreElements()) {
            commandParts.add(stringTokenizer.nextToken());
        }

        // check if each part is a name, option or argument
        for (int i = 0; i < commandParts.size(); i++) {
            if (i == 0) {
                name = commandParts.get(i); // first index is the name
            } else {
                if (commandParts.get(i).startsWith("-")) { // option
                    options.append(commandParts.get(i).substring(1));
                } else {
                    argument.add(commandParts.get(i)); // argument
                }
            }
        }
    }
}
