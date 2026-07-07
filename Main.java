import java.util.Scanner;
import java.util.NoSuchElementException; // to protect iterations

/*
 Baker Legerme
 CEN 3024C - 31032
 July 3rd, 2026

 class: Main
 Where it all begins

 Overall program objective: This is a DMS for tracking peptides.

*/
public class Main {

    private static final float MAX_SAFE_LIMIT = 100.0f;


    public static void main(String[] args) {
        Main app = new Main();
        app.runApplication();
    }

    /*
     method: readFloat
     purpose: keeps asking until the user types a real number inside the allowed range,
              so letters, blanks, or out of range values can never crash anything
     arguments: scanner: the shared input scanner, prompt: what to show the user,
                min to max: the allowed range
     return: the validated float
    */
    public float readFloat(Scanner scanner, String prompt, float min, float max) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                float value = Float.parseFloat(line);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("Error: Value must be between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a numeric value.");
            }
        }
    }

    /*
     method: readInt
     purpose: same idea as readFloat but for whole numbers
     arguments: scanner: the shared input scanner, prompt: what to show the user,
                min to max: the allowed range
     return: the validated int
    */
    public int readInt(Scanner scanner, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("Error: Value must be between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a whole number.");
            }
        }
    }

    /*
     method: readBoolean
     purpose: only accepts the words true or false (any casing), re-prompts on anything else
     arguments: scanner - the shared input scanner, prompt - what to show the user
     return: the validated boolean
    */
    public boolean readBoolean(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim().toLowerCase();
            if (line.equals("true")) return true;
            if (line.equals("false")) return false;
            System.out.println("Error: Please enter true or false.");
        }
    }

    /*
     method: runApplication
     purpose: the main loop. shows the menu, validates the selection, and routes each
              choice to the matching InventoryManager operation. wraps everything so a
              closed input stream exits without crashing
     arguments: none
     return: true when the user exits normally
    */
    public boolean runApplication() {
        InventoryManager manager = new InventoryManager();
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("=== Peptide Database Management System (Phase 1) ===");

        try {
            while (running) { //displays the cases when running is true
                System.out.println("\n1. Load Data\n2. Display Inventory\n3. Add Peptide\n4. Update Peptide Data\n5. Remove Peptide\n6. Calculate Supply\n0. Exit");
                int choice = readInt(scanner, "Select an option: ", 0, 6);

                switch (choice) {
                    case 1:
                        // shows the user the exact format before asking for a path, the
                        // actual checking happens inside loadFromFile line by line
                        System.out.println("\n--- File Format Rules ---");
                        System.out.println("Each line in your text file must be ONE record with 10 comma-separated values, in this order:");
                        System.out.println("  name,route,targetDose,currentDose,minDose,totalMass,concentration,titrationNeeded,increment,daysPerStep");
                        System.out.println("Example: BPC-157,subcutaneous,0.5,0.25,0.1,10,2.5,true,0.05,7");
                        System.out.println("Rules:");
                        System.out.println("  - name: 3-20 characters, letters/numbers/hyphens only");
                        System.out.println("  - route: subcutaneous, topical, or intramuscular");
                        System.out.println("  - doses and mass: greater than 0, no larger than 100 mg; current and min cannot exceed target");
                        System.out.println("  - concentration: greater than 0, up to 1000 mg/mL");
                        System.out.println("  - titrationNeeded: true or false");
                        System.out.println("  - daysPerStep: whole number, 1 to 365");
                        System.out.println("Lines that break any rule are skipped and reported by line number.\n");
                        System.out.print("Enter the path to your data file (or 0 to go back): ");
                        String path = scanner.nextLine().trim();
                        if (path.equals("0")) { System.out.println("Returning to main menu..."); break; }
                        if (path.isEmpty()) { System.out.println("Error: File path cannot be blank."); break; }
                        System.out.println(manager.loadFromFile(path));
                        break;

                    case 2: //runs the function to show all the peptides
                        System.out.println(manager.getAllPeptidesAsString());
                        break;

                    case 3:
                        System.out.print("\nEnter Compound Name (or type '0' to go back): ");
                        String name = scanner.nextLine().trim();
                        if (name.equals("0")) { System.out.println("Returning to main menu..."); break; }
                        while (!name.matches("^[a-zA-Z0-9-]{3,20}$")) { //checks the formatting
                            System.out.print("Error. Enter Compound Name (3-20 chars, alphanumeric/hyphens): ");
                            name = scanner.nextLine().trim();
                        }

                        String route = "";
                        while (true) { //forces the choices to match
                            System.out.print("Enter Delivery Method (Subcutaneous, Topical, Intramuscular): ");
                            route = scanner.nextLine().trim().toLowerCase();
                            if (route.equals("subcutaneous") || route.equals("topical") || route.equals("intramuscular")) break;
                            System.out.println("Error: Must match one of the listed methods.");
                        }

                        float totalMass = readFloat(scanner, "Enter Total Vial Mass in mg (0.1 - 100.0): ", 0.1f, MAX_SAFE_LIMIT);
                        float target = readFloat(scanner, "Enter Target Dose in mg (0.1 - 100.0): ", 0.1f, MAX_SAFE_LIMIT);
                        float current = readFloat(scanner, "Enter Current Dose in mg (0.1 - " + target + "): ", 0.1f, target);
                        float min = readFloat(scanner, "Enter Min Therapeutic Dose in mg (0.1 - " + target + "): ", 0.1f, target);
                        float conc = readFloat(scanner, "Enter Concentration in mg/mL (0.1 - 1000.0): ", 0.1f, 1000.0f);
                        boolean isTitrating = readBoolean(scanner, "Is Titration Needed? (true/false): ");

                        float increment = 0f;
                        int days = 1;
                        if (isTitrating) {
                            // Only ask titration details when they apply additionally forces a lower bound of 0 that prevents crashing with division of 0
                            increment = readFloat(scanner, "Enter Titration Increment in mg (0.1 - " + target + "): ", 0.1f, target);
                            days = readInt(scanner, "Enter Days Needed per Step (1 - 365): ", 1, 365);
                        }

                        boolean success = manager.addPeptide(name, route, target, current, min, totalMass, conc, isTitrating, increment, days);
                        System.out.println(success ? "Vial added successfully." : "System Error: Failed to add.");
                        System.out.println(manager.getAllPeptidesAsString());
                        break;

                    case 4:
                        int updateId = readInt(scanner, "\nEnter Vial ID to update (or type 0 to go back): ", 0, Integer.MAX_VALUE);
                        if (updateId == 0) { System.out.println("Returning to main menu..."); break; }

                        // Verify the record exists before offering any fields to edit
                        Peptide record = manager.findPeptide(updateId);
                        if (record == null) {
                            System.out.println("Error: ID not found.");
                            break;
                        }

                        // Field-edit loop: the user can keep updating fields on this
                        // record until they choose 0, then the inventory prints.
                        boolean editing = true;
                        while (editing) {
                            System.out.println("\nEditing " + record.getCompoundName() + " (Vial " + record.getVialId() + ")");
                            System.out.println("1. Compound Name\n2. Delivery Route\n3. Target Dose\n4. Current Dose\n5. Min Therapeutic Dose\n6. Total Vial Mass\n7. Concentration\n8. Titration Needed\n9. Titration Increment\n10. Days per Step\n0. Done - back to inventory");
                            int updateChoice = readInt(scanner, "Select field to update: ", 0, 10);

                            switch (updateChoice) {
                                case 0:
                                    editing = false;
                                    break;
                                case 1:
                                    String newName = "";
                                    while (true) {
                                        System.out.print("Enter new Compound Name (3-20 chars, alphanumeric/hyphens): ");
                                        newName = scanner.nextLine().trim();
                                        if (newName.matches("^[a-zA-Z0-9-]{3,20}$")) break;
                                        System.out.println("Error: Invalid characters or length.");
                                    }
                                    System.out.println(manager.updatePeptideName(updateId, newName) ? "Updated successfully." : "Error: Update failed.");
                                    break;
                                case 2:
                                    String newRoute = "";
                                    while (true) {
                                        System.out.print("Enter new Delivery Route (Subcutaneous, Topical, Intramuscular): ");
                                        newRoute = scanner.nextLine().trim().toLowerCase();
                                        if (newRoute.equals("subcutaneous") || newRoute.equals("topical") || newRoute.equals("intramuscular")) break;
                                        System.out.println("Error: Must match one of the listed methods.");
                                    }
                                    System.out.println(manager.updatePeptideRoute(updateId, newRoute) ? "Updated successfully." : "Error: Update failed.");
                                    break;
                                case 3:
                                    // New target cannot drop below the current dose already on the record
                                    float floor = Math.max(record.getCurrentDosemg(), record.getMinTherapeuticDosemg());
                                    float newTarget = readFloat(scanner, "Enter new Target Dose in mg (" + floor + " - 100.0): ", floor, MAX_SAFE_LIMIT);
                                    System.out.println(manager.updatePeptideTargetDose(updateId, newTarget) ? "Updated successfully." : "Error: Update failed.");
                                    break;
                                case 4:
                                    float newCurrent = readFloat(scanner, "Enter new Current Dose in mg (0.1 - " + record.getTargetedDosemg() + "): ", 0.1f, record.getTargetedDosemg());
                                    System.out.println(manager.updatePeptideCurrentDose(updateId, newCurrent) ? "Updated successfully." : "Error: Update failed.");
                                    break;
                                case 5:
                                    float newMin = readFloat(scanner, "Enter new Min Therapeutic Dose in mg (0.1 - " + record.getTargetedDosemg() + "): ", 0.1f, record.getTargetedDosemg());
                                    System.out.println(manager.updatePeptideMinDose(updateId, newMin) ? "Updated successfully." : "Error: Update failed.");
                                    break;
                                case 6:
                                    float newMass = readFloat(scanner, "Enter new Total Vial Mass in mg (0.1 - 100.0): ", 0.1f, MAX_SAFE_LIMIT);
                                    System.out.println(manager.updatePeptideTotalMass(updateId, newMass) ? "Updated successfully." : "Error: Update failed.");
                                    break;
                                case 7:
                                    float newConc = readFloat(scanner, "Enter new Concentration in mg/mL (0.1 - 1000.0): ", 0.1f, 1000.0f);
                                    System.out.println(manager.updatePeptideConcentration(updateId, newConc) ? "Updated successfully." : "Error: Update failed.");
                                    break;
                                case 8:
                                    boolean newTitration = readBoolean(scanner, "Is Titration Needed? (true/false): ");
                                    manager.updatePeptideTitration(updateId, newTitration);
                                    if (newTitration && record.getTitrationIncrementmg() <= 0f) {
                                        // Turning titration on with no increment set - collect it now
                                        // so the supply calculation has valid inputs to work with
                                        float inc = readFloat(scanner, "Enter Titration Increment in mg (0.1 - " + record.getTargetedDosemg() + "): ", 0.1f, record.getTargetedDosemg());
                                        manager.updatePeptideIncrement(updateId, inc);
                                        int stepDays = readInt(scanner, "Enter Days Needed per Step (1 - 365): ", 1, 365);
                                        manager.updatePeptideDays(updateId, stepDays);
                                    }
                                    System.out.println("Updated successfully.");
                                    break;
                                case 9:
                                    float newInc = readFloat(scanner, "Enter new Titration Increment in mg (0.1 - " + record.getTargetedDosemg() + "): ", 0.1f, record.getTargetedDosemg());
                                    System.out.println(manager.updatePeptideIncrement(updateId, newInc) ? "Updated successfully." : "Error: Update failed.");
                                    break;
                                case 10:
                                    int newDays = readInt(scanner, "Enter new Days per Step (1 - 365): ", 1, 365);
                                    System.out.println(manager.updatePeptideDays(updateId, newDays) ? "Updated successfully." : "Error: Update failed.");
                                    break;
                            }
                        }
                        System.out.println(manager.getAllPeptidesAsString());
                        break;

                    case 5:
                        int delId = readInt(scanner, "\nEnter Vial ID to remove (or type 0 to go back): ", 0, Integer.MAX_VALUE);
                        if (delId == 0) { System.out.println("Returning to main menu..."); break; }
                        System.out.println(manager.removePeptide(delId) ? "Record permanently deleted." : "Error: ID not found.");
                        System.out.println(manager.getAllPeptidesAsString());
                        break;

                    case 6:
                        int calcId = readInt(scanner, "\nEnter Vial ID to Calculate Supply (or type 0 to go back): ", 0, Integer.MAX_VALUE);
                        if (calcId == 0) { System.out.println("Returning to main menu..."); break; }
                        System.out.println(manager.calculateSupply(calcId));
                        break;

                    case 0:
                        running = false;
                        System.out.println("Shutting down DMS...");
                        break;
                }
            }
        } catch (NoSuchElementException e) {
            // If input stream was closed
            System.out.println("\nInput stream closed. Shutting down DMS...");
        }
        scanner.close();
        return true;
    }
}