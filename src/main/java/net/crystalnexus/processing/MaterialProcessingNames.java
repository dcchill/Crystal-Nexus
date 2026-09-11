package net.crystalnexus.processing;

/** Pure naming rules shared by catalog discovery and unit tests. */
public final class MaterialProcessingNames {
    private MaterialProcessingNames() {}

    public static String extract(String tagId) {
        int colon = tagId.indexOf(':');
        String path = colon < 0 ? tagId : tagId.substring(colon + 1);
        int slash = path.indexOf('/');
        return slash < 0 || slash == path.length() - 1 ? "" : path.substring(slash + 1);
    }

    public static String normalizeMaterial(String id) {
        int colon = id.indexOf(':');
        String path = colon < 0 ? id : id.substring(colon + 1);
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    public static int requiredMachineAge(String material) {
        String normalized = normalizeMaterial(material);
		if (normalized.contains("hyper") || normalized.contains("solar") || normalized.contains("zero_point"))
			return MachineAge.AGE_4.number();
		if (normalized.contains("tungsten") || normalized.contains("carbon") || normalized.contains("titanium_carbide"))
			return MachineAge.AGE_3.number();
		if (normalized.contains("invertium") || normalized.contains("titanium") || normalized.contains("platinum"))
			return MachineAge.AGE_2.number();
		return MachineAge.AGE_1.number();
    }
}
