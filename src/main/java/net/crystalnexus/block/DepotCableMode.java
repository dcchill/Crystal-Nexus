package net.crystalnexus.block;

import net.minecraft.util.StringRepresentable;

public enum DepotCableMode implements StringRepresentable {
	DEFAULT("default"),
	IMPORT("import"),
	EXPORT("export");

	private final String name;

	DepotCableMode(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public DepotCableMode next() {
		return switch (this) {
			case DEFAULT -> IMPORT;
			case IMPORT -> EXPORT;
			case EXPORT -> DEFAULT;
		};
	}
}
