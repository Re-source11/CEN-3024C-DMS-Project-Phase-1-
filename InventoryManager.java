import java.util.ArrayList;
import java.util.List;
import java.io.File; //allows for files to be imported
import java.io.FileNotFoundException; //exception if path does not exist
import java.util.Scanner;

/*
 Baker Legerme
 CEN 3024C - 31032
 July 19th, 2026

 class: InventoryManager
 The functions and creation of objects and the crud plus custom.
 phase 4: can now be backed by MySQL. the list is still the working copy,
 the database just mirrors it, so the GUI and the tests call the exact
 same methods and never know the difference.
*/
public class InventoryManager {
    private List<Peptide> inventory;
    private int idGenerator;
    private DatabaseManager database; // null when running purely in memory
    private String lastDatabaseError = ""; // the raw reason the last connect failed

    public InventoryManager() {
        this.inventory = new ArrayList<>();
        this.idGenerator = 100; //automatic id generator to prevent inserting where one exists already
        this.database = null;
    }

    /*
     method: connectDatabase
     purpose: switches into database mode. loads whatever is stored into the
              list and starts the id generator at the highest stored id plus
              one, because ids retire, they never get handed out twice
     arguments: url: the jdbc url, user/password: the database login
     return: true if it connected and loaded, false to just stay in memory
    */
    public boolean connectDatabase(String url, String user, String password) {
        DatabaseManager db = new DatabaseManager();
        if (!db.connect(url, user, password)) {
            this.lastDatabaseError = db.getLastError();
            return false;
        }
        this.database = db;
        this.inventory = new ArrayList<>(db.loadAll());
        int maxId = 99;
        for (Peptide p : inventory) {
            if (p.getVialId() > maxId) { maxId = p.getVialId(); }
        }
        this.idGenerator = maxId + 1; // resume after the highest stored key
        return true;
    }

    /*
     method: isDatabaseConnected
     purpose: lets the GUI say which storage mode is on in the status bar
     arguments: none
     return: true when a database is behind the inventory
    */
    /*
     method: getLastDatabaseError
     purpose: passes the databases own failure message up to the GUI so the
              status bar can say the real reason instead of a guess
     arguments: none
     return: the last connect error text, or empty if there wasnt one
    */
    public String getLastDatabaseError() {
        return lastDatabaseError;
    }

    /*
     method: isDatabaseConnected
     purpose: lets the GUI say which storage mode is on in the status bar
     arguments: none
     return: true when a database is behind the inventory
    */
    public boolean isDatabaseConnected() {
        return database != null && database.isConnected();
    }

    /*
     method: syncToDatabase
     purpose: helper. after an update sticks in memory this pushes the records
              new state to the database (if ones connected) so both copies
              always match. one method covers every update path
     arguments: p: the record that just changed
     return: nothing
    */
    private void syncToDatabase(Peptide p) {
        if (isDatabaseConnected()) { database.update(p); }
    }

    /*
     method: addPeptide
     purpose: builds a new Peptide with the next auto id and puts it in the
              inventory, and mirrors it into the database when ones connected
     arguments: name, route, target, current, min, totalMass, concentration,
                frequency, titration, increment, days
     return: true if the list accepted the new object
    */
    public boolean addPeptide(String name, String route, float target, float current, float min,
                              float totalMass, float concentration, String frequency,
                              boolean titration, float increment, int days) {
        Peptide newPep = new Peptide(idGenerator++, name, route, target, current, min, totalMass,
                concentration, frequency, titration, increment, days); //creates the actual object and iterates
        boolean added = inventory.add(newPep);
        if (added && isDatabaseConnected()) { database.insert(newPep); }
        return added;
    }

    /*
     method: getAllPeptides
     purpose: hands the GUI the actual list so the table can build one row per
              record. this is the one accessor the table needed
     arguments: none
     return: the inventory as a List of Peptide objects
    */
    public List<Peptide> getAllPeptides() {
        return inventory;
    }

    /*
     method: getAllPeptidesAsString
     purpose: turns the whole inventory into one printable block, one vial per line
     arguments: none
     return: the full inventory as a String, or a message if its empty
    */
    public String getAllPeptidesAsString() {
        if (inventory.isEmpty()) {
            return "Inventory is currently empty.";
        }
        StringBuilder sb = new StringBuilder();
        for (Peptide p : inventory) {
            sb.append(p.toString()).append("\n"); // goes through each object in inventory and adds it to the custom to string
        }
        return sb.toString();
    }

    /*
     method: findPeptide
     purpose: looks up a vial by its id so callers can check it exists and read its values
     arguments: vialId: the id to search for
     return: the matching Peptide, or null if no record has that id
    */
    public Peptide findPeptide(int vialId) {
        for (Peptide p : inventory) {
            if (p.getVialId() == vialId) { return p; }
        }
        return null;
    }

    /*
     method: updatePeptideName
     purpose: changes the compound name on the vial with the matching id
     arguments: id: which vial, newName: the new name
     return: true if updated, false if the id was not found
    */
    public boolean updatePeptideName(int id, String newName) {
        Peptide p = findPeptide(id);
        if (p == null) { return false; }
        boolean ok = p.setCompoundName(newName);
        if (ok) { syncToDatabase(p); }
        return ok;
    }

    /*
     method: updatePeptideRoute
     purpose: changes the delivery method on the vial with the matching id
     arguments: id: which vial, newRoute: the new route
     return: true if updated, false if the id was not found
    */
    public boolean updatePeptideRoute(int id, String newRoute) {
        Peptide p = findPeptide(id);
        if (p == null) { return false; }
        boolean ok = p.setDeliveryMethod(newRoute);
        if (ok) { syncToDatabase(p); }
        return ok;
    }

    /*
     method: updatePeptideCurrentDose
     purpose: changes the current dose, but only if it stays at or below the target dose
              so the titration math always moves in a valid direction
     arguments: id: which vial, newCurrent: the new current dose in mg
     return: true if updated, false if the id was not found or the value broke the rule
    */
    public boolean updatePeptideCurrentDose(int id, float newCurrent) {
        Peptide p = findPeptide(id);
        if (p == null) { return false; }
        if (newCurrent > 0f && newCurrent <= p.getTargetedDosemg()) {
            boolean ok = p.setCurrentDosemg(newCurrent);
            if (ok) { syncToDatabase(p); }
            return ok;
        }
        return false;
    }

    /*
     method: updatePeptideMinDose
     purpose: changes the minimum therapeutic dose, capped at the target dose
     arguments: id: which vial, newMin: the new minimum dose in mg
     return: true if updated, false if the id was not found or the value broke the rule
    */
    public boolean updatePeptideMinDose(int id, float newMin) {
        Peptide p = findPeptide(id);
        if (p == null) { return false; }
        if (newMin > 0f && newMin <= p.getTargetedDosemg()) {
            boolean ok = p.setMinTherapeuticDosemg(newMin);
            if (ok) { syncToDatabase(p); }
            return ok;
        }
        return false;
    }

    /*
     method: updatePeptideTotalMass
     purpose: changes the total vial mass, kept inside the 0-100mg safety range
     arguments: id: which vial, newMass - the new mass in mg
     return: true if updated, false if the id was not found or the value was out of range
    */
    public boolean updatePeptideTotalMass(int id, float newMass) {
        Peptide p = findPeptide(id);
        if (p == null) { return false; }
        if (newMass > 0f && newMass <= 100f) {
            boolean ok = p.setTotalVialMassmg(newMass);
            if (ok) { syncToDatabase(p); }
            return ok;
        }
        return false;
    }

    /*
     method: updatePeptideConcentration
     purpose: changes the concentration, kept inside the 0-1000 mg/mL range
     arguments: id: which vial, newConc: the new concentration in mg/mL
     return: true if updated, false if the id was not found or the value was out of range
    */
    public boolean updatePeptideConcentration(int id, float newConc) {
        Peptide p = findPeptide(id);
        if (p == null) { return false; }
        if (newConc > 0f && newConc <= 1000f) {
            boolean ok = p.setConcentrationmgPermL(newConc);
            if (ok) { syncToDatabase(p); }
            return ok;
        }
        return false;
    }

    /*
     method: updatePeptideTargetDose
     purpose: changes the target dose, but never below the current dose already on the record,
              otherwise the titration schedule would run backwards
     arguments: id: which vial, newTargetDose: the new target dose in mg
     return: true if updated, false if the id was not found or the new target was too low
    */
    public boolean updatePeptideTargetDose(int id, float newTargetDose) {
        Peptide p = findPeptide(id);
        if (p == null) { return false; }
        if (newTargetDose >= p.getCurrentDosemg()) {
            boolean ok = p.setTargetedDosemg(newTargetDose);
            if (ok) { syncToDatabase(p); }
            return ok;
        }
        return false;
    }

    /*
     method: updatePeptideTitration
     purpose: flips whether this vial uses a titration schedule or a static dose
     arguments: id: which vial, needed: true for titration, false for static
     return: true if updated, false if the id was not found
    */
    public boolean updatePeptideTitration(int id, boolean needed) {
        Peptide p = findPeptide(id);
        if (p == null) { return false; }
        boolean ok = p.setTitrationNeeded(needed);
        if (ok) { syncToDatabase(p); }
        return ok;
    }

    /*
     method: removePeptide
     purpose: deletes the vial with the matching id from the inventory, and
              from the database table too when ones connected. the id is gone
              for good, never reused
     arguments: vialId: the id to remove
     return: true if a record was deleted, false if the id was not found
    */
    public boolean removePeptide(int vialId) {
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).getVialId() == vialId) {
                inventory.remove(i);
                if (isDatabaseConnected()) { database.delete(vialId); }
                return true;
            }
        }
        return false;
    }

    /*
     method: updatePeptideIncrement
     purpose: changes the titration increment. has to be above zero, which also protects
              the supply calculation from dividing by zero
     arguments: id: which vial, newIncrement: the new increment in mg
     return: true if updated, false if the id was not found or the value broke the rule
    */
    public boolean updatePeptideIncrement(int id, float newIncrement) {
        Peptide p = findPeptide(id);
        if (p == null) { return false; }
        if (newIncrement > 0f && newIncrement <= p.getTargetedDosemg()) {
            boolean ok = p.setTitrationIncrementMg(newIncrement);
            if (ok) { syncToDatabase(p); }
            return ok;
        }
        return false;
    }

    /*
     method: updatePeptideDays
     purpose: changes how many days are spent at each titration step (1 to 365)
     arguments: id: which vial, newDays: the new days per step
     return: true if updated, false if the id was not found or the value was out of range
    */
    public boolean updatePeptideDays(int id, int newDays) {
        Peptide p = findPeptide(id);
        if (p == null) { return false; }
        if (newDays >= 1 && newDays <= 365) {
            boolean ok = p.setDaysNeeded(newDays);
            if (ok) { syncToDatabase(p); }
            return ok;
        }
        return false;
    }

    /*
     method: frequencyToDaysBetween
     purpose: converts the user's frequency input (a word or a number of days)
              into days-between-doses for the supply math. one converter, one place.
     arguments: input - "daily", "eod", "weekly", or a number of days up to 10
     return: days between doses as a float, or -1 if the input is invalid
    */
    public float frequencyToDaysBetween(String input) {
        if (input == null) { return -1f; }
        String f = input.trim().toLowerCase();
        if (f.equals("daily")) { return 1f; }
        if (f.equals("eod")) { return 2f; }
        if (f.equals("weekly")) { return 7f; }
        try {
            float days = Float.parseFloat(f);
            if (days > 0f && days <= 10f) { return days; }
        } catch (NumberFormatException e) {
            // falls through to invalid
        }
        return -1f;
    }

    /*
     method: updateFrequency
     purpose: changes how often this vial is dosed. the input runs through
              frequencyToDaysBetween first so only a real frequency (daily,
              eod, weekly, or 1 to 10 days) can ever land on a record
     arguments: id: which vial, newFrequency: whatever the user typed
     return: true if updated, false if the id was not found or the input was invalid
    */
    public boolean updateFrequency(int id, String newFrequency) {
        Peptide p = findPeptide(id);
        if (p == null) { return false; }
        if (frequencyToDaysBetween(newFrequency) < 0f) { return false; }
        boolean ok = p.setFrequency(newFrequency.trim().toLowerCase());
        if (ok) { syncToDatabase(p); }
        return ok;
    }

    /*
     method: calculateSupply
     purpose: the custom action. figures out how many days the vial will last.
              every branch multiplies by the frequency now, since a vial holding
              5 doses lasts 5 days dosed daily but 35 dosed weekly. for a static
              dose its just doses left times days between. for a titration
              schedule it walks each dose step, subtracts the mass that step
              eats (days per step divided by days between, kept in float math
              so 7 days at eod is 3.5 doses and not 3), and either warns that
              the vial runs out mid-titration (with the total mg the protocol
              needs) or reports how many days the full protocol lasts including
              the leftover days at the target dose
     arguments: vialId: which vial to run the calculation on
     return: a result String, either the supply projection or an error if the id was not found
    */
    public String calculateSupply(int vialId) {
        Peptide target = findPeptide(vialId);
        if (target == null) return "Error: Vial ID not found in inventory.";

        float remainingMass = target.getTotalVialMassmg();
        float currentDose = target.getCurrentDosemg();
        float targetDose = target.getTargetedDosemg();

        // frequency multiplier, how many days pass between doses. anything
        // unset or invalid falls back to daily so old records still calculate
        float daysBetween = frequencyToDaysBetween(target.getFrequency());
        if (daysBetween < 0f) { daysBetween = 1f; }

        if (!target.isTitrationNeeded()) {
            float days = (remainingMass / currentDose) * daysBetween;
            return "Adequate Supply: Static dose schedule lasts " + String.format("%.1f", days) + " days. (Log entry saved)";
        }

        //  Does not allow for division of 0
        float increment = target.getTitrationIncrementmg();
        if (increment <= 0f) {
            float days = (remainingMass / currentDose) * daysBetween;
            return "Adequate Supply: No titration increment set; schedule lasts " + String.format("%.1f", days) + " days. (Log entry saved)";
        }

        int steps = (int) Math.ceil((targetDose - currentDose) / increment);
        float totalDays = 0;
        float totalRequiredMass = 0;
        int daysPerStep = target.getDaysNeeded();
        // float division on purpose. 7 days per step dosed eod is 3.5 doses,
        // integer division would quietly round that to 3 and understate how
        // much mass each step eats
        float dosesPerStep = daysPerStep / daysBetween;

        for (int k = 0; k < steps; k++) {
            float activeDose = currentDose + (k * increment);
            float massForThisStep = activeDose * dosesPerStep;
            totalRequiredMass += massForThisStep;

            if (remainingMass < massForThisStep) {
                totalDays += (remainingMass / activeDose) * daysBetween;
                return "WARNING: Supply lasts " + String.format("%.1f", totalDays) + " days. " + String.format("%.1f", totalRequiredMass) +
                        "mg required to finish titration phase. (Log entry saved regardless)";
            }
            remainingMass -= massForThisStep;
            totalDays += daysPerStep;
        }

        // after the last step the schedule is sitting at the target dose, so
        // the leftover mass gets used up at targetDose, not the starting dose
        totalDays += (remainingMass / targetDose) * daysBetween;
        return "SUCCESS: Adequate supply remains. Protocol lasts " + String.format("%.1f", totalDays) + " days.";
    }

    /*
     method: loadFromFile
     purpose: batch create. reads a text file where every line is one record in the format
              name,route,target,current,min,totalMass,concentration,frequency,titration,increment,days
              (eleven columns now, frequency is the new eighth one) and runs each line
              through the same validation as manual entry so bad data never gets in.
              bad lines are skipped, counted, and pointed out by line number instead
              of stopping the whole load
     arguments: filePath: the location of the text file the user picked
     return: a summary String with how many records loaded and which line numbers were skipped
    */
    public String loadFromFile(String filePath) {
        int loaded = 0;
        int lineNumber = 0;
        List<Integer> skippedLines = new ArrayList<>();
        File file = new File(filePath.trim());

        if (!file.exists() || !file.isFile()) {
            return "Error: File not found at '" + filePath + "'. Please check the path and try again.";
        }

        try (Scanner fileScanner = new Scanner(file)) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine().trim();
                lineNumber++; // counts every line in the file so reported numbers match the file
                if (line.isEmpty()) continue; // ignore blank lines

                String[] parts = line.split(",");
                if (parts.length != 11) { skippedLines.add(lineNumber); continue; }

                try {
                    String name = parts[0].trim();
                    String route = parts[1].trim().toLowerCase();
                    float target = Float.parseFloat(parts[2].trim());
                    float current = Float.parseFloat(parts[3].trim());
                    float min = Float.parseFloat(parts[4].trim());
                    float totalMass = Float.parseFloat(parts[5].trim());
                    float conc = Float.parseFloat(parts[6].trim());
                    String frequency = parts[7].trim().toLowerCase();
                    boolean titration = Boolean.parseBoolean(parts[8].trim());
                    float increment = Float.parseFloat(parts[9].trim());
                    int days = Integer.parseInt(parts[10].trim());

                    // Same validation rules as manual entry so bad data is not allowed
                    boolean validName = name.matches("^[a-zA-Z0-9-]{3,20}$");
                    boolean validRoute = route.equals("subcutaneous") || route.equals("topical") || route.equals("intramuscular");
                    boolean validFrequency = frequencyToDaysBetween(frequency) > 0f;
                    boolean validNums = target > 0f && target <= 100f && current > 0f && current <= target
                            && min > 0f && min <= target && totalMass > 0f && totalMass <= 100f
                            && conc > 0f && increment >= 0f && days > 0;

                    if (validName && validRoute && validFrequency && validNums) {
                        addPeptide(name, route, target, current, min, totalMass, conc, frequency, titration, increment, days);
                        loaded++;
                    } else {
                        skippedLines.add(lineNumber);
                    }
                } catch (NumberFormatException e) {
                    skippedLines.add(lineNumber); // malformed numbers on this line - skip it, keep going
                }
            }
        } catch (FileNotFoundException e) {
            return "Error: Could not open file '" + filePath + "'.";
        }

        // Build the summary; only mention skipped lines if there were any
        String summary = "Load complete: " + loaded + " records added, " + skippedLines.size() + " invalid lines skipped.";
        if (!skippedLines.isEmpty()) {
            StringBuilder sb = new StringBuilder(summary);
            sb.append("\nSkipped line");
            sb.append(skippedLines.size() == 1 ? "" : "s"); // singular/plural
            sb.append(": ");
            for (int i = 0; i < skippedLines.size(); i++) {
                sb.append(skippedLines.get(i));
                if (i < skippedLines.size() - 1) { sb.append(", "); }
            }
            summary = sb.toString();
        }
        return summary;
    }
}