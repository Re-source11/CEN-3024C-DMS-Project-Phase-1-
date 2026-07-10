import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class InventoryManagerTest {


    @Test
    void loadFromFileTest() {
        InventoryManager manager = new InventoryManager();
        String result = manager.loadFromFile("doesnotexist.txt");
        assertTrue(result.contains("File not found"));
    }

    @Test
    void addPeptideTest() {

        InventoryManager manager = new InventoryManager();

        // act: call the one method under test
        boolean result = manager.addPeptide("BPC-157", "subcutaneous",
                0.5f, 0.25f, 0.1f, 10f, 2.5f, true, 0.05f, 7);

        // assert: check both the return and the resulting state
        assertTrue(result);
        assertNotNull(manager.findPeptide(100));
        assertEquals("BPC-157", manager.findPeptide(100).getCompoundName());


    }

    @Test
    void removePeptideTest(){

        InventoryManager manager = new InventoryManager();
        boolean add =  manager.addPeptide("BPC-157", "subcutaneous",
                0.5f, 0.25f, 0.1f, 10f, 2.5f, true, 0.05f, 7);

        boolean result = manager.removePeptide(100);
        assertTrue(result);
        assertNull(manager.findPeptide(100));


    }

    @Test
    void updatePeptideTest(){

        InventoryManager manager = new InventoryManager();
        boolean add =  manager.addPeptide("BPC-157", "subcutaneous",
                0.5f, 0.25f, 0.1f, 10f, 2.5f, true, 0.05f, 7);

        boolean update = manager.updatePeptideTitration(100, false);

        assertTrue(add);

        assertFalse(manager.findPeptide(100).isTitrationNeeded());
    }


     @Test
    void customActionTest(){

        InventoryManager manager = new InventoryManager();
         boolean add =  manager.addPeptide("BPC-157", "subcutaneous",
                 0.5f, 0.5f, 0.1f, 10f, 2.5f, false, 0, 7);

        String calculate = manager.calculateSupply(100);

        assertTrue(add);
        assertEquals("Adequate Supply: Static dose schedule lasts " + String.format("%.1f", 20.0) + " days. (Log entry saved)", calculate);
    }

    @Test
    void customActionTest2(){
        InventoryManager manager = new InventoryManager();
        boolean add =  manager.addPeptide("BPC-157", "subcutaneous",
                0.5f, 0.25f, 0.1f, 10f, 2.5f, true, 0.25f, 7);

        String calculate = manager.calculateSupply(100);

        assertTrue(add);
        assertTrue(calculate.contains("23.5"));
    }

    @Test
    void loadFileTest(){

        InventoryManager manager = new InventoryManager();
        String result = manager.loadFromFile("peptides.txt");

        assertTrue(result.contains("Load"));

    }
}



