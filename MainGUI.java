import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/*
 Baker Legerme
 CEN 3024C - 31032
 July 19th, 2026

 class: MainGUI
 The window. users only ever touch this.

 Overall program objective: This is a DMS for tracking peptides. load,
 display, add, update, remove, plus the custom supply projection, with the
 records living in a real table and saved to MySQL when a database is
 reachable so the data survives the program closing.

 this class only draws things and collects input. every button is just a
 call into InventoryManager, which has no idea a GUI even exists. the
 selected table row IS the input for update, remove, and calculate, so
 theres no way to target a record that doesnt exist.
*/
public class MainGUI {
    private InventoryManager manager;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JFrame frame;
    private String dbStatus = "use the Database button to connect"; // shown in the status bar until a database is hooked up

    // one table column per Peptide field
    private static final String[] COLUMNS = {
            "Vial ID", "Compound", "Route", "Target (mg)", "Current (mg)",
            "Min (mg)", "Mass (mg)", "Conc (mg/mL)", "Frequency",
            "Titration", "Increment (mg)", "Days/Step"
    };

    public MainGUI() {
        manager = new InventoryManager();

        // 1. the main window
        frame = new JFrame("Peptide Database Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 560);
        frame.setIconImage(createAppIcon());
        frame.setLayout(new BorderLayout(0, 0));

        // 2. the table. the data itself is the interface now
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // edits go through the update dialog and its validation instead
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        // double clicking a row pops up just that one peptide, so the user can
        // look at a single record without reading across twelve columns
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) { showSelectedDetail(); }
            }
        });
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 0, 10),
                BorderFactory.createTitledBorder("Inventory")));

        // 3. status bar. record count and storage mode, updated on every refresh
        statusLabel = new JLabel(" ");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));

        // 4. the button panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 7, 8, 8));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 4, 10));

        JButton btnLoad = new JButton("Load Data...");
        JButton btnDatabase = new JButton("Database...");
        JButton btnAdd = new JButton("Add Peptide");
        JButton btnUpdate = new JButton("Update Selected");
        JButton btnRemove = new JButton("Remove Selected");
        JButton btnSupply = new JButton("Calculate Supply");
        JButton btnExit = new JButton("Exit");

        buttonPanel.add(btnLoad);
        buttonPanel.add(btnDatabase);
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnRemove);
        buttonPanel.add(btnSupply);
        buttonPanel.add(btnExit);

        btnLoad.addActionListener(e -> loadData());
        btnDatabase.addActionListener(e -> connectDatabaseDialog());
        btnAdd.addActionListener(e -> addPeptide());
        btnUpdate.addActionListener(e -> updateSelected());
        btnRemove.addActionListener(e -> removeSelected());
        btnSupply.addActionListener(e -> calculateSelected());
        btnExit.addActionListener(e -> System.exit(0));

        JPanel south = new JPanel(new BorderLayout());
        south.add(buttonPanel, BorderLayout.CENTER);
        south.add(statusLabel, BorderLayout.SOUTH);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(south, BorderLayout.SOUTH);

        refreshTable();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /*
     method: driverPresent
     purpose: checks if a MySQL or MariaDB driver class can even be found, so
              a missing driver jar gets reported as its own separate problem
     arguments: none
     return: true if either driver is on the classpath
    */
    private boolean driverPresent() {
        String[] drivers = {"com.mysql.cj.jdbc.Driver", "com.mysql.jdbc.Driver", "org.mariadb.jdbc.Driver"};
        for (String d : drivers) {
            try { Class.forName(d); return true; } catch (ClassNotFoundException ignored) { }
        }
        return false;
    }

    /*
     method: refreshTable
     purpose: rebuilds the table from the manager after every operation so the
              screen always matches whats actually stored. also refreshes the
              record count and storage mode down in the status bar
     arguments: none
     return: nothing
    */
    private void refreshTable() {
        tableModel.setRowCount(0);
        List<Peptide> all = manager.getAllPeptides();
        for (Peptide p : all) {
            tableModel.addRow(new Object[]{
                    p.getVialId(), p.getCompoundName(), p.getDeliveryMethod(),
                    p.getTargetedDosemg(), p.getCurrentDosemg(), p.getMinTherapeuticDosemg(),
                    p.getTotalVialMassmg(), p.getConcentrationmgPermL(), p.getFrequency(),
                    p.isTitrationNeeded() ? "Yes" : "No", p.getTitrationIncrementmg(), p.getDaysNeeded()
            });
        }
        String mode = manager.isDatabaseConnected()
                ? "Database (MySQL)"
                : "In-memory - " + dbStatus;
        statusLabel.setText(all.size() + " record" + (all.size() == 1 ? "" : "s") + "  |  Storage: " + mode);
    }

    /*
     method: selectedVialId
     purpose: reads the vial id right out of column zero of the selected row.
              the selection is the input now, nobody types ids anymore, so its
              impossible to target a record that isnt on the screen
     arguments: none
     return: the selected records id, or -1 (after a warning) if nothing is selected
    */
    private int selectedVialId() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(frame, "Please select a record in the table first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return -1;
        }
        return (Integer) tableModel.getValueAt(row, 0);
    }

    /*
     method: loadData
     purpose: opens a real file picker instead of asking the user to type a
              path, then hands the chosen file to the same phase 1 loader, so
              the validation and line skip reporting run unchanged underneath
     arguments: none
     return: nothing
    */
    private void loadData() {
        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle("Choose a peptide data file");

        // the format rules ride along on the side of the picker, same idea as
        // the old CLI printing the rules before asking for a path
        JTextArea rules = new JTextArea(
                "File Format Rules\n"
                        + "-----------------\n"
                        + "One record per line, 11 comma\n"
                        + "separated values in this order:\n\n"
                        + "name,route,target,current,min,\n"
                        + "totalMass,concentration,frequency,\n"
                        + "titration,increment,days\n\n"
                        + "Example:\n"
                        + "BPC-157,subcutaneous,0.5,0.25,\n"
                        + "0.1,10,2.5,daily,true,0.05,7\n\n"
                        + "- name: 3-20 chars, letters/\n"
                        + "  numbers/hyphens only\n"
                        + "- route: subcutaneous, topical,\n"
                        + "  or intramuscular\n"
                        + "- doses/mass: over 0, max 100mg,\n"
                        + "  current and min cannot exceed\n"
                        + "  target\n"
                        + "- concentration: over 0, up to\n"
                        + "  1000 mg/mL\n"
                        + "- frequency: daily, eod, weekly,\n"
                        + "  or 1-10 days\n"
                        + "- titration: true or false\n"
                        + "- days: whole number, 1 to 365\n\n"
                        + "Bad lines are skipped and\n"
                        + "reported by line number.");
        rules.setEditable(false);
        rules.setFont(new Font("Monospaced", Font.PLAIN, 11));
        rules.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        chooser.setAccessory(new JScrollPane(rules));

        int result = chooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            String summary = manager.loadFromFile(chooser.getSelectedFile().getAbsolutePath());
            refreshTable();
            JOptionPane.showMessageDialog(frame, summary, "Load Result", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /*
     method: addPeptide
     purpose: the add form, running the exact phase 1 rules. route is a
              dropdown so a bad route cant even be typed, every field label
              says its allowed range up front, the titration boxes stay greyed
              out until the checkbox is on, and breaking a rule reopens the
              form with everything still filled in and a message saying which
              rule it was. same loop-until-valid idea as the old CLI, the user
              can never get bad data in or crash anything
     arguments: none
     return: nothing
    */
    private void addPeptide() {
        JTextField nameField = new JTextField();
        String[] routes = {"subcutaneous", "topical", "intramuscular"};
        JComboBox<String> routeBox = new JComboBox<>(routes);
        JTextField targetField = new JTextField();
        JTextField currentField = new JTextField();
        JTextField minField = new JTextField();
        JTextField massField = new JTextField();
        JTextField concField = new JTextField();
        JTextField freqField = new JTextField("daily");
        JCheckBox titrationCheck = new JCheckBox("Yes");
        JTextField incField = new JTextField("0.0");
        JTextField daysField = new JTextField("1");

        // conditional titration fields, greyed out until the box is checked
        incField.setEnabled(false);
        daysField.setEnabled(false);
        titrationCheck.addItemListener(e -> {
            boolean on = titrationCheck.isSelected();
            incField.setEnabled(on);
            daysField.setEnabled(on);
            if (!on) { incField.setText("0.0"); daysField.setText("1"); }
        });

        Object[] message = {
                "Compound Name (3-20 chars, letters/numbers/hyphens):", nameField,
                "Delivery Route:", routeBox,
                "Target Dose (mg, 0.1 - 100):", targetField,
                "Current Dose (mg, 0.1 up to target):", currentField,
                "Min Therapeutic Dose (mg, 0.1 up to target):", minField,
                "Total Vial Mass (mg, 0.1 - 100):", massField,
                "Concentration (mg/mL, 0.1 - 1000):", concField,
                "Frequency (daily/eod/weekly or 1-10 days):", freqField,
                "Titration Needed:", titrationCheck,
                "Titration Increment (mg, 0.1 up to target):", incField,
                "Days per Step (1 - 365):", daysField
        };

        // same loop idea as the old CLI, keep asking until its right or they
        // cancel. the dialog reopens with everything they typed still in it
        while (true) {
            int option = JOptionPane.showConfirmDialog(frame, message, "Add New Peptide", JOptionPane.OK_CANCEL_OPTION);
            if (option != JOptionPane.OK_OPTION) { return; }

            String error = null;
            String name = nameField.getText().trim();
            float target = 0, current = 0, min = 0, mass = 0, conc = 0, increment = 0;
            int days = 1;
            String frequency = freqField.getText().trim().toLowerCase();
            boolean titration = titrationCheck.isSelected();

            // the exact phase 1 rules, checked in the same order the CLI asked
            if (!name.matches("^[a-zA-Z0-9-]{3,20}$")) {
                error = "Compound name must be 3-20 characters, letters, numbers, or hyphens only.";
            }
            if (error == null) {
                try {
                    target = Float.parseFloat(targetField.getText().trim());
                    current = Float.parseFloat(currentField.getText().trim());
                    min = Float.parseFloat(minField.getText().trim());
                    mass = Float.parseFloat(massField.getText().trim());
                    conc = Float.parseFloat(concField.getText().trim());
                    increment = Float.parseFloat(incField.getText().trim());
                    days = Integer.parseInt(daysField.getText().trim());
                } catch (NumberFormatException ex) {
                    error = "Every number field needs a valid number in it.";
                }
            }
            if (error == null && (target < 0.1f || target > 100f)) { error = "Target dose must be between 0.1 and 100 mg."; }
            if (error == null && (current < 0.1f || current > target)) { error = "Current dose must be between 0.1 and the target dose."; }
            if (error == null && (min < 0.1f || min > target)) { error = "Min therapeutic dose must be between 0.1 and the target dose."; }
            if (error == null && (mass < 0.1f || mass > 100f)) { error = "Total vial mass must be between 0.1 and 100 mg."; }
            if (error == null && (conc < 0.1f || conc > 1000f)) { error = "Concentration must be between 0.1 and 1000 mg/mL."; }
            if (error == null && manager.frequencyToDaysBetween(frequency) < 0f) { error = "Frequency must be daily, eod, weekly, or a number of days from 1 to 10."; }
            if (error == null && titration && (increment < 0.1f || increment > target)) { error = "Titration increment must be between 0.1 and the target dose."; }
            if (error == null && titration && (days < 1 || days > 365)) { error = "Days per step must be a whole number from 1 to 365."; }

            if (error != null) {
                JOptionPane.showMessageDialog(frame, error, "Invalid Input", JOptionPane.ERROR_MESSAGE);
                continue; // dialog comes back with their values still filled in
            }

            boolean added = manager.addPeptide(name, routeBox.getSelectedItem().toString(),
                    target, current, min, mass, conc, frequency, titration,
                    titration ? increment : 0f, titration ? days : 1);
            refreshTable();
            if (!added) {
                JOptionPane.showMessageDialog(frame, "System Error: Failed to add.", "Error", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }
    }

    /*
     method: updateSelected
     purpose: edits one field of whatever record is selected in the table. the
              id comes from the selection, the field from a dropdown, every
              prompt says the allowed range up front like the old CLI prompts
              did, route and titration are pick-lists so they cant be typed
              wrong, and a broken rule gets named and re-asked until the value
              passes or the user cancels, never kicked back out
     arguments: none
     return: nothing
    */
    private void updateSelected() {
        int id = selectedVialId();
        if (id < 0) { return; }
        Peptide record = manager.findPeptide(id);
        if (record == null) { return; }

        String[] options = {
                "Compound Name", "Delivery Route", "Target Dose", "Current Dose",
                "Min Therapeutic Dose", "Total Vial Mass", "Concentration",
                "Frequency", "Titration Needed", "Titration Increment", "Days per Step"
        };

        String choice = (String) JOptionPane.showInputDialog(frame, "Select field to update for Vial " + id + ":",
                "Update Peptide", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice == null) { return; }

        // every text field below loops until the value passes or the user
        // cancels, same as the CLI kept re-asking. a broken rule gets named
        // and the prompt comes right back instead of kicking them out
        boolean success = false;
        switch (choice) {
            case "Compound Name": {
                while (true) {
                    String v = JOptionPane.showInputDialog(frame,
                            "Enter new Compound Name (3-20 chars, letters/numbers/hyphens):");
                    if (v == null) { return; }
                    if (v.trim().matches("^[a-zA-Z0-9-]{3,20}$")) {
                        success = manager.updatePeptideName(id, v.trim());
                        break;
                    }
                    ruleMessage("Compound name must be 3-20 characters, letters, numbers, or hyphens only.");
                }
                break;
            }
            case "Delivery Route": {
                // a dropdown instead of typing, so a wrong route is impossible
                String[] routes = {"subcutaneous", "topical", "intramuscular"};
                String v = (String) JOptionPane.showInputDialog(frame, "Select new Delivery Route:",
                        "Update Route", JOptionPane.QUESTION_MESSAGE, null, routes, record.getDeliveryMethod());
                if (v == null) { return; }
                success = manager.updatePeptideRoute(id, v);
                break;
            }
            case "Target Dose": {
                float floor = Math.max(record.getCurrentDosemg(), record.getMinTherapeuticDosemg());
                while (true) {
                    String v = JOptionPane.showInputDialog(frame,
                            "Enter new Target Dose in mg (" + floor + " - 100.0):");
                    if (v == null) { return; }
                    try {
                        float f = Float.parseFloat(v);
                        if (f <= 100f && manager.updatePeptideTargetDose(id, f)) { success = true; break; }
                        ruleMessage("Target dose must be at least the current dose (" + floor + ") and at most 100 mg.");
                    } catch (NumberFormatException ex) { ruleMessage("Please enter a valid number."); }
                }
                break;
            }
            case "Current Dose": {
                while (true) {
                    String v = JOptionPane.showInputDialog(frame,
                            "Enter new Current Dose in mg (0.1 - " + record.getTargetedDosemg() + "):");
                    if (v == null) { return; }
                    try {
                        if (manager.updatePeptideCurrentDose(id, Float.parseFloat(v))) { success = true; break; }
                        ruleMessage("Current dose must be between 0.1 and the target dose (" + record.getTargetedDosemg() + ").");
                    } catch (NumberFormatException ex) { ruleMessage("Please enter a valid number."); }
                }
                break;
            }
            case "Min Therapeutic Dose": {
                while (true) {
                    String v = JOptionPane.showInputDialog(frame,
                            "Enter new Min Therapeutic Dose in mg (0.1 - " + record.getTargetedDosemg() + "):");
                    if (v == null) { return; }
                    try {
                        if (manager.updatePeptideMinDose(id, Float.parseFloat(v))) { success = true; break; }
                        ruleMessage("Min dose must be between 0.1 and the target dose (" + record.getTargetedDosemg() + ").");
                    } catch (NumberFormatException ex) { ruleMessage("Please enter a valid number."); }
                }
                break;
            }
            case "Total Vial Mass": {
                while (true) {
                    String v = JOptionPane.showInputDialog(frame, "Enter new Total Vial Mass in mg (0.1 - 100.0):");
                    if (v == null) { return; }
                    try {
                        if (manager.updatePeptideTotalMass(id, Float.parseFloat(v))) { success = true; break; }
                        ruleMessage("Total vial mass must be between 0.1 and 100 mg.");
                    } catch (NumberFormatException ex) { ruleMessage("Please enter a valid number."); }
                }
                break;
            }
            case "Concentration": {
                while (true) {
                    String v = JOptionPane.showInputDialog(frame, "Enter new Concentration in mg/mL (0.1 - 1000.0):");
                    if (v == null) { return; }
                    try {
                        if (manager.updatePeptideConcentration(id, Float.parseFloat(v))) { success = true; break; }
                        ruleMessage("Concentration must be between 0.1 and 1000 mg/mL.");
                    } catch (NumberFormatException ex) { ruleMessage("Please enter a valid number."); }
                }
                break;
            }
            case "Frequency": {
                while (true) {
                    String v = JOptionPane.showInputDialog(frame,
                            "Enter new Frequency (daily, eod, weekly, or 1-10 days):");
                    if (v == null) { return; }
                    if (manager.updateFrequency(id, v)) { success = true; break; }
                    ruleMessage("Frequency must be daily, eod, weekly, or a number of days from 1 to 10.");
                }
                break;
            }
            case "Titration Needed": {
                // yes or no buttons, so nothing to type wrong
                int v = JOptionPane.showConfirmDialog(frame, "Is titration needed for this vial?",
                        "Update Titration", JOptionPane.YES_NO_OPTION);
                if (v != JOptionPane.YES_OPTION && v != JOptionPane.NO_OPTION) { return; }
                success = manager.updatePeptideTitration(id, v == JOptionPane.YES_OPTION);
                break;
            }
            case "Titration Increment": {
                while (true) {
                    String v = JOptionPane.showInputDialog(frame,
                            "Enter new Titration Increment in mg (0.1 - " + record.getTargetedDosemg() + "):");
                    if (v == null) { return; }
                    try {
                        if (manager.updatePeptideIncrement(id, Float.parseFloat(v))) { success = true; break; }
                        ruleMessage("Increment must be between 0.1 and the target dose (" + record.getTargetedDosemg() + ").");
                    } catch (NumberFormatException ex) { ruleMessage("Please enter a valid number."); }
                }
                break;
            }
            case "Days per Step": {
                while (true) {
                    String v = JOptionPane.showInputDialog(frame, "Enter new Days per Step (1 - 365):");
                    if (v == null) { return; }
                    try {
                        if (manager.updatePeptideDays(id, Integer.parseInt(v))) { success = true; break; }
                        ruleMessage("Days per step must be a whole number from 1 to 365.");
                    } catch (NumberFormatException ex) { ruleMessage("Please enter a whole number."); }
                }
                break;
            }
        }
        refreshTable();
        if (!success) {
            JOptionPane.showMessageDialog(frame, "Update failed.", "Update Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    /*
     method: ruleMessage
     purpose: helper that shows which validation rule an update broke, so the
              user gets told the actual rule instead of a generic failed message
     arguments: text: the rule that got broken
     return: nothing
    */
    private void ruleMessage(String text) {
        JOptionPane.showMessageDialog(frame, text, "Invalid Input", JOptionPane.ERROR_MESSAGE);
    }

    /*
     method: removeSelected
     purpose: deletes the selected record, with a confirm popup first so one
              stray click can never destroy data
     arguments: none
     return: nothing
    */
    private void removeSelected() {
        int id = selectedVialId();
        if (id < 0) { return; }
        int confirm = JOptionPane.showConfirmDialog(frame,
                "Permanently delete Vial " + id + "?", "Confirm Removal", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            manager.removePeptide(id);
            refreshTable();
        }
    }

    /*
     method: calculateSelected
     purpose: runs the custom supply projection on the selected record
     arguments: none
     return: nothing
    */
    private void calculateSelected() {
        int id = selectedVialId();
        if (id < 0) { return; }
        String result = manager.calculateSupply(id);
        JOptionPane.showMessageDialog(frame, result, "Supply Result for Vial " + id, JOptionPane.INFORMATION_MESSAGE);
    }

    /*
     method: connectDatabaseDialog
     purpose: asks the user for the mysql server address, port, database name,
              username, and password, then concatenates the jdbc url out of
              those pieces. nothing about the connection is hardcoded or
              assumed, the user supplies all of it. blank fields get called
              out, a bad address or wrong password shows the databases own
              error and re-asks, and cancel just leaves the program in memory.
              nothing typed here can crash anything
     arguments: none
     return: nothing
    */
    private void connectDatabaseDialog() {
        JTextField hostField = new JTextField("localhost", 24);
        JTextField portField = new JTextField("3306", 24);
        JTextField dbField = new JTextField("peptide_dms", 24);
        JTextField userField = new JTextField("", 24);
        JPasswordField passField = new JPasswordField("", 24);

        while (true) {
            Object[] message = {
                    "MySQL Server Address:", hostField,
                    "Port:", portField,
                    "Database Name:", dbField,
                    "Username:", userField,
                    "Password:", passField
            };
            int option = JOptionPane.showConfirmDialog(frame, message, "Connect to Database", JOptionPane.OK_CANCEL_OPTION);
            if (option != JOptionPane.OK_OPTION) { return; } // cancel, stay in memory

            String host = hostField.getText().trim();
            String port = portField.getText().trim();
            String dbName = dbField.getText().trim();
            String user = userField.getText().trim();
            String password = new String(passField.getPassword());

            // blank details get their own message instead of a mystery failure
            if (host.isEmpty() || port.isEmpty() || dbName.isEmpty() || user.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Server address, port, database name, and username cannot be blank.",
                        "Missing Details", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            if (!port.matches("\\d{1,5}")) {
                JOptionPane.showMessageDialog(frame, "Port must be a number.",
                        "Missing Details", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            if (!driverPresent()) {
                JOptionPane.showMessageDialog(frame,
                        "No MySQL driver on the classpath, so a connection is impossible.\nAdd the Connector/J jar and relaunch.",
                        "Driver Missing", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // the jdbc url is concatenated together from what the user typed,
            // nothing about the connection lives in the code
            String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName;

            if (manager.connectDatabase(url, user, password)) {
                dbStatus = "";
                refreshTable();
                JOptionPane.showMessageDialog(frame, "Connected. Stored records loaded from the database.",
                        "Database Connected", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            // improper details, show the databases own reason and let them retry
            JOptionPane.showMessageDialog(frame,
                    "Could not connect:\n" + manager.getLastDatabaseError(),
                    "Connection Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    /*
     method: showSelectedDetail
     purpose: shows every field of just the double clicked record in one popup,
              so the user can view a single peptide without scanning the whole
              table row across all twelve columns
     arguments: none
     return: nothing
    */
    private void showSelectedDetail() {
        int id = selectedVialId();
        if (id < 0) { return; }
        Peptide p = manager.findPeptide(id);
        if (p == null) { return; }
        String detail = "Vial ID: " + p.getVialId()
                + "\nCompound: " + p.getCompoundName()
                + "\nRoute: " + p.getDeliveryMethod()
                + "\nTarget Dose: " + p.getTargetedDosemg() + " mg"
                + "\nCurrent Dose: " + p.getCurrentDosemg() + " mg"
                + "\nMin Therapeutic Dose: " + p.getMinTherapeuticDosemg() + " mg"
                + "\nTotal Vial Mass: " + p.getTotalVialMassmg() + " mg"
                + "\nConcentration: " + p.getConcentrationmgPermL() + " mg/mL"
                + "\nFrequency: " + p.getFrequency()
                + "\nTitration: " + (p.isTitrationNeeded() ? "Yes" : "No")
                + "\nIncrement: " + p.getTitrationIncrementmg() + " mg"
                + "\nDays per Step: " + p.getDaysNeeded();
        JOptionPane.showMessageDialog(frame, detail, p.getCompoundName() + " (Vial " + id + ")",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /*
     method: createAppIcon
     purpose: draws the app icon (a little peptide vial) in code, so theres no
              image file to bundle or lose. shows up in the title bar and the
              windows taskbar
     arguments: none
     return: a 64x64 Image of the vial
    */
    private static Image createAppIcon() {
        int s = 64;
        java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(s, s, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // vial body
        g.setColor(new Color(225, 240, 250));
        g.fillRoundRect(20, 14, 24, 44, 12, 12);
        // liquid fill
        g.setColor(new Color(66, 133, 244));
        g.fillRoundRect(20, 34, 24, 24, 12, 12);
        // cap
        g.setColor(new Color(55, 71, 89));
        g.fillRoundRect(17, 6, 30, 10, 4, 4);
        // outline
        g.setColor(new Color(55, 71, 89));
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(20, 14, 24, 44, 12, 12);
        g.dispose();
        return img;
    }

    /*
     method: main
     purpose: where it all begins now. the GUI is the only entry point
     arguments: args: unused
     return: nothing
    */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainGUI::new);
    }
}