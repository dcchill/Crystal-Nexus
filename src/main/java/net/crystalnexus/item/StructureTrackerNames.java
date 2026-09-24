package net.crystalnexus.item;

public final class StructureTrackerNames {
	private StructureTrackerNames() {
	}

	public static String displayName(String path) {
		path = path.replace('_', ' ');
		StringBuilder result = new StringBuilder(path.length());
		boolean capitalize = true;
		for (int i = 0; i < path.length(); i++) {
			char character = path.charAt(i);
			result.append(capitalize ? Character.toUpperCase(character) : character);
			capitalize = character == ' ' || character == '/';
		}
		return result.toString();
	}
}
