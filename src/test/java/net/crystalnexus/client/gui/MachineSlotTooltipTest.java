package net.crystalnexus.client.gui;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MachineSlotTooltipTest {
    @Test void genericSlotsHaveNoHint() {
        for (String role : List.of("storage", "input", "output", "slot"))
            assertNull(MachineGuiWidgets.slotTooltipKey(role, false), role);
    }

    @Test void specialPurposeSlotsKeepTheirHints() {
        for (String role : List.of("fuel", "upgrade", "filter", "catalyst", "component", "star",
                "fluid_container", "transport", "pattern", "set_plant"))
            assertEquals("gui.crystalnexus.machine." + role, MachineGuiWidgets.slotTooltipKey(role, false));
    }

    @Test void craftingFilterHintTakesPriorityOverGenericRoles() {
        for (String role : List.of("storage", "input", "output", "slot", "filter"))
            assertEquals("gui.crystalnexus.crafting_factory_gui.tooltip_crafting_filter",
                    MachineGuiWidgets.slotTooltipKey(role, true));
    }
}
