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

    public static int requiredMachineTier(String material) {
        String normalized = normalizeMaterial(material);
        if (normalized.contains("hyper") || normalized.contains("carbon") || normalized.contains("tungsten")) return 3;
        if (normalized.contains("invert") || normalized.contains("platinum")) return 2;
        return 1;
    }
}
