package projet;

import javax.swing.*;
import java.awt.*;
import javax.swing.table.DefaultTableModel;

public class SimulatorGUI extends JFrame {
    private Cpu6809 cpu;
    private Memory memory;
    private JDesktopPane desktopPane;
    private JInternalFrame archFrame, editorFrame, ramFrame, romFrame, progFrame;
    private ArchitecturePanel archPanel;
    private JTextArea editorArea, progArea;
    private JTable ramTable, romTable;

    public SimulatorGUI(Cpu6809 cpu, Memory memory) {
        this.cpu = cpu;
        this.memory = memory;
        setTitle("MOTO6809 Simulator - Final");
        setSize(1300, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        desktopPane = new JDesktopPane();
        desktopPane.setBackground(new Color(50, 50, 60));
        add(desktopPane, BorderLayout.CENTER);

        createWindows();
        createMenuBar();
        createToolBar();
    }

    private void createWindows() {
        archFrame = createFrame("Architecture interne du 6809", 10, 10, 280, 520);
        archPanel = new ArchitecturePanel(cpu);
        archFrame.add(archPanel);
        desktopPane.add(archFrame);

        editorFrame = createFrame("Editeur", 300, 10, 300, 400);
        JToolBar editBar = new JToolBar();
        JButton btnUpdate = new JButton("Assembler le code");
        btnUpdate.addActionListener(e -> compileCode());
        editBar.add(btnUpdate);
        editorArea = new JTextArea("");
        editorArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JPanel p = new JPanel(new BorderLayout());
        p.add(editBar, BorderLayout.NORTH);
        p.add(new JScrollPane(editorArea), BorderLayout.CENTER);
        editorFrame.add(p);
        desktopPane.add(editorFrame);

        ramFrame = createFrame("RAM", 610, 10, 200, 350);
        setupTable(ramFrame, 0);

        romFrame = createFrame("ROM", 820, 10, 200, 350);
        setupTable(romFrame, 0xF000);

        progFrame = createFrame("Désassembleur", 1030, 10, 240, 350);
        progArea = new JTextArea();
        progArea.setEditable(false);
        progArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        progFrame.add(new JScrollPane(progArea));
        desktopPane.add(progFrame);
    }

    private void setupTable(JInternalFrame f, int startAddr) {
        String[] cols = { "Addr", "Val" };
        Object[][] data = new Object[32][2];
        for (int i = 0; i < 32; i++) {
            data[i][0] = String.format("%04X", startAddr + i);
            data[i][1] = "00";
        }
        JTable table = new JTable(new DefaultTableModel(data, cols));
        if (startAddr == 0)
            ramTable = table;
        else
            romTable = table;
        f.add(new JScrollPane(table));
        desktopPane.add(f);
    }

    private JInternalFrame createFrame(String title, int x, int y, int w, int h) {
        JInternalFrame f = new JInternalFrame(title, true, true, true, true);
        f.setSize(w, h);
        f.setLocation(x, y);
        f.setVisible(true);
        return f;
    }

    private void compileCode() {
        String code = editorArea.getText();
        if (code.trim().isEmpty())
            return;
        String[] lines = code.split("\n");
        int address = 0xF000;
        cpu.setPC(address);
        memory.reset();

        try {
            for (String line : lines) {
                line = line.trim().toUpperCase();
                if (line.isEmpty())
                    continue;
                String[] parts = line.split("\\s+");
                String mnemo = parts[0];
                String operandStr = (parts.length > 1) ? parts[1] : "";

                boolean isImm = operandStr.startsWith("#");
                boolean isIdx = operandStr.equals(",X") || operandStr.equals("X");

                if (mnemo.equals("LDA")) {
                    if (isIdx) {
                        memory.write(address++, 0xA6);
                        memory.write(address++, 0x00);
                    } else if (isImm)
                        memory.write(address++, 0x86);
                } else if (mnemo.equals("LDX")) {
                    memory.write(address++, 0x8E);
                } else if (mnemo.equals("STA")) {
                    if (isIdx) {
                        memory.write(address++, 0xA7);
                        memory.write(address++, 0x00);
                    }
                } else if (mnemo.equals("INX")) {
                    memory.write(address++, 0x30);
                    memory.write(address++, 0x00);
                } else if (mnemo.equals("INCA"))
                    memory.write(address++, 0x4C);
                else if (mnemo.equals("CLRA"))
                    memory.write(address++, 0x4F);
                else if (mnemo.equals("BNE")) {
                    memory.write(address++, 0x26);
                } else if (mnemo.equals("SWI") || mnemo.equals("END"))
                    memory.write(address++, 0x3F);

                // Ecriture opérande simple (hex)
                if (!operandStr.isEmpty() && !isIdx) {
                    String clean = operandStr.replace("#", "").replace("$", "");
                    int v = Integer.parseInt(clean, 16);
                    if (mnemo.equals("LDX") || mnemo.equals("BNE")) {
                        if (mnemo.equals("BNE"))
                            memory.write(address++, v); // Offset
                        else {
                            memory.write(address++, (v >> 8));
                            memory.write(address++, (v & 0xFF));
                        }
                    } else {
                        memory.write(address++, v);
                    }
                }
            }
            JOptionPane.showMessageDialog(this, "Assemblage réussi !");
            updateAll();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage());
        }
    }

    private void updateAll() {
        archPanel.updateValues();
        for (int i = 0; i < 32; i++)
            ramTable.setValueAt(String.format("%02X", memory.read(i)), i, 1);
        progArea.setText(
                "PC: " + String.format("%04X", cpu.getPC()) + "\nInstruction: " + cpu.getCurrentInstructionStr());
    }

    private void createMenuBar() {
        JMenuBar mb = new JMenuBar();
        JMenu winMenu = new JMenu("Fenêtres");
        mb.add(new JMenu("Fichier"));
        mb.add(winMenu);
        setJMenuBar(mb);
    }

    private void createToolBar() {
        JToolBar tb = new JToolBar();
        JButton r = new JButton("RESET");
        r.addActionListener(e -> {
            cpu.reset();
            updateAll();
        });
        JButton s = new JButton("Step");
        s.addActionListener(e -> {
            cpu.step();
            updateAll();
        });
        tb.add(r);
        tb.add(s);
        add(tb, BorderLayout.NORTH);
    }

    // --- PANEL ARCHITECTURE ---
    class ArchitecturePanel extends JPanel {
        private Cpu6809 cpu;
        private JTextField txtPC, txtInst, txtA, txtX, txtCC;

        public ArchitecturePanel(Cpu6809 cpu) {
            this.cpu = cpu;
            setLayout(null);
            addLbl("PC", 20, 20);
            txtPC = addField(50, 20, 60);
            txtInst = addField(20, 60, 230);
            txtInst.setForeground(Color.RED);
            addLbl("A", 20, 100);
            txtA = addField(50, 100, 40);
            addLbl("X", 120, 100);
            txtX = addField(150, 100, 60);
            addLbl("CC", 20, 150);
            txtCC = addField(50, 150, 100);
        }

        private JTextField addField(int x, int y, int w) {
            JTextField t = new JTextField();
            t.setBounds(x, y, w, 25);
            t.setEditable(false);
            add(t);
            return t;
        }

        private void addLbl(String s, int x, int y) {
            JLabel l = new JLabel(s);
            l.setBounds(x, y, 30, 25);
            add(l);
        }

        public void updateValues() {
            txtPC.setText(String.format("%04X", cpu.getPC()));
            txtInst.setText(cpu.getCurrentInstructionStr());
            txtA.setText(String.format("%02X", cpu.getA()));
            txtX.setText(String.format("%04X", cpu.getX()));
            txtCC.setText(Integer.toBinaryString(cpu.getCC()));
        }
    }
}