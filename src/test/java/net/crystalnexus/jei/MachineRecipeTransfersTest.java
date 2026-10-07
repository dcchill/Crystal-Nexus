package net.crystalnexus.jei;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class MachineRecipeTransfersTest {
    @Test
    void registeredInputsNeverIncludeOutputsUpgradesOrPlayerSlots() throws Exception {
        var sourceRoot = Path.of("src/main/java/net/crystalnexus");
        String registrations = Files.readString(sourceRoot.resolve("jei/MachineRecipeTransfers.java"));
        var registrationsMatcher = Pattern.compile("add\\(registration, (\\w+)\\.class, [^;]+?_Type, ([\\d, ]+)\\);")
            .matcher(registrations);
        int checked = 0;
        while (registrationsMatcher.find()) {
            String menuName = registrationsMatcher.group(1);
            String menu = Files.readString(sourceRoot.resolve("world/inventory/" + menuName + ".java"));
            // Read menu slot order, which can differ from the block inventory's slot IDs.
            var slotMatcher = Pattern.compile("addSlot\\(new (?:Slot(?:ItemHandler)?|MachineItemSlot)\\([^,]+,\\s*\\d+, ")
                .matcher(menu);
            var starts = new ArrayList<Integer>();
            while (slotMatcher.find()) starts.add(slotMatcher.start());
            int playerSlotsStart = menu.indexOf("new Slot(inv");
            assertTrue(playerSlotsStart >= 0, menuName);
            for (String input : registrationsMatcher.group(2).split(",")) {
                int index = Integer.parseInt(input.trim());
                assertTrue(index < starts.size(), menuName + " input " + index);
                int start = starts.get(index);
                assertTrue(start < playerSlotsStart, menuName + " transfers into player inventory");
                int end = index + 1 < starts.size() ? starts.get(index + 1) : playerSlotsStart;
                String slot = menu.substring(start, Math.min(end, playerSlotsStart));
                assertFalse(slot.contains("return false;"), menuName + " transfers into output " + index);
                assertFalse(slot.contains("machine_upgrades"), menuName + " transfers into upgrade " + index);
            }
            checked++;
        }
        assertTrue(checked >= 20, "Audit must cover the machine transfer registrations");
    }
}
