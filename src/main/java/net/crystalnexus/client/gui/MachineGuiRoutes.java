package net.crystalnexus.client.gui;

/** Inventory indices, never menu-list order. Negative endpoints identify drawn fluid tanks. */
public final class MachineGuiRoutes {
    private MachineGuiRoutes() {}
    private static final int[][] NONE = {};
    public static int[][] connections(String menu) {
        return switch (menu) {
            case "IronSmelterGuiMenu", "CrusherGuiMenu", "InverterGuiMenu", "SingularityMatrixGUIMenu",
                    "SingularityCompressorGUIMenu", "ComputationClusterGUIMenu", "BioMGuiMenu", "BioSIMGuiMenu",
                    "PartsAssemblerMenu", "ItemChargerGuiMenu" -> new int[][]{{0, 1}};
            case "FluidPackagerGUIMenu" -> new int[][]{{0, 1}, {-1, 1}};
            case "CircuitPressGUIMenu", "CrystalPurifierGUIMenu" -> new int[][]{{0, 1}, {2, 1}, {-1, 1}};
            case "ArcFurnaceMenu" -> new int[][]{{0, 2}, {1, 2}};
            case "ChemicalReactionChamberGUIMenu" -> new int[][]{{0, 3}, {1, 3}, {2, 3}};
            case "CraftingFactoryGUIMenu" -> new int[][]{{0, 9}, {1, 9}, {2, 9}, {3, 9}, {4, 9}, {5, 9}, {6, 9}, {7, 9}, {8, 9}};
            case "AcceleratorGuiMenu", "ExtractinatorGuiMenu" -> new int[][]{{0, 1}};
            case "SeparatorGuiMenu" -> new int[][]{{0, 1}};
            case "UltimaSmelterGuiMenu" -> new int[][]{{0, 1}, {3, 7}, {5, 9}, {6, 10}};
            case "OreProGUIMenu" -> new int[][]{{1, 2, 0}, {2, 3, 1}, {2, 4, 1}};
            case "EnergyExtractorGUIMenu" -> new int[][]{{0, 2}};
            case "RefineryMenu", "CryogenicFlashFreezerMenu" -> new int[][]{{-1, -2}};
            case "FluidChemicalReactionChamberGUIMenu" -> new int[][]{{-1, -3}, {-2, -3}};
            case "NodeExtractorGUIMenu", "SteamEngineGUIMenu" -> new int[][]{{-100, -1}};
            case "ReactionGUIMenu" -> new int[][]{{-100, 0}};
            case "NodeMinerGUIMenu" -> new int[][]{{-100, 0}};
            case "SolarSimulatorMenu" -> new int[][]{{4, -101}};
            case "SolarEngineMenu" -> new int[][]{{0, -101}, {-1, -101}};
            case "PistonGenGUIMenu" -> new int[][]{{0, 1}, {-1, 1}};
            case "SteamChamberGUIMenu" -> new int[][]{{0, -1}, {2, -1}};
            case "NeutronFluxChamberHatchMenu" -> new int[][]{{0, -1}};
            case "BioMCGuiMenu" -> new int[][]{{0, 1}, {3, 1}};
            case "HemolyzerMenu" -> new int[][]{{0, -1}};
            case "HemochanterMenu" -> new int[][]{{-1, 0}};
            case "MasticatorMenu" -> new int[][]{{1, 2}, {1, 3}, {-1, 2}};
            case "CometForgeMenu", "GravitationalArrayMenu" -> new int[][]{{0, 4}, {1, 4}, {2, 4}, {3, 4}};
            case "CelestialGearForgeMenu" -> new int[][]{{-102, 9}};
            default -> NONE;
        };
    }
    /** Machines whose GUI shows separate inputs being combined; each input keeps its own feed. */
    public static boolean combining(String menu) {
        return switch (menu) {
            case "CircuitPressGUIMenu", "CrystalPurifierGUIMenu", "FluidChemicalReactionChamberGUIMenu",
                    "FluidPackagerGUIMenu", "PistonGenGUIMenu", "SteamChamberGUIMenu", "BioMCGuiMenu",
                    "CelestialGearForgeMenu", "MasticatorMenu" -> true;
            default -> false;
        };
    }
    public static String role(String menu, int slot) {
        if (menu.equals("BioMCGuiMenu") && slot == 3) return "set_plant";
        if (menu.equals("AcceleratorGuiMenu") && slot >= 0 && slot < 8)
            return slot == 0 || slot == 2 || slot == 3 || slot == 4 ? "input" : "output";
        if (menu.equals("ExtractinatorGuiMenu") && slot >= 0 && slot <= 6) return slot == 0 ? "input" : "output";
        if (menu.equals("MatterTransmutationGUIMenu") && slot <= net.crystalnexus.world.inventory.MachineGuiLayout.TRANSMUTATION_OUTPUT)
            return slot == net.crystalnexus.world.inventory.MachineGuiLayout.TRANSMUTATION_OUTPUT ? "output" : "input";
        if (menu.equals("CelestialGearForgeMenu") && slot < 9) return slot == 8 ? "star" : "input";
        if (menu.equals("CrystalPurifierGUIMenu") && slot == 2) return "catalyst";
        if (menu.equals("SteamChamberGUIMenu") && slot == 0) return "fluid_container";
        if (menu.equals("SteamChamberGUIMenu") && slot == 2) return "fuel";
        if (menu.equals("SolarEngineMenu") && slot == 0) return "star";
        if (menu.equals("RefineryMenu") || menu.equals("CryogenicFlashFreezerMenu") || menu.equals("FluidChemicalReactionChamberGUIMenu")) return "fluid_container";
        for (int[] route : connections(menu)) if (route[1] == slot) return "output";
        for (int[] route : connections(menu)) if (route[0] == slot) return "input";
        return switch (menu) {
            case "DepotCableConnectionMenu", "SmartSplitterGUIMenu" -> "filter";
            case "ControlRodGuiMenu" -> "fuel";
            case "GasolineGeneratorMenu" -> slot == 0 ? "fuel" : "output";
            case "BlockPlacerGuiMenu", "FactoryItemControllerGuiMenu" -> "input";
            case "ItemElevatorGuiMenu", "ItemElevatorGuiDownMenu" -> "transport";
            case "CraftingProcessorMenu" -> "pattern";
            case "ItemCollectorGUIMenu" -> "output";
            case "ContainerGUIMenu", "DepotMenu", "TesseractGuiMenu" -> "storage";
            case "MawMenu", "QuarryGUIMenu", "QuantumMinerGUIMenu", "NodeMinerGUIMenu", "WasteOutputGuiMenu" -> "output";
            case "PlasmaGeneratorMenu" -> "component";
            default -> "slot";
        };
    }
}
