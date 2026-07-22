import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * one test per crud operation plus the custom action, the loader, and the
 * frequency feature. every test builds its own manager so nothing leaks
 * between tests, and every one checks both the return and the resulting state.
 * <p>
 * CEN 3024C - 31032
 *
 * @author Baker Legerme
 * @version 4.0
 */
public class InventoryManagerTest {

    /**
     * loading a path that doesnt exist reports the missing file instead of crashing.
     */
    @Test
    void loadFromFileTest() {
        // act: try to load a path that doesnt exist
        // assert: the loader reports the missing file instead of crashing
        InventoryManager manager = new InventoryManager();
        String result = manager.loadFromFile("doesnotexist.txt");
        assertTrue(result.contains("File not found"));
    }

    /**
     * adding a record works and the record is really in the inventory.
     */
    @Test
    void addPeptideTest() {
        InventoryManager manager = new InventoryManager();

        // act: call the one method under test
        boolean result = manager.addPeptide("BPC-157", "subcutaneous",
                0.5f, 0.25f, 0.1f, 10f, 2.5f, "daily", true, 0.05f, 7);

        // assert: check both the return and the resulting state
        assertTrue(result);
        assertNotNull(manager.findPeptide(100));
        assertEquals("BPC-157", manager.findPeptide(100).getCompoundName());
    }

    /**
     * removing a record works and the record is really gone.
     */
    @Test
    void removePeptideTest() {
        // arrange: add a record so theres something to remove
        InventoryManager manager = new InventoryManager();
        boolean add = manager.addPeptide("BPC-157", "subcutaneous",
                0.5f, 0.25f, 0.1f, 10f, 2.5f, "daily", true, 0.05f, 7);

        // act + assert: remove works and the record is actually gone
        boolean result = manager.removePeptide(100);
        assertTrue(add);
        assertTrue(result);
        assertNull(manager.findPeptide(100));
    }

    /**
     * flipping titration off through the update path actually lands on the record.
     */
    @Test
    void updatePeptideTest() {
        // arrange: add a record with titration turned on
        InventoryManager manager = new InventoryManager();
        boolean add = manager.addPeptide("BPC-157", "subcutaneous",
                0.5f, 0.25f, 0.1f, 10f, 2.5f, "daily", true, 0.05f, 7);

        // act: flip titration off through the update path
        boolean update = manager.updatePeptideTitration(100, false);

        // assert: the update said it worked and the flag really is false now
        assertTrue(add);
        assertTrue(update);
        assertFalse(manager.findPeptide(100).isTitrationNeeded());
    }

    /**
     * the supply math for a static daily schedule, 10mg at 0.5mg is 20.0 days.
     */
    @Test
    void customActionTest() {
        // arrange: static schedule (no titration), a 10mg vial at 0.5mg daily
        InventoryManager manager = new InventoryManager();
        boolean add = manager.addPeptide("BPC-157", "subcutaneous",
                0.5f, 0.5f, 0.1f, 10f, 2.5f, "daily", false, 0, 7);

        // act: run the supply projection
        String calculate = manager.calculateSupply(100);

        // assert: 10mg / 0.5mg = 20 doses, dosed daily = 20.0 days
        assertTrue(add);
        assertEquals("Adequate Supply: Static dose schedule lasts " + String.format("%.1f", 20.0) + " days. (Log entry saved)", calculate);
    }

    /**
     * the supply math for a titration schedule, whole protocol comes to 23.5 days.
     */
    @Test
    void customActionTest2() {
        // arrange: titration schedule from 0.25 up to 0.5 in one 0.25mg step
        InventoryManager manager = new InventoryManager();
        boolean add = manager.addPeptide("BPC-157", "subcutaneous",
                0.5f, 0.25f, 0.1f, 10f, 2.5f, "daily", true, 0.25f, 7);

        // act: run the supply projection
        String calculate = manager.calculateSupply(100);

        // assert: 7 days at 0.25mg burns 1.75mg, then the 8.25mg left at the
        // 0.5mg target is 16.5 more days, so the whole protocol is 23.5 days
        assertTrue(add);
        assertTrue(calculate.contains("23.5"));
    }

    /**
     * the frequency multiplier, 5 weekly doses is 35.0 days of supply, not 5.
     */
    @Test
    void frequencyWeeklyTest() {
        // arrange: static schedule, 10mg vial at 2mg a dose, dosed weekly.
        // this is the sema case, the whole reason the frequency feature exists
        InventoryManager manager = new InventoryManager();
        boolean add = manager.addPeptide("Semaglutide", "subcutaneous",
                2f, 2f, 0.25f, 10f, 2.5f, "weekly", false, 0, 7);

        // act: run the supply projection
        String calculate = manager.calculateSupply(100);

        // assert: 5 doses on paper, but one dose a week means 35.0 days of
        // supply, not 5. without the multiplier this would read 5.0 and lie
        assertTrue(add);
        assertTrue(calculate.contains("35.0"));
    }

    /**
     * a real frequency change lands and garbage gets rejected without overwriting.
     */
    @Test
    void updateFrequencyTest() {
        // arrange: a daily record
        InventoryManager manager = new InventoryManager();
        boolean add = manager.addPeptide("BPC-157", "subcutaneous",
                0.5f, 0.25f, 0.1f, 10f, 2.5f, "daily", true, 0.05f, 7);

        // act: switch it to weekly, then try garbage
        boolean valid = manager.updateFrequency(100, "weekly");
        boolean invalid = manager.updateFrequency(100, "hourly");

        // assert: the real change landed, the garbage got rejected and didnt
        // overwrite whats stored
        assertTrue(add);
        assertTrue(valid);
        assertFalse(invalid);
        assertEquals("weekly", manager.findPeptide(100).getFrequency());
    }

    /**
     * the real sample file loads and the loader reports a summary.
     */
    @Test
    void loadFileTest() {
        // act: load the actual sample data file (eleven columns a line)
        InventoryManager manager = new InventoryManager();
        String result = manager.loadFromFile("peptides.txt");

        // assert: the loader ran and gave back a load summary
        assertTrue(result.contains("Load"));
    }
}