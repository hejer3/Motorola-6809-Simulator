package projet;

public class Cpu6809 {
    private Memory memory;
    private int PC;
    private int S, U, X, Y;
    private int A, B, DP, CC;
    private boolean programFinished = false;
    private String currentInstructionStr = "RESET";

    public Cpu6809(Memory memory) {
        this.memory = memory;
        reset();
    }

    public void reset() {
        PC = 0xF000;
        S = 0x0000;
        U = 0x0000;
        X = 0x0000;
        Y = 0x0000;
        A = 0x00;
        B = 0x00;
        DP = 0x00;
        CC = 0;
        programFinished = false;
        currentInstructionStr = "RESET";
    }

    public void step() {
        if (programFinished)
            return;

        int opcode = memory.read(PC);
        PC++;

        switch (opcode) {
            // --- CHARGEMENT IMMEDIAT ---
            case 0x86:
                A = memory.read(PC++);
                updateFlags(A);
                currentInstructionStr = String.format("LDA #$%02X", A);
                break;
            case 0xC6:
                B = memory.read(PC++);
                updateFlags(B);
                currentInstructionStr = String.format("LDB #$%02X", B);
                break;
            case 0x8E:
                X = read16();
                updateFlags16(X);
                currentInstructionStr = String.format("LDX #$%04X", X);
                break;

            // --- EFFACEMENT / CLR ---
            case 0x4F:
                A = 0;
                updateFlags(0);
                currentInstructionStr = "CLRA";
                break;
            case 0x5F:
                B = 0;
                updateFlags(0);
                currentInstructionStr = "CLRB";
                break;

            // --- INCREMENTATION / DECREMENTATION ---
            case 0x4C:
                A = (A + 1) & 0xFF;
                updateFlags(A);
                currentInstructionStr = "INCA";
                break;
            case 0x5C:
                B = (B + 1) & 0xFF;
                updateFlags(B);
                currentInstructionStr = "INCB";
                break;
            case 0x4A:
                A = (A - 1) & 0xFF;
                updateFlags(A);
                currentInstructionStr = "DECA";
                break;
            case 0x5A:
                B = (B - 1) & 0xFF;
                updateFlags(B);
                currentInstructionStr = "DECB";
                break;

            // AJOUT : INX (Incrémenter le registre d'index X)
            case 0x30:
                int postbyteInx = memory.read(PC++); // On simule le post-byte du 6809
                X = (X + 1) & 0xFFFF;
                updateFlags16(X);
                currentInstructionStr = "INX";
                break;

            // --- MODE INDEXÉ (,X) ---
            // Correction : On lit le post-byte (souvent $00 pour ,X) mais on utilise
            // l'adresse dans X
            case 0xA6:
                memory.read(PC++);
                A = memory.read(X);
                updateFlags(A);
                currentInstructionStr = "LDA ,X";
                break;
            case 0xA7:
                memory.read(PC++);
                memory.write(X, A);
                currentInstructionStr = "STA ,X";
                break;

            // --- BRANCHEMENTS (BOUCLES) ---
            case 0x20: // BRA (Branchement Toujours)
                byte off = (byte) memory.read(PC++);
                PC += off;
                currentInstructionStr = String.format("BRA $%02X", off);
                break;

            case 0x26: // BNE (Branchement si non égal à zéro)
                byte offne = (byte) memory.read(PC++);
                if ((CC & 0x04) == 0)
                    PC += offne; // Si le flag Z est à 0
                currentInstructionStr = String.format("BNE $%02X", (int) offne);
                break;

            case 0x27: // BEQ (Branchement si égal à zéro)
                byte offeq = (byte) memory.read(PC++);
                if ((CC & 0x04) != 0)
                    PC += offeq; // Si le flag Z est à 1
                currentInstructionStr = String.format("BEQ $%02X", (int) offeq);
                break;

            // --- COMPARAISON ---
            case 0x8C: // CMPX
                int vX = read16();
                if (X == vX)
                    CC |= 0x04;
                else
                    CC &= ~0x04;
                currentInstructionStr = String.format("CMPX #$%04X", vX);
                break;

            case 0x81: // CMPA
                int valA = memory.read(PC++);
                int resA = (A - valA) & 0xFF;
                updateFlags(resA);
                currentInstructionStr = String.format("CMPA #$%02X", valA);
                break;

            // --- ARRÊT DU PROGRAMME ---
            case 0x3F:
                programFinished = true;
                currentInstructionStr = "SWI (END)";
                break;

            default:
                currentInstructionStr = String.format("??? (%02X)", opcode);
                break;
        }
    }

    private int read16() {
        int h = memory.read(PC++);
        int l = memory.read(PC++);
        return (h << 8) | l;
    }

    private void updateFlags(int res) {
        CC &= ~(0x08 | 0x04); // Reset N et Z
        if (res == 0)
            CC |= 0x04; // Set Z si résultat nul
        if ((res & 0x80) != 0)
            CC |= 0x08; // Set N si bit 7 est 1
    }

    private void updateFlags16(int res) {
        CC &= ~(0x04);
        if ((res & 0xFFFF) == 0)
            CC |= 0x04;
    }

    public void setPC(int pc) {
        this.PC = pc;
    }

    // Getters
    public int getPC() {
        return PC;
    }

    public int getA() {
        return A;
    }

    public int getX() {
        return X;
    }

    public int getCC() {
        return CC;
    }

    public String getCurrentInstructionStr() {
        return currentInstructionStr;
    }

    public boolean isFinished() {
        return programFinished;
    }
}