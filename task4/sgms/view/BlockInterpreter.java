package view;

import java.util.List;
import java.util.Scanner;

import controller.Controller;
import model.Instrument;

/**
 * Reads and interprets user commands. This command interpreter is blocking, the
 * user
 * interface does not react to user input while a command is being executed.
 */
public class BlockInterpreter {
    private static final String PROMPT = "> ";
    private final Scanner console = new Scanner(System.in);
    private Controller ctrl;
    private boolean keepReceivingCmds = false;

    /**
     * Creates a new instance that will use the specified controller for all
     * operations.
     * 
     * @param ctrl The controller used by this instance.
     */
    public BlockInterpreter(Controller ctrl) {
        this.ctrl = ctrl;
    }

    /**
     * Stops the command interpreter
     */
    public void stop() {
        keepReceivingCmds = false;
    }

    /**
     * Interprets and performs user commands. This method will not return until the
     * UI has been stopped. The UI is stopped either when the user gives the
     * 'quit' command, or when the method <code>stop()</code> is called.
     */
    public void handleCmds() {
        keepReceivingCmds = true;
        while(keepReceivingCmds) {
            try {
                CmdLine cmdLine = new CmdLine(readNextLine());
                switch (cmdLine.getCmd()) {
                    case HELP:
                        for(Command command : Command.values()){
                            if(command == Command.ILLEGAL_COMMAND){
                                continue;
                            }
                            System.out.println(command.toString().toLowerCase());
                        }
                        break;
                    case QUIT:
                        keepReceivingCmds = false;
                        break;
                    case LIST:
                        List<Instrument> instruments = ctrl.getRentableInstruments(cmdLine.getParameter(0));
                        for(Instrument instrument  : instruments){
                            System.out.println(instrument.getId() + " | " + instrument.getBrand() + ", " + instrument.getFee());
                        } 
                        break;
                    case RENT:
                        ctrl.rentInstrument(cmdLine.getParameter(0), cmdLine.getParameter(1));
                        System.out.println("You rented instrument: " +
                                cmdLine.getParameter(0) + " to: " + cmdLine.getParameter(1));
                        break;
                    case TERMINATE:
                        ctrl.terminateRental(cmdLine.getParameter(0), cmdLine.getParameter(1));
                        System.out.println("You terminated the rental of instrument: " +
                                cmdLine.getParameter(0) + " to: " + cmdLine.getParameter(1));
                        break;
                
                    default:
                        System.out.println("illegal command");
                        break;
                }
            } catch (Exception e) {
                System.out.println("Operation failed");
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
            
        }
    }

    private String readNextLine() {
        System.out.print(PROMPT);
        return console.nextLine();
    }

}
