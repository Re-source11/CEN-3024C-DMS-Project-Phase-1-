/**
 * This is the object class for the DMS. one instance is one vial in inventory.
 * frequency (how often a dose gets taken) is a real field now, carried from
 * the constructor all the way through the supply math.
 * <p>
 * CEN 3024C - 31032
 *
 * @author Baker Legerme
 * @version 4.0
 */
public class Peptide {
    private int vialId;
    private String compoundName;
    private String deliveryMethod;
    private float targetedDosemg;
    private float currentDosemg;
    private float minTherapeuticDosemg;
    private float totalVialMassmg;
    private float concentrationmgPermL;
    private boolean titrationNeeded;
    private float titrationIncrementmg;
    private int daysNeeded;
    private String frequency;

    /**
     * builds one vial with every field set up front.
     *
     * @param vialId the unique id for this vial
     * @param compoundName the compound name
     * @param deliveryMethod how its taken
     * @param targetedDosemg the target dose in mg
     * @param currentDosemg the current dose in mg
     * @param minTherapeuticDosemg the minimum therapeutic dose in mg
     * @param totalVialMassmg how much the whole vial holds in mg
     * @param concentrationMgPermL the concentration in mg per mL
     * @param frequency how often its dosed
     * @param titrationNeeded whether this vial follows a titration schedule
     * @param titrationIncrementmg the step size for titration in mg
     * @param daysNeeded days per titration step
     */
    public Peptide(int vialId, String compoundName, String deliveryMethod, float targetedDosemg,
                   float currentDosemg, float minTherapeuticDosemg, float totalVialMassmg,
                   float concentrationMgPermL, String frequency, boolean titrationNeeded,
                   float titrationIncrementmg, int daysNeeded) {
        this.vialId = vialId;
        this.compoundName = compoundName;
        this.deliveryMethod = deliveryMethod;
        this.targetedDosemg = targetedDosemg;
        this.currentDosemg = currentDosemg;
        this.minTherapeuticDosemg = minTherapeuticDosemg;
        this.totalVialMassmg = totalVialMassmg;
        this.concentrationmgPermL = concentrationMgPermL;
        this.titrationNeeded = titrationNeeded;
        this.titrationIncrementmg = titrationIncrementmg;
        this.daysNeeded = daysNeeded;
        this.frequency = frequency;
    }

    /**
     * gets the vial id.
     * @return the vial id
     */
    public int getVialId() { return vialId; }
    /**
     * gets the compound name.
     * @return the compound name
     */
    public String getCompoundName() { return compoundName; }
    /**
     * gets the delivery route.
     * @return the delivery route
     */
    public String getDeliveryMethod() { return deliveryMethod; }
    /**
     * gets the dosing frequency.
     * @return the dosing frequency
     */
    public String getFrequency() { return frequency; }
    /**
     * gets the target dose in mg.
     * @return the target dose in mg
     */
    public float getTargetedDosemg() { return targetedDosemg; }
    /**
     * gets the current dose in mg.
     * @return the current dose in mg
     */
    public float getCurrentDosemg() { return currentDosemg; }
    /**
     * gets the minimum therapeutic dose in mg.
     * @return the minimum therapeutic dose in mg
     */
    public float getMinTherapeuticDosemg() { return minTherapeuticDosemg; }
    /**
     * gets the total vial mass in mg.
     * @return the total vial mass in mg
     */
    public float getTotalVialMassmg() { return totalVialMassmg; }
    /**
     * gets the concentration in mg per mL.
     * @return the concentration in mg per mL
     */
    public float getConcentrationmgPermL() { return concentrationmgPermL; }
    /**
     * gets whether titration is on.
     * @return whether titration is on
     */
    public boolean isTitrationNeeded() { return titrationNeeded; }
    /**
     * gets the titration step size in mg.
     * @return the titration step size in mg
     */
    public float getTitrationIncrementmg() { return titrationIncrementmg; }
    /**
     * gets days per titration step.
     * @return days per titration step
     */
    public int getDaysNeeded() { return daysNeeded; }

    /**
     * sets the vial id.
     * @param vialId the new value
     * @return true once set
     */
    public boolean setVialId(int vialId) { this.vialId = vialId; return true; }
    /**
     * sets the compound name.
     * @param compoundName the new value
     * @return true once set
     */
    public boolean setCompoundName(String compoundName) { this.compoundName = compoundName; return true; }
    /**
     * sets the delivery route.
     * @param deliveryMethod the new value
     * @return true once set
     */
    public boolean setDeliveryMethod(String deliveryMethod) { this.deliveryMethod = deliveryMethod; return true; }
    /**
     * sets the target dose in mg.
     * @param targetedDosemg the new value
     * @return true once set
     */
    public boolean setTargetedDosemg(float targetedDosemg) { this.targetedDosemg = targetedDosemg; return true; }
    /**
     * sets the current dose in mg.
     * @param currentDosemg the new value
     * @return true once set
     */
    public boolean setCurrentDosemg(float currentDosemg) { this.currentDosemg = currentDosemg; return true; }
    /**
     * sets the minimum therapeutic dose in mg.
     * @param minTherapeuticDosemg the new value
     * @return true once set
     */
    public boolean setMinTherapeuticDosemg(float minTherapeuticDosemg) { this.minTherapeuticDosemg = minTherapeuticDosemg; return true; }
    /**
     * sets the total vial mass in mg.
     * @param totalVialMassmg the new value
     * @return true once set
     */
    public boolean setTotalVialMassmg(float totalVialMassmg) { this.totalVialMassmg = totalVialMassmg; return true; }
    /**
     * sets the concentration in mg per mL.
     * @param concentrationmgPermL the new value
     * @return true once set
     */
    public boolean setConcentrationmgPermL(float concentrationmgPermL) { this.concentrationmgPermL = concentrationmgPermL; return true; }
    /**
     * sets whether titration is on.
     * @param titrationNeeded the new value
     * @return true once set
     */
    public boolean setTitrationNeeded(boolean titrationNeeded) { this.titrationNeeded = titrationNeeded; return true; }
    /**
     * sets the titration step size in mg.
     * @param titrationIncrementMg the new value
     * @return true once set
     */
    public boolean setTitrationIncrementMg(float titrationIncrementMg) { this.titrationIncrementmg = titrationIncrementMg; return true; }
    /**
     * sets days per titration step.
     * @param daysNeeded the new value
     * @return true once set
     */
    public boolean setDaysNeeded(int daysNeeded) { this.daysNeeded = daysNeeded; return true; }

    /**
     * changes the dosing frequency. returns a boolean like every other
     * setter so they all match and the manager treats them the same
     *
     * @param frequency the new frequency (daily, eod, weekly, or days as text)
     * @return true once the value is set
     */
    /**
     * sets the dosing frequency.
     * @param frequency the new value
     * @return true once set
     */
    public boolean setFrequency(String frequency) { this.frequency = frequency; return true; }

    /*
     method: toString
     purpose: formats one vial as a single readable line, like a receipt, so the
              inventory printout stays aligned in columns. the GUI shows records
              in a table now so this is mostly for logs and quick dumps
     arguments: none
     return: the formatted String for this vial
    */
    @Override
    public String toString() {
        return String.format("Vial ID: %-5d | %-12s | Route: %-13s | Mass: %-5.1f mg | Target Dose: %-4.1f mg | Freq: %-6s",
                vialId, compoundName, deliveryMethod, totalVialMassmg, targetedDosemg, frequency);
    }
}