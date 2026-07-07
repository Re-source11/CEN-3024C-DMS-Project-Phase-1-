import java.util.ArrayList;
import java.util.List;
import java.io.File; //allows for files to be imported
import java.io.FileNotFoundException; //exception if path does not exist
import java.util.Scanner;

/*
 Baker Legerme
 CEN 3024C - 31032
 July 3rd, 2026

 class: InventoryManager
The functions and creation of objects and the crud plus custom
*/
public class InventoryManager {
    private List<Peptide> inventory;
    private int idGenerator;

    public InventoryManager() {
        this.inventory = new ArrayList<>();
        this.idGenerator = 100; //automatic id generator to prevent inserting where one exists already
    }

    /*
     method: addPeptide
     purpose: builds a new Peptide with the next auto id and puts it in the inventory
     arguments: name, route, target, current, min, totalMass, concentration, titration,
                increment, days
     return: true if the list accepted the new object
    */
    public boolean addPeptide(String name, String route, float target, float current, float min,
                              float totalMass, float concentration, boolean titration, float increment, int days) {
        Peptide newPep = new Peptide(idGenerator++, name, route, target, current, min, totalMass, concentration, titration, increment, days); //creates the actual object and iterates
        return inventory.add(newPep);
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
        for (Peptide p : inventory) { //goes through the inventory and looks for the object that matches the id.
            if (p.getVialId() == id) { return p.setCompoundName(newName); }
        }
        return false;
    }

    /*
     method: updatePeptideRoute
     purpose: changes the delivery method on the vial with the matching id
     arguments: id: which vial, newRoute: the new route
     return: true if updated, false if the id was not found
    */
    public boolean updatePeptideRoute(int id, String newRoute) {
        for (Peptide p : inventory) {
            if (p.getVialId() == id) { return p.setDeliveryMethod(newRoute); }
        }
        return false;
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
            return p.setCurrentDosemg(newCurrent);
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
            return p.setMinTherapeuticDosemg(newMin);
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
        if (p == null) {
            return false;
        }
        if (newMass > 0f && newMass <= 100f) {
            return p.setTotalVialMassmg(newMass);
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
            return p.setConcentrationmgPermL(newConc);
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
        for (Peptide p : inventory) {
            if (p.getVialId() == id) {
                if (newTargetDose >= p.getCurrentDosemg()) {
                    return p.setTargetedDosemg(newTargetDose);
                }
            }
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
        return p.setTitrationNeeded(needed);
    }

    /*
     method: removePeptide
     purpose: deletes the vial with the matching id from the inventory
     arguments: vialId: the id to remove
     return: true if a record was deleted, false if the id was not found
    */
    public boolean removePeptide(int vialId) {
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).getVialId() == vialId) {
                inventory.remove(i);
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
            return p.setTitrationIncrementMg(newIncrement);
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
            return p.setDaysNeeded(newDays);
        }
        return false;
    }

    /*
     method: calculateSupply
     purpose: the custom action. figures out how many days the vial will last. for a static
              dose its just mass divided by dose. for a titration schedule it walks each dose
              step, subtracts the mass that step consumes, and either warns that the vial runs
              out mid-titration (with the total mg the protocol needs) or reports how many days
              the full protocol lasts including the leftover days at the target dose
     arguments: vialId: which vial to run the calculation on
     return: a result String, either the supply projection or an error if the id was not found
    */
    public String calculateSupply(int vialId) {
        Peptide target = null;
        for (Peptide p : inventory) {
            if (p.getVialId() == vialId) { target = p; break; }
        }

        if (target == null) return "Error: Vial ID not found in inventory.";

        float remainingMass = target.getTotalVialMassmg();
        float currentDose = target.getCurrentDosemg();
        float targetDose = target.getTargetedDosemg();

        if (!target.isTitrationNeeded()) {
            float days = remainingMass / targetDose;
            return "Adequate Supply: Static dose schedule lasts " + String.format("%.1f", days) + " days. (Log entry saved)";
        }

        //  Does not allow for division of 0
        float increment = target.getTitrationIncrementmg();
        if (increment <= 0f) {
            float days = remainingMass / targetDose;
            return  "Adequate Supply: No titration increment set; schedule lasts " + String.format("%.1f", days) + " days. (Log entry saved)";
        }

        int steps = (int) Math.ceil((targetDose - currentDose) / increment);
        float totalDays = 0;
        float totalRequiredMass = 0;
        int daysPerStep = target.getDaysNeeded();

        for (int k = 0; k < steps; k++) {
            float activeDose = currentDose + (k * increment);
            float massForThisStep = activeDose * daysPerStep;
            totalRequiredMass += massForThisStep;

            if (remainingMass < massForThisStep) {
                totalDays += (remainingMass / activeDose);
                return  "WARNING: Supply lasts " + String.format("%.1f", totalDays) + " days. " + String.format("%.1f", totalRequiredMass) +
                        "mg required to finish titration phase. (Log entry saved regardless)";
            }
            remainingMass -= massForThisStep;
            totalDays += daysPerStep;
        }

        totalDays += (remainingMass / targetDose);
        return  "SUCCESS: Adequate supply remains. Protocol lasts " + String.format("%.1f", totalDays) + " days.";

    }

    /*
     method: loadFromFile
     purpose: batch create. reads a text file where every line is one record in the format
              name,route,target,current,min,totalMass,concentration,titration,increment,days
              and runs each line through the same validation as manual entry so bad data
              never gets in. bad lines are skipped, counted, and pointed out by line number
              instead of stopping the whole load
     arguments: filePath: the location of the text file the user typed in
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
                if (parts.length != 10) { skippedLines.add(lineNumber); continue; }

                try {
                    String name = parts[0].trim();
                    String route = parts[1].trim().toLowerCase();
                    float target = Float.parseFloat(parts[2].trim());
                    float current = Float.parseFloat(parts[3].trim());
                    float min = Float.parseFloat(parts[4].trim());
                    float totalMass = Float.parseFloat(parts[5].trim());
                    float conc = Float.parseFloat(parts[6].trim());
                    boolean titration = Boolean.parseBoolean(parts[7].trim());
                    float increment = Float.parseFloat(parts[8].trim());
                    int days = Integer.parseInt(parts[9].trim());

                    // Same validation rules as manual entry so bad data is not allowed
                    boolean validName = name.matches("^[a-zA-Z0-9-]{3,20}$");
                    boolean validRoute = route.equals("subcutaneous") || route.equals("topical") || route.equals("intramuscular");
                    boolean validNums = target > 0f && target <= 100f && current > 0f && current <= target
                            && min > 0f && min <= target && totalMass > 0f && totalMass <= 100f
                            && conc > 0f && increment >= 0f && days > 0;

                    if (validName && validRoute && validNums) {
                        addPeptide(name, route, target, current, min, totalMass, conc, titration, increment, days);
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