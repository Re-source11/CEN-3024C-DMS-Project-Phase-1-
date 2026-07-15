import javax.swing.*;
import java.awt.*;

public class MainGUI {
    private InventoryManager manager;
    private JTextArea displayArea;

    public MainGUI() {
        manager = new InventoryManager();

        // 1. Create the Main Window
        JFrame frame = new JFrame("Peptide Database Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(850, 500);
        frame.setLayout(new BorderLayout());

        // 2. Create the Display Area
        displayArea = new JTextArea();
        displayArea.setEditable(false);
        displayArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        displayArea.setText("=== Peptide Database Management System ===\nReady.\n");
        JScrollPane scrollPane = new JScrollPane(displayArea);

        // 3. Create the Button Panel 
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(2, 4, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnLoad = new JButton("Load Data");
        JButton btnDisplay = new JButton("Display Inventory");
        JButton btnAdd = new JButton("Add Peptide");
        JButton btnUpdate = new JButton("Update Peptide");
        JButton btnRemove = new JButton("Remove Peptide");
        JButton btnSupply = new JButton("Calculate Supply");
        JButton btnExit = new JButton("Exit");

        buttonPanel.add(btnLoad);
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnRemove);
        buttonPanel.add(btnDisplay);
        buttonPanel.add(btnSupply);
        buttonPanel.add(new JLabel("")); // Empty placeholder for grid alignment
        buttonPanel.add(btnExit);


        // LOAD DATA
        btnLoad.addActionListener(e -> {
            String path = JOptionPane.showInputDialog(frame, "Enter the path to your data file:");
            if (path != null && !path.trim().isEmpty()) {
                String result = manager.loadFromFile(path);
                displayArea.setText(result + "\n\n" + manager.getAllPeptidesAsString());
            }
        });

        // ADD PEPTIDE
        btnAdd.addActionListener(e -> {
            // Creates a custom form panel for adding a peptide
            JTextField nameField = new JTextField();
            String[] routes = {"subcutaneous", "topical", "intramuscular"};
            JComboBox<String> routeBox = new JComboBox<>(routes);
            JTextField targetField = new JTextField();
            JTextField currentField = new JTextField();
            JTextField minField = new JTextField();
            JTextField massField = new JTextField();
            JTextField concField = new JTextField();
            JCheckBox titrationCheck = new JCheckBox("Yes");
            JTextField incField = new JTextField("0.0");
            JTextField daysField = new JTextField("1");

            Object[] message = {
                    "Compound Name:", nameField,
                    "Delivery Route:", routeBox,
                    "Target Dose (mg):", targetField,
                    "Current Dose (mg):", currentField,
                    "Min Therapeutic Dose (mg):", minField,
                    "Total Vial Mass (mg):", massField,
                    "Concentration (mg/mL):", concField,
                    "Titration Needed:", titrationCheck,
                    "Titration Increment (mg):", incField,
                    "Days per Step:", daysField
            };

            int option = JOptionPane.showConfirmDialog(frame, message, "Add New Peptide", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                try {
                    boolean success = manager.addPeptide(
                            nameField.getText().trim(),
                            routeBox.getSelectedItem().toString(),
                            Float.parseFloat(targetField.getText()),
                            Float.parseFloat(currentField.getText()),
                            Float.parseFloat(minField.getText()),
                            Float.parseFloat(massField.getText()),
                            Float.parseFloat(concField.getText()),
                            titrationCheck.isSelected(),
                            Float.parseFloat(incField.getText()),
                            Integer.parseInt(daysField.getText())
                    );
                    displayArea.setText(success ? "Vial added successfully.\n\n" : "System Error: Failed to add.\n\n");
                    displayArea.append(manager.getAllPeptidesAsString());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Invalid input. Please ensure all number fields contain valid numbers.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // UPDATE PEPTIDE
        btnUpdate.addActionListener(e -> {
            String idStr = JOptionPane.showInputDialog(frame, "Enter Vial ID to update:");
            if (idStr != null && !idStr.trim().isEmpty()) {
                try {
                    int id = Integer.parseInt(idStr);
                    if (manager.findPeptide(id) == null) {
                        JOptionPane.showMessageDialog(frame, "Error: ID not found.", "Not Found", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    String[] options = {
                            "Compound Name", "Delivery Route", "Target Dose", "Current Dose",
                            "Min Therapeutic Dose", "Total Vial Mass", "Concentration",
                            "Titration Needed", "Titration Increment", "Days per Step"
                    };

                    String choice = (String) JOptionPane.showInputDialog(frame, "Select field to update for Vial " + id + ":",
                            "Update Peptide", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                    if (choice != null) {
                        String newValue = JOptionPane.showInputDialog(frame, "Enter new value for " + choice + ":");
                        if (newValue != null && !newValue.trim().isEmpty()) {
                            boolean success = false;
                            switch (choice) {
                                case "Compound Name": success = manager.updatePeptideName(id, newValue); break;
                                case "Delivery Route": success = manager.updatePeptideRoute(id, newValue); break;
                                case "Target Dose": success = manager.updatePeptideTargetDose(id, Float.parseFloat(newValue)); break;
                                case "Current Dose": success = manager.updatePeptideCurrentDose(id, Float.parseFloat(newValue)); break;
                                case "Min Therapeutic Dose": success = manager.updatePeptideMinDose(id, Float.parseFloat(newValue)); break;
                                case "Total Vial Mass": success = manager.updatePeptideTotalMass(id, Float.parseFloat(newValue)); break;
                                case "Concentration": success = manager.updatePeptideConcentration(id, Float.parseFloat(newValue)); break;
                                case "Titration Needed": success = manager.updatePeptideTitration(id, Boolean.parseBoolean(newValue)); break;
                                case "Titration Increment": success = manager.updatePeptideIncrement(id, Float.parseFloat(newValue)); break;
                                case "Days per Step": success = manager.updatePeptideDays(id, Integer.parseInt(newValue)); break;
                            }
                            displayArea.setText(success ? "Updated successfully.\n\n" : "Error: Update failed. Check validation rules.\n\n");
                            displayArea.append(manager.getAllPeptidesAsString());
                        }
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Error: Please enter valid numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // REMOVE PEPTIDE
        btnRemove.addActionListener(e -> {
            String idStr = JOptionPane.showInputDialog(frame, "Enter Vial ID to remove:");
            if (idStr != null && !idStr.trim().isEmpty()) {
                try {
                    int id = Integer.parseInt(idStr);
                    boolean success = manager.removePeptide(id);
                    displayArea.setText(success ? "Record permanently deleted.\n\n" : "Error: ID not found.\n\n");
                    displayArea.append(manager.getAllPeptidesAsString());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Error: Please enter a valid numeric ID.", "Input Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // DISPLAY INVENTORY
        btnDisplay.addActionListener(e -> {
            displayArea.setText(manager.getAllPeptidesAsString());
        });

        // CALCULATE SUPPLY
        btnSupply.addActionListener(e -> {
            String idStr = JOptionPane.showInputDialog(frame, "Enter Vial ID to Calculate Supply:");
            if (idStr != null && !idStr.trim().isEmpty()) {
                try {
                    int id = Integer.parseInt(idStr);
                    String result = manager.calculateSupply(id);
                    displayArea.setText("Supply Result for Vial " + id + ":\n\n" + result);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Error: Please enter a valid numeric ID.", "Input Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // EXIT
        btnExit.addActionListener(e -> System.exit(0));

       // Adding everything to the window and show it
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainGUI::new);
    }
}