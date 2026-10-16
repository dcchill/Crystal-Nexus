package net.crystalnexus.drone;

import net.crystalnexus.client.gui.DroneEditorLayout;
import net.crystalnexus.client.gui.DroneEditorSaveState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DroneEditorSaveTest {
    @Test void closeWaitsForMatchingSuccessfulSave(){
        var state=new DroneEditorSaveState();state.changed();state.closing=true;state.begin(4);
        assertFalse(state.canClose());assertFalse(state.acknowledge(3,state.request,true));assertFalse(state.canClose());
        assertTrue(state.acknowledge(4,state.request,true));assertTrue(state.canClose());assertFalse(state.dirty);
    }
    @Test void rejectedSaveKeepsEditsAndCancelsClose(){
        var state=new DroneEditorSaveState();state.changed();state.closing=true;state.begin(4);
        assertTrue(state.acknowledge(4,state.request,false));assertTrue(state.dirty);assertFalse(state.closing);assertFalse(state.canClose());
        int oldRequest=state.request;state.begin(4);assertFalse(state.acknowledge(4,oldRequest,false));assertTrue(state.acknowledge(4,state.request,true));assertFalse(state.dirty);
    }
    @Test void editsDuringSaveNeedAnotherAcknowledgementBeforeClosing(){
        var state=new DroneEditorSaveState();state.changed();state.begin(4);state.changed();state.closing=true;
        state.acknowledge(4,state.request,true);assertTrue(state.dirty);assertFalse(state.canClose());
        state.begin(5);state.acknowledge(5,state.request,true);assertTrue(state.canClose());
    }
    @Test void zoomAllowsLargeOverviewsAndCloseInspection(){
        assertEquals(.25F,DroneEditorLayout.clampZoom(.01F));assertEquals(3F,DroneEditorLayout.clampZoom(10));
        assertEquals(.4F,DroneEditorLayout.clampZoom(.4F));assertEquals(2F,DroneEditorLayout.clampZoom(2));
    }
}
