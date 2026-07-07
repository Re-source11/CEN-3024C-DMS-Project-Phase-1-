/*
 Baker Legerme
 CEN 3024C - 31032
 July 3rd, 2026

 class: Peptide
 This is the object class for the DMS.
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
    public Peptide(int vialId, String compoundName, String deliveryMethod, float targetedDosemg,
                   float currentDosemg, float minTherapeuticDosemg, float totalVialMassmg,
                   float concentrationMgPermL, boolean titrationNeeded, float titrationIncrementmg, int daysNeeded) {
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
    }
    public int getVialId() { return vialId; }
    public String getCompoundName() { return compoundName; }
    public String getDeliveryMethod() { return deliveryMethod; }
    public float getTargetedDosemg() { return targetedDosemg; }
    public float getCurrentDosemg() { return currentDosemg; }
    public float getMinTherapeuticDosemg() { return minTherapeuticDosemg; }
    public float getTotalVialMassmg() { return totalVialMassmg; }
    public float getConcentrationmgPermL() { return concentrationmgPermL; }
    public boolean isTitrationNeeded() { return titrationNeeded; }
    public float getTitrationIncrementmg() { return titrationIncrementmg; }
    public int getDaysNeeded() { return daysNeeded; }
    public boolean setVialId(int vialId) { this.vialId = vialId; return true; }
    public boolean setCompoundName(String compoundName) { this.compoundName = compoundName; return true; }
    public boolean setDeliveryMethod(String deliveryMethod) { this.deliveryMethod = deliveryMethod; return true; }
    public boolean setTargetedDosemg(float targetedDosemg) { this.targetedDosemg = targetedDosemg; return true; }
    public boolean setCurrentDosemg(float currentDosemg) { this.currentDosemg = currentDosemg; return true; }
    public boolean setMinTherapeuticDosemg(float minTherapeuticDosemg) { this.minTherapeuticDosemg = minTherapeuticDosemg; return true; }
    public boolean setTotalVialMassmg(float totalVialMassmg) { this.totalVialMassmg = totalVialMassmg; return true; }
    public boolean setConcentrationmgPermL(float concentrationmgPermL) { this.concentrationmgPermL = concentrationmgPermL; return true; }
    public boolean setTitrationNeeded(boolean titrationNeeded) { this.titrationNeeded = titrationNeeded; return true; }
    public boolean setTitrationIncrementMg(float titrationIncrementMg) { this.titrationIncrementmg = titrationIncrementMg; return true; }
    public boolean setDaysNeeded(int daysNeeded) { this.daysNeeded = daysNeeded; return true; }

    /*
     method: toString
     purpose: formats one vial as a single readable line, like a receipt, so the
              inventory printout stays aligned in columns
     arguments: none
     return: the formatted String for this vial
    */
    @Override
    public String toString() {
        return String.format("Vial ID: %-5d | %-12s | Route: %-13s | Mass: %-5.1f mg | Target Dose: %-4.1f mg",
                vialId, compoundName, deliveryMethod, totalVialMassmg, targetedDosemg);
    }
}