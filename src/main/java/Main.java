import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Main class for the Sorting Algorithm Performance application.
 *
 * Features:
 * 1. Upload a CSV dataset.
 * 2. Display the CSV column names.
 * 3. Detect numeric columns.
 * 4. Allow the user to select a numeric column.
 * 5. Benchmark five sorting algorithms.
 * 6. Display execution times and sorted output.
 */
public class Main {

    // Holds the currently loaded CSV dataset.
    private Dataset dataset;

    // List model used to display all CSV column names.
    private final DefaultListModel<String> allColumnsModel =
            new DefaultListModel<>();

    private final JList<String> allColumnsList =
            new JList<>(allColumnsModel);

    // Dropdown containing only numeric column names.
    private final JComboBox<String> numericColumnCombo =
            new JComboBox<>();

    // Shows the name and status of the loaded file.
    private final JLabel fileStatus =
            new JLabel("No CSV file loaded.");

    // Displays a preview of the sorted numeric values.
    private final JTextArea resultPreview =
            new JTextArea(6, 60);

    // Table displaying each algorithm's measured execution time.
    private final DefaultTableModel resultTableModel =
            new DefaultTableModel(
                    new Object[]{
                        "Algorithm",
                        "Time (ns)",
                        "Time (ms)",
                        "Status"
                    },
                    0
            );

    private final JTable resultTable =
            new JTable(resultTableModel);

    /**
     * Application entry point.
     */
    public static void main(String[] args) {

        // Swing GUI components should be created on the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> {
            new Main().createAndShowGui();
        });
    }

    /**
     * Creates and displays the application window.
     */
    private void createAndShowGui() {

        JFrame frame = new JFrame(
                "Sorting Algorithm Performance"
        );

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(850, 650);
        frame.setLocationRelativeTo(null);

        // Application heading.
        JLabel title = new JLabel(
                "Sorting Algorithm Performance",
                SwingConstants.CENTER
        );

        title.setFont(
                new Font("SansSerif", Font.BOLD, 22)
        );

        // Buttons for loading and benchmarking data.
        JButton uploadButton = new JButton("Upload CSV");

        JButton compareButton =
                new JButton("Compare Sorting Algorithms");

        // Panel containing the upload button and file status.
        JPanel uploadPanel =
                new JPanel(new FlowLayout(FlowLayout.LEFT));

        uploadPanel.add(uploadButton);
        uploadPanel.add(fileStatus);

        // Panel containing the numeric column dropdown and compare button.
        JPanel selectionPanel =
                new JPanel(new FlowLayout(FlowLayout.LEFT));

        selectionPanel.add(new JLabel("Numeric column:"));
        selectionPanel.add(numericColumnCombo);
        selectionPanel.add(compareButton);

        // Top section of the window.
        JPanel northPanel = new JPanel(
                new BorderLayout(5, 5)
        );

        northPanel.add(title, BorderLayout.NORTH);
        northPanel.add(uploadPanel, BorderLayout.CENTER);
        northPanel.add(selectionPanel, BorderLayout.SOUTH);

        // Panel listing all columns found in the CSV.
        JPanel columnsPanel =
                new JPanel(new BorderLayout());

        columnsPanel.setBorder(
                BorderFactory.createTitledBorder("CSV columns")
        );

        columnsPanel.add(
                new JScrollPane(allColumnsList),
                BorderLayout.CENTER
        );

        // Configure the sorted output preview.
        resultPreview.setEditable(false);
        resultPreview.setLineWrap(true);
        resultPreview.setWrapStyleWord(true);

        JPanel previewPanel =
                new JPanel(new BorderLayout());

        previewPanel.setBorder(
                BorderFactory.createTitledBorder(
                        "Sorted values preview"
                )
        );

        previewPanel.add(
                new JScrollPane(resultPreview),
                BorderLayout.CENTER
        );

        // Panel displaying algorithm performance results.
        JPanel resultsPanel =
                new JPanel(new BorderLayout());

        resultsPanel.setBorder(
                BorderFactory.createTitledBorder(
                        "Performance results"
                )
        );

        resultsPanel.add(
                new JScrollPane(resultTable),
                BorderLayout.CENTER
        );

        // Split the output preview and performance table vertically.
        JSplitPane lowerSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                previewPanel,
                resultsPanel
        );

        lowerSplit.setResizeWeight(0.35);

        // Split the column list and results area horizontally.
        JSplitPane mainSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                columnsPanel,
                lowerSplit
        );

        mainSplit.setResizeWeight(0.22);

        // Add the main panels to the window.
        frame.add(northPanel, BorderLayout.NORTH);
        frame.add(mainSplit, BorderLayout.CENTER);

        // Connect the buttons to their event handlers.
        uploadButton.addActionListener(
                event -> uploadCsv(frame)
        );

        compareButton.addActionListener(
                event -> compareAlgorithms(frame)
        );

        // Show the completed GUI.
        frame.setVisible(true);
    }

    /**
     * Opens a file chooser and loads a CSV file.
     */
    private void uploadCsv(JFrame parent) {

        JFileChooser chooser = new JFileChooser();

        chooser.setDialogTitle("Select a CSV dataset");

        // Filter the file chooser to show CSV files.
        chooser.setFileFilter(
                new javax.swing.filechooser.FileNameExtensionFilter(
                        "CSV files (*.csv)",
                        "csv"
                )
        );

        // Stop if the user cancels the file chooser.
        if (chooser.showOpenDialog(parent)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = chooser.getSelectedFile();

        try {
            // Read the selected file using CSVReader.
            Dataset loaded = CSVReader.readCSV(selectedFile);

            // A header without data rows is not useful for benchmarking.
            if (loaded.getRows().isEmpty()) {
                throw new IllegalArgumentException(
                        "The CSV has a header but no data rows."
                );
            }

            // Replace the existing dataset after successful loading.
            dataset = loaded;

            // Clear information from the previously loaded dataset.
            allColumnsModel.clear();
            numericColumnCombo.removeAllItems();
            resultTableModel.setRowCount(0);
            resultPreview.setText("");

            // Display every CSV column in the column list.
            for (String column : dataset.getColumnNames()) {
                allColumnsModel.addElement(column);
            }

            // Find columns that contain numeric data.
            for (int i = 0;
                    i < dataset.getColumnNames().size();
                    i++) {

                if (NumericColumnUtils.isNumericColumn(
                        dataset, i)) {

                    numericColumnCombo.addItem(
                            dataset.getColumnNames().get(i)
                    );
                }
            }

            // Update the file status label.
            fileStatus.setText(
                    selectedFile.getName()
                    + " | "
                    + dataset.getRows().size()
                    + " data rows"
            );

            // Inform the user whether a numeric column was found.
            if (numericColumnCombo.getItemCount() == 0) {

                JOptionPane.showMessageDialog(
                        parent,
                        "No numeric columns were found in this CSV.",
                        "No numeric columns",
                        JOptionPane.WARNING_MESSAGE
                );

            } else {

                JOptionPane.showMessageDialog(
                        parent,
                        "CSV loaded successfully.\nRows: "
                        + dataset.getRows().size()
                        + "\nNumeric columns: "
                        + numericColumnCombo.getItemCount(),
                        "Upload complete",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

        } catch (Exception ex) {

            // Clear the current dataset if the new file fails to load.
            dataset = null;

            allColumnsModel.clear();
            numericColumnCombo.removeAllItems();

            fileStatus.setText("Could not load CSV.");

            // Show a useful error message to the user.
            JOptionPane.showMessageDialog(
                    parent,
                    "Could not read the selected file:\n"
                    + ex.getMessage(),
                    "CSV error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Extracts the selected numeric column and benchmarks all algorithms.
     */
    private void compareAlgorithms(JFrame parent) {

        // The user must upload a dataset before comparing algorithms.
        if (dataset == null) {

            JOptionPane.showMessageDialog(
                    parent,
                    "Upload a CSV file first.",
                    "No dataset",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        // Get the selected numeric column name from the dropdown.
        String selectedColumn =
                (String) numericColumnCombo.getSelectedItem();

        if (selectedColumn == null) {

            JOptionPane.showMessageDialog(
                    parent,
                    "Select a numeric column first.",
                    "No column selected",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        try {
            // Find the position of the selected column.
            int index = dataset.getColumnNames()
                    .indexOf(selectedColumn);

            // Convert the selected column into a numeric array.
            double[] values =
                    NumericColumnUtils.extractValues(
                            dataset, index
                    );

            if (values.length == 0) {
                throw new IllegalArgumentException(
                        "The selected column has no numeric values."
                );
            }

            // Run all five sorting algorithms and collect timings.
            List<SortBenchmark.BenchmarkResult> results =
                    SortBenchmark.compare(values);

            // Remove results from the previous comparison.
            resultTableModel.setRowCount(0);

            // Add each algorithm's result to the table.
            for (SortBenchmark.BenchmarkResult result : results) {

                resultTableModel.addRow(
                        new Object[]{
                            result.getAlgorithmName(),
                            result.getElapsedNanos(),
                            String.format(
                                    "%.6f",
                                    result.getElapsedMillis()
                            ),
                            "Sorted correctly"
                        }
                );
            }

            // Display the sorted result from the first algorithm.
            // SortBenchmark checks the correctness of every algorithm.
            resultPreview.setText(
                    "Selected column: "
                    + selectedColumn
                    + "\nValues sorted ascending (preview):\n"
                    + formatPreview(
                            results.get(0).getSortedValues(),
                            100
                    )
            );

        } catch (Exception ex) {

            // Display any errors encountered during sorting.
            JOptionPane.showMessageDialog(
                    parent,
                    "Could not sort this column:\n"
                    + ex.getMessage(),
                    "Sorting error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Formats the sorted array for display in the GUI.
     *
     * Large datasets are limited to a preview of 100 values
     * so the application remains readable.
     */
    private String formatPreview(
            double[] values, int maxValues) {

        StringBuilder text = new StringBuilder();

        int count = Math.min(
                values.length,
                maxValues
        );

        for (int i = 0; i < count; i++) {

            // Separate values with commas.
            if (i > 0) {
                text.append(", ");
            }

            text.append(values[i]);
        }

        // Inform the user if the preview does not show every value.
        if (values.length > maxValues) {

            text.append(" ... (showing ")
                    .append(maxValues)
                    .append(" of ")
                    .append(values.length)
                    .append(" values)");
        }

        return text.toString();
    }
}